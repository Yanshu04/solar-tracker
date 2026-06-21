package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.api.TomorrowioApi
import com.example.data.api.TomorrowForecastResponse
import com.example.data.api.TomorrowHourlyItem
import com.example.data.api.OpenMeteoApi
import com.example.data.api.OpenMeteoResponse
import com.example.data.api.OpenMeteoGeocodingApi
import com.example.data.api.GeocodingResult
import com.example.data.db.SolarDao
import com.example.data.model.Alert
import com.example.data.model.ForecastHourEntity
import com.example.data.model.Site
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin

class SolarRepository(
    private val context: Context,
    private val solarDao: SolarDao,
    private val api: TomorrowioApi = TomorrowioApi.create(),
    private val openMeteoApi: OpenMeteoApi = OpenMeteoApi.create(),
    private val openMeteoGeocodingApi: OpenMeteoGeocodingApi = OpenMeteoGeocodingApi.create()
) {
    val allSites: Flow<List<Site>> = solarDao.getAllSitesFlow()
    val allAlerts: Flow<List<Alert>> = solarDao.getAllAlertsFlow()

    suspend fun searchCity(query: String): List<GeocodingResult> = withContext(Dispatchers.IO) {
        try {
            val response = openMeteoGeocodingApi.searchCity(query)
            response.results ?: emptyList()
        } catch (e: Exception) {
            Log.e("SolarRepository", "Geocoding search failed", e)
            emptyList()
        }
    }

    suspend fun fetchRawWeather(latitude: Double, longitude: Double): OpenMeteoResponse? = withContext(Dispatchers.IO) {
        try {
            openMeteoApi.getForecast(latitude, longitude)
        } catch (e: Exception) {
            Log.e("SolarRepository", "Failed to fetch raw weather", e)
            null
        }
    }

    init {
        // Run seed in IO coroutine context
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfNeeded()
        }
    }

    fun getSiteById(siteId: String): Flow<Site?> = solarDao.getSiteByIdFlow(siteId)

    fun getForecastForSite(siteId: String): Flow<List<ForecastHourEntity>> =
        solarDao.getForecastForSiteFlow(siteId)

    suspend fun insertAlert(alert: Alert) = withContext(Dispatchers.IO) {
        solarDao.insertAlert(alert)
    }

    suspend fun deleteAlert(alertId: Int) = withContext(Dispatchers.IO) {
        solarDao.deleteAlertById(alertId)
    }

    suspend fun clearAllAlerts() = withContext(Dispatchers.IO) {
        solarDao.clearAllAlerts()
    }

    suspend fun insertSite(site: Site) = withContext(Dispatchers.IO) {
        solarDao.insertSite(site)
    }

    suspend fun updateSiteOverride(siteId: String, override: String) = withContext(Dispatchers.IO) {
        val site = solarDao.getSiteById(siteId) ?: return@withContext
        var finalMode = site.currentMode
        var finalAngle = site.currentAngle

        when (override) {
            "Follow" -> {
                finalMode = "Following sun"
                finalAngle = calculateAngleForHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
            }
            "Hold" -> {
                finalMode = "Holding"
            }
            "Stow" -> {
                finalMode = "Stowed"
                finalAngle = 0f
            }
        }

        val updatedSite = site.copy(
            manualOverride = override,
            currentMode = finalMode,
            currentAngle = finalAngle,
            lastUpdated = System.currentTimeMillis()
        )
        solarDao.updateSite(updatedSite)
    }

    suspend fun updateSiteLocation(
        siteId: String,
        name: String,
        latitude: Double,
        longitude: Double,
        hasSolarPlant: Boolean = true,
        isManualSetup: Boolean = false,
        temp: Double? = null,
        wind: Double? = null,
        cloud: Double? = null,
        ghi: Double? = null,
        status: String? = null
    ) = withContext(Dispatchers.IO) {
        val site = solarDao.getSiteById(siteId) ?: return@withContext
        val updatedSite = site.copy(
            name = name,
            latitude = latitude,
            longitude = longitude,
            hasSolarPlant = hasSolarPlant,
            isManualSetup = isManualSetup,
            currentTemp = temp ?: site.currentTemp,
            currentWindSpeed = wind ?: site.currentWindSpeed,
            currentCloudCover = cloud ?: site.currentCloudCover,
            currentSolarGHI = ghi ?: site.currentSolarGHI,
            status = status ?: site.status
        )
        solarDao.updateSite(updatedSite)
    }

    // Refresh weather data: makes real network requests to Tomorrow.io or falling back to Open-Meteo
    suspend fun refreshWeather() = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.TOMORROW_API_KEY
        val hasRealKey = apiKey.isNotEmpty() && apiKey != "MY_TOMORROW_API_KEY"

        val sites = solarDao.getAllSites()
        for (site in sites) {
            if (site.isManualSetup) {
                Log.d("SolarRepository", "Skipping live forecast API fetch for manual site: ${site.name}")
                continue
            }
            try {
                if (hasRealKey) {
                    Log.d("SolarRepository", "Fetching real Tomorrow.io weather for ${site.name}")
                    val locationStr = "${site.latitude},${site.longitude}"
                    val response = api.getForecast(location = locationStr, apikey = apiKey)
                    parseAndSaveForecast(site, response)
                } else {
                    Log.d("SolarRepository", "Fetching real Open-Meteo weather for ${site.name}")
                    val response = openMeteoApi.getForecast(
                        latitude = site.latitude,
                        longitude = site.longitude
                    )
                    parseAndSaveOpenMeteoForecast(site, response)
                }
            } catch (e: Exception) {
                Log.e("SolarRepository", "Failed weather refresh for ${site.name}, falling back to mock", e)
                simulateWeatherForSite(site)
            }
        }
    }

    private suspend fun parseAndSaveOpenMeteoForecast(site: Site, response: OpenMeteoResponse) {
        val hourly = response.hourly ?: return
        val times = hourly.time ?: return
        if (times.isEmpty()) return

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val nowMs = System.currentTimeMillis()
        var closestIndex = 0
        var minDiff = Long.MAX_VALUE

        for (i in times.indices) {
            val tStr = times[i]
            val date = try { sdf.parse(tStr) } catch(e: Exception) { null }
            if (date != null) {
                val diff = Math.abs(date.time - nowMs)
                if (diff < minDiff) {
                    minDiff = diff
                    closestIndex = i
                }
            }
        }

        val temp = hourly.temperature2m?.getOrNull(closestIndex) ?: site.currentTemp
        val wind = hourly.windSpeed10m?.getOrNull(closestIndex) ?: site.currentWindSpeed
        val cloud = hourly.cloudCover?.getOrNull(closestIndex) ?: site.currentCloudCover
        val solar = hourly.shortwaveRadiation?.getOrNull(closestIndex) ?: site.currentSolarGHI

        var calculatedMode = site.currentMode
        var calculatedStatus = site.status

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val isNight = currentHour < 5 || currentHour >= 20

        if (wind > 50.0) {
            calculatedMode = "Stowed"
            calculatedStatus = "Storm mode"
        } else if (wind >= 30.0) {
            calculatedMode = "Safe mode"
            calculatedStatus = "Active"
        } else if (isNight) {
            calculatedMode = "Reset"
            calculatedStatus = "Active"
        } else if (cloud < 30.0) {
            calculatedMode = "Following sun"
            calculatedStatus = "Active"
        } else {
            calculatedMode = "Holding"
            calculatedStatus = "Active"
        }

        // Apply manual override
        val finalMode = when (site.manualOverride) {
            "Follow" -> "Following sun"
            "Hold" -> "Holding"
            "Stow" -> "Stowed"
            else -> calculatedMode
        }

        val finalAngle = when (finalMode) {
            "Following sun" -> calculateAngleForHour(currentHour)
            "Stowed", "Safe mode" -> 0f
            "Reset" -> -50f
            else -> site.currentAngle
        }

        val updatedSite = site.copy(
            currentTemp = temp,
            currentWindSpeed = wind,
            currentCloudCover = cloud,
            currentSolarGHI = solar,
            currentMode = finalMode,
            currentAngle = finalAngle,
            status = if (site.status == "Fault") "Fault" else if (site.status == "Offline") "Offline" else calculatedStatus,
            lastUpdated = System.currentTimeMillis()
        )
        solarDao.updateSite(updatedSite)

        // Generate Alert if needed
        if (wind > 50.0) {
            solarDao.insertAlert(
                Alert(
                    siteId = site.id,
                    siteName = site.name,
                    alertType = "Storm warning",
                    message = "Severe winds detected at ${site.name} (${String.format("%.1f", wind)} km/h). Panels auto-stowed for protection.",
                    severity = "High"
                )
            )
        }

        // 3. Save tomorrow's hourly predictions (5 AM to 8 PM)
        val forecastEntities = mutableListOf<ForecastHourEntity>()
        val tomorrowCal = Calendar.getInstance()
        tomorrowCal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(tomorrowCal.time)

        val formatOutput = SimpleDateFormat("hh:00 a", Locale.getDefault())

        for (i in times.indices) {
            val tStr = times[i]
            if (tStr.startsWith(tomorrowDateStr)) {
                val date = try { sdf.parse(tStr) } catch(e: Exception) { null }
                if (date != null) {
                    val cal = Calendar.getInstance().apply { time = date }
                    val hr = cal.get(Calendar.HOUR_OF_DAY)

                    if (hr in 5..20) {
                        val hourLabel = try {
                            formatOutput.format(date).uppercase()
                        } catch (e: Exception) {
                            "${String.format("%02d", hr)}:00 ${if (hr < 12) "AM" else "PM"}"
                        }

                        val fTemp = hourly.temperature2m?.getOrNull(i) ?: 28.0
                        val fWind = hourly.windSpeed10m?.getOrNull(i) ?: 12.0
                        val fCloud = hourly.cloudCover?.getOrNull(i) ?: 15.0
                        val fPrecip = hourly.precipitationProbability?.getOrNull(i) ?: 0.0
                        val fSolar = hourly.shortwaveRadiation?.getOrNull(i) ?: 400.0

                        val (action, color) = calculateActionAndColor(hr, fWind, fPrecip, fCloud)

                        forecastEntities.add(
                            ForecastHourEntity(
                                siteId = site.id,
                                hourTime = hourLabel,
                                temperature = fTemp,
                                windSpeed = fWind,
                                cloudCover = fCloud,
                                precipitationProbability = fPrecip,
                                solarGHI = fSolar,
                                weatherCondition = when {
                                    fWind > 50.0 -> "storm"
                                    fPrecip > 80.0 -> "rain"
                                    fCloud > 70.0 -> "cloudy"
                                    else -> "clear"
                                },
                                panelAction = action,
                                actionColor = color
                            )
                        )
                    }
                }
            }
        }

        if (forecastEntities.isNotEmpty()) {
            solarDao.deleteForecastForSite(site.id)
            solarDao.insertForecastHours(forecastEntities)
        }
    }

    private suspend fun parseAndSaveForecast(site: Site, response: TomorrowForecastResponse) {
        val hourlyList = response.timelines?.hourly ?: return
        if (hourlyList.isEmpty()) return

        // 1. Parse current live weather (from first element or close to current hour)
        val currentItem = hourlyList[0]
        val currentVals = currentItem.values

        val temp = currentVals?.temperature ?: site.currentTemp
        val wind = currentVals?.windSpeed ?: site.currentWindSpeed
        val cloud = currentVals?.cloudCover ?: site.currentCloudCover
        val solar = currentVals?.solarGHI ?: site.currentSolarGHI

        // 2. Map actions & status
        var calculatedMode = site.currentMode
        var calculatedStatus = site.status

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMin = calendar.get(Calendar.MINUTE)
        val isNight = currentHour < 5 || currentHour >= 20

        if (wind > 50.0) {
            calculatedMode = "Stowed"
            calculatedStatus = "Storm mode"
        } else if (wind >= 30.0) {
            calculatedMode = "Safe mode"
            calculatedStatus = "Active"
        } else if (isNight) {
            calculatedMode = "Reset"
            calculatedStatus = "Active"
        } else if (cloud < 30.0) {
            calculatedMode = "Following sun"
            calculatedStatus = "Active"
        } else {
            calculatedMode = "Holding"
            calculatedStatus = "Active"
        }

        // Apply manual override
        val finalMode = when (site.manualOverride) {
            "Follow" -> "Following sun"
            "Hold" -> "Holding"
            "Stow" -> "Stowed"
            else -> calculatedMode
        }

        val finalAngle = when (finalMode) {
            "Following sun" -> calculateAngleForHour(currentHour)
            "Stowed", "Safe mode" -> 0f
            "Reset" -> -50f
            else -> site.currentAngle
        }

        // Update Site in DB
        val updatedSite = site.copy(
            currentTemp = temp,
            currentWindSpeed = wind,
            currentCloudCover = cloud,
            currentSolarGHI = solar,
            currentMode = finalMode,
            currentAngle = finalAngle,
            status = calculatedStatus,
            lastUpdated = System.currentTimeMillis()
        )
        solarDao.updateSite(updatedSite)

        // Generate Alert if needed
        if (wind > 50.0) {
            solarDao.insertAlert(
                Alert(
                    siteId = site.id,
                    siteName = site.name,
                    alertType = "Storm warning",
                    message = "Severe winds detected at ${site.name} (${String.format("%.1f", wind)} km/h). Panels auto-stowed for protection.",
                    severity = "High"
                )
            )
        }

        // 3. Save tomorrow's hourly predictions (5 AM to 8 PM)
        val forecastEntities = mutableListOf<ForecastHourEntity>()
        // Filter elements that are within tomorrow
        val tomorrowCal = Calendar.getInstance()
        tomorrowCal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(tomorrowCal.time)

        // Safe Tomorrow.io response dates usually are iso-8601 strings
        val itemsForForecast = hourlyList.filter { item ->
            val t = item.time ?: ""
            // Get Tomorrow's records
            t.startsWith(tomorrowDateStr)
        }.take(24)

        val formatInput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val formatOutput = SimpleDateFormat("hh:00 a", Locale.getDefault())

        for (item in (if (itemsForForecast.isNotEmpty()) itemsForForecast else hourlyList.take(24))) {
            val date = try {
                formatInput.parse(item.time ?: "")
            } catch (e: Exception) {
                Date()
            }
            val cal = Calendar.getInstance().apply { time = date ?: Date() }
            val hr = cal.get(Calendar.HOUR_OF_DAY)

            // Hour filter: 5AM to 8PM
            if (hr in 5..20) {
                val hourLabel = try {
                    formatOutput.format(date ?: Date()).uppercase()
                } catch (e: Exception) {
                    "${String.format("%02d", hr)}:00 ${if (hr < 12) "AM" else "PM"}"
                }

                val vals = item.values
                val fWind = vals?.windSpeed ?: 12.0
                val fCloud = vals?.cloudCover ?: 15.0
                val fPrecip = vals?.precipitationProbability ?: 0.0
                val fSolar = vals?.solarGHI ?: 400.0
                val fTemp = vals?.temperature ?: 28.0

                val (action, color) = calculateActionAndColor(hr, fWind, fPrecip, fCloud)

                forecastEntities.add(
                    ForecastHourEntity(
                        siteId = site.id,
                        hourTime = hourLabel,
                        temperature = fTemp,
                        windSpeed = fWind,
                        cloudCover = fCloud,
                        precipitationProbability = fPrecip,
                        solarGHI = fSolar,
                        weatherCondition = when {
                            fWind > 50.0 -> "storm"
                            fPrecip > 50.0 -> "rain"
                            fCloud > 70.0 -> "cloudy"
                            else -> "clear"
                        },
                        panelAction = action,
                        actionColor = color
                    )
                )
            }
        }

        if (forecastEntities.isNotEmpty()) {
            solarDao.deleteForecastForSite(site.id)
            solarDao.insertForecastHours(forecastEntities)
        }
    }

    private suspend fun simulateWeatherForSite(site: Site) {
        if (site.isManualSetup) return
        val randomFactor = (Math.random() - 0.5) * 5.0
        val baseTemp = site.currentTemp + randomFactor
        val minTemp = 24.0
        val maxTemp = 42.0
        val currentTemp = baseTemp.coerceIn(minTemp, maxTemp)

        // Wind simulation
        var currentWind = site.currentWindSpeed + (Math.random() - 0.5) * 6.0
        currentWind = currentWind.coerceIn(5.0, 65.0)

        // Cloud simulation
        var currentCloud = site.currentCloudCover + (Math.random() - 0.5) * 15.0
        currentCloud = currentCloud.coerceIn(0.0, 100.0)

        // Solar GHI calculation (based on clock hour and clouds)
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val solarBase = if (hour in 6..18) {
            val peakFactor = sin((hour - 6) * Math.PI / 12)
            solarIndexForRajkotHour(hour) * peakFactor
        } else {
            0.0
        }
        val cloudMod = (100.0 - currentCloud) / 100.0
        var currentSolar = (solarBase * cloudMod).coerceIn(0.0, 1000.0)

        // Mode logic
        var calculatedStatus = "Active"
        var calculatedMode = "Following sun"

        val isNight = hour < 6 || hour >= 19

        if (site.id == "3") { // Morbi Road Array is our preset high-wind storm site
            currentWind = 52.4 + Math.random() * 8.0
            currentCloud = 95.0
        }

        if (currentWind > 50.0) {
            calculatedMode = "Stowed"
            calculatedStatus = "Storm mode"
        } else if (currentWind >= 30.0) {
            calculatedMode = "Safe mode"
            calculatedStatus = "Active"
        } else if (isNight) {
            calculatedMode = "Reset"
            calculatedStatus = "Active"
        } else if (currentCloud < 30.0) {
            calculatedMode = "Following sun"
            calculatedStatus = "Active"
        } else if (currentCloud in 30.0..70.0) {
            calculatedMode = "Holding"
            calculatedStatus = "Active"
        } else {
            calculatedMode = "Holding" // Above 70% hold too
            calculatedStatus = "Active"
        }

        // Retain Fault and Offline states if pre-seeded
        if (site.status == "Fault") {
            calculatedStatus = "Fault"
            calculatedMode = "Holding"
        } else if (site.status == "Offline") {
            calculatedStatus = "Offline"
            calculatedMode = "Holding"
            currentWind = 0.0
            currentCloud = 0.0
            currentSolar = 0.0
        }

        // Apply manual override
        val finalMode = when (site.manualOverride) {
            "Follow" -> "Following sun"
            "Hold" -> "Holding"
            "Stow" -> "Stowed"
            else -> calculatedMode
        }

        val finalAngle = when (finalMode) {
            "Following sun" -> calculateAngleForHour(hour)
            "Stowed", "Safe mode" -> 0f
            "Reset" -> -50f
            else -> site.currentAngle
        }

        val updatedSite = site.copy(
            currentTemp = currentTemp,
            currentWindSpeed = currentWind,
            currentCloudCover = currentCloud,
            currentSolarGHI = currentSolar,
            currentMode = finalMode,
            currentAngle = finalAngle,
            status = calculatedStatus,
            lastUpdated = System.currentTimeMillis()
        )
        solarDao.updateSite(updatedSite)

        // Insert alert if winds are excessive
        if (currentWind > 50.0 && calculatedStatus == "Storm mode" && site.id == "3") {
            val alertCount = solarDao.getAllAlertsFlow().first().count { it.siteId == site.id && it.alertType == "Storm warning" }
            if (alertCount == 0) {
                solarDao.insertAlert(
                    Alert(
                        siteId = site.id,
                        siteName = site.name,
                        alertType = "Storm warning",
                        message = "High storm warning: Rajkot Northern quadrant winds exceeded 50 km/h. Morbi Road Array safe-stowed.",
                        severity = "High"
                    )
                )
            }
        }
    }

    private fun solarIndexForRajkotHour(hour: Int): Double {
        return when (hour) {
            12, 13, 14 -> 920.0
            11, 15 -> 810.0
            10, 16 -> 650.0
            9, 17 -> 480.0
            8, 18 -> 260.0
            7, 19 -> 120.0
            6, 20 -> 40.0
            else -> 0.0
        }
    }

    private fun calculateAngleForHour(hour: Int): Float {
        if (hour < 6 || hour >= 19) return -50f // reset to east
        // linear scaling: 6AM is -50 deg, 7PM (19) is +50 deg
        val totalHoursOfRun = 13.0f
        val currentOffset = (hour - 6).toFloat()
        return -50f + (100f * (currentOffset / totalHoursOfRun))
    }

    private fun calculateActionAndColor(
        hour: Int,
        windSpeed: Double,
        precipProb: Double,
        cloudCover: Double
    ): Pair<String, String> {
        val isNight = hour < 6 || hour >= 19
        return when {
            isNight -> Pair("Reset to east position", "gray")
            windSpeed > 50.0 -> Pair("Auto stow (Storm)", "red")
            precipProb > 80.0 -> Pair("Auto stow (Rain)", "red")
            windSpeed >= 30.0 -> Pair("Safe mode", "orange")
            cloudCover < 30.0 -> Pair("Follow sun", "green")
            else -> Pair("Hold angle", "blue")
        }
    }

    // Seed mock data for Rajkot solar systems
    private suspend fun seedInitialDataIfNeeded() {
        val existing = solarDao.getAllSites()
        if (existing.isNotEmpty()) return

        Log.d("SolarRepository", "Seeding initial data for Rajkot sites...")

        val initialSites = listOf(
            Site(
                id = "1",
                name = "Rajkot Solar East",
                latitude = 22.3039,
                longitude = 70.8022,
                currentTemp = 32.4,
                currentWindSpeed = 14.5,
                currentCloudCover = 12.0,
                currentSolarGHI = 820.0,
                currentAngle = calculateAngleForHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)),
                currentMode = "Following sun",
                status = "Active"
            ),
            Site(
                id = "2",
                name = "Metoda GIDC Plant",
                latitude = 22.2572,
                longitude = 70.7101,
                currentTemp = 34.1,
                currentWindSpeed = 25.1,
                currentCloudCover = 28.0,
                currentSolarGHI = 780.0,
                currentAngle = calculateAngleForHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)),
                currentMode = "Following sun",
                status = "Active"
            ),
            Site(
                id = "3",
                name = "Morbi Road Array",
                latitude = 22.3615,
                longitude = 70.8123,
                currentTemp = 26.5,
                currentWindSpeed = 55.2,
                currentCloudCover = 90.0,
                currentSolarGHI = 110.0,
                currentAngle = 0f,
                currentMode = "Stowed",
                status = "Storm mode"
            ),
            Site(
                id = "4",
                name = "Kalawad Road Hub",
                latitude = 22.2680,
                longitude = 70.7600,
                currentTemp = 30.2,
                currentWindSpeed = 32.5,
                currentCloudCover = 45.0,
                currentSolarGHI = 490.0,
                currentAngle = 0f,
                currentMode = "Safe mode",
                status = "Active"
            ),
            Site(
                id = "5",
                name = "Shapar Industrial Unit",
                latitude = 22.1852,
                longitude = 70.7938,
                currentTemp = 31.0,
                currentWindSpeed = 12.0,
                currentCloudCover = 15.0,
                currentSolarGHI = 0.0,
                currentAngle = 0f,
                currentMode = "Holding",
                status = "Offline"
            ),
            Site(
                id = "6",
                name = "Kuvadva Solar Field",
                latitude = 22.3789,
                longitude = 70.9254,
                currentTemp = 33.5,
                currentWindSpeed = 18.2,
                currentCloudCover = 32.0,
                currentSolarGHI = 610.0,
                currentAngle = 15f,
                currentMode = "Holding",
                status = "Fault"
            ),
            Site(
                id = "7",
                name = "Gondal Highway Station",
                latitude = 22.1120,
                longitude = 70.7845,
                currentTemp = 31.8,
                currentWindSpeed = 19.5,
                currentCloudCover = 60.0,
                currentSolarGHI = 430.0,
                currentAngle = -10f,
                currentMode = "Holding",
                status = "Active"
            ),
            Site(
                id = "8",
                name = "University Campus Array",
                latitude = 22.2855,
                longitude = 70.7424,
                currentTemp = 33.1,
                currentWindSpeed = 8.5,
                currentCloudCover = 5.0,
                currentSolarGHI = 850.0,
                currentAngle = calculateAngleForHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)),
                currentMode = "Following sun",
                status = "Active"
            )
        )

        solarDao.insertSites(initialSites)

        // Seed default alerts for faulty or offline systems
        solarDao.insertAlert(
            Alert(
                siteId = "3",
                siteName = "Morbi Road Array",
                alertType = "Storm warning",
                message = "High storm warning: Rajkot Northern quadrant winds exceeded 50 km/h. Morbi Road Array safe-stowed.",
                severity = "High",
                timestamp = System.currentTimeMillis() - 600000
            )
        )
        solarDao.insertAlert(
            Alert(
                siteId = "6",
                siteName = "Kuvadva Solar Field",
                alertType = "Motor fault",
                message = "Motor hardware failure: Axis gear lock issue on secondary panel row 3. Status set to HOLDING.",
                severity = "Medium",
                timestamp = System.currentTimeMillis() - 1500000
            )
        )
        solarDao.insertAlert(
            Alert(
                siteId = "5",
                siteName = "Shapar Industrial Unit",
                alertType = "Site offline",
                message = "Telemetry loss: Gateway failed to ping for past 45 minutes. Checking wireless transceivers.",
                severity = "Low",
                timestamp = System.currentTimeMillis() - 3600000
            )
        )

        // Seed 5AM to 8PM hour prediction lists for all 8 sites
        val conditions = listOf("clear", "clear", "cloudy", "rain", "storm", "clear")
        val hours = (5..20)
        for (site in initialSites) {
            val forecastList = mutableListOf<ForecastHourEntity>()
            for (hr in hours) {
                val hourStr = calendarHourLabel(hr)

                // Produce dynamic, realistic variation values per site
                val baseWind = if (site.id == "3") 42.0 else 10.0
                val hourlyWind = (baseWind + (hr % 5) * 4.5 + Math.random() * 2).coerceIn(4.0, 58.0)
                val hourlyCloud = ((hr * 4 + site.id.toInt() * 10) % 100).toDouble()
                val hourlyPrecip = if (hourlyCloud > 60 && hr % 3 == 0) 85.0 else 10.0
                val hourlyTemp = (25 + (hr % 4) * 3 + Math.random()).coerceIn(22.0, 41.0)

                val peakFactor = sin((hr - 6) * Math.PI / 12).coerceAtLeast(0.0)
                val solarValue = if (hr in 6..18) {
                    solarIndexForRajkotHour(hr) * peakFactor * ((100.0 - hourlyCloud) / 100.0)
                } else {
                    0.0
                }

                val (action, color) = calculateActionAndColor(hr, hourlyWind, hourlyPrecip, hourlyCloud)

                forecastList.add(
                    ForecastHourEntity(
                        siteId = site.id,
                        hourTime = hourStr,
                        temperature = hourlyTemp,
                        windSpeed = hourlyWind,
                        cloudCover = hourlyCloud,
                        precipitationProbability = hourlyPrecip,
                        solarGHI = solarValue,
                        weatherCondition = when {
                            hourlyWind > 50.0 -> "storm"
                            hourlyPrecip > 80.0 -> "rain"
                            hourlyCloud > 70.0 -> "cloudy"
                            else -> "clear"
                        },
                        panelAction = action,
                        actionColor = color
                    )
                )
            }
            solarDao.insertForecastHours(forecastList)
        }
    }

    private fun calendarHourLabel(hr: Int): String {
        return when {
            hr == 0 -> "12:00 AM"
            hr < 12 -> "${String.format("%2d", hrEntirely(hr))}:00 AM"
            hr == 12 -> "12:00 PM"
            else -> "${String.format("%2d", hrEntirely(hr - 12))}:00 PM"
        }.trim()
    }

    private fun hrEntirely(h: Int): Int {
        return if (h == 0) 12 else h
    }
}
