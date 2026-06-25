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

    init {
        // Run seed in IO coroutine context
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfNeeded()
            // Ensure data is refreshed immediately after seeding
            refreshWeather()
        }
    }

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
                finalAngle = calculateSolarAngle(site, Calendar.getInstance())
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
            status = status ?: site.status,
            lastUpdated = 0L // Force refresh
        )
        solarDao.updateSite(updatedSite)
    }

    // Refresh weather data: makes real network requests to Tomorrow.io or falling back to Open-Meteo
    suspend fun refreshWeather() = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.TOMORROW_API_KEY
        val hasRealKey = apiKey.isNotEmpty() && apiKey != "MY_TOMORROW_API_KEY"

        val sites = solarDao.getAllSites()
        val now = System.currentTimeMillis()
        val refreshInterval = 15 * 60 * 1000 // 15 minutes (Requirement TEST 12)

        for (site in sites) {
            // Only skip if data is fresh (under 5 mins) AND not a new site (lastUpdated != 0)
            if (site.lastUpdated != 0L && now - site.lastUpdated < refreshInterval) {
                continue 
            }

            if (site.isManualSetup) {
                Log.d("SolarRepository", "Skipping live forecast API fetch for manual site: ${site.name}")
                continue
            }
            try {
                if (hasRealKey) {
                    Log.d("SolarRepository", "Fetching real Tomorrow.io weather for ${site.name}")
                    val locationStr = "${site.latitude},${site.longitude}"
                    val response = api.getForecast(location = locationStr, apikey = apiKey)
                    Log.d("SolarRepository", "Tomorrow.io response: $response")
                    parseAndSaveForecast(site, response)
                } else {
                    Log.d("SolarRepository", "Fetching real Open-Meteo weather for ${site.name} at ${site.latitude},${site.longitude}")
                    val response = openMeteoApi.getForecast(
                        latitude = site.latitude,
                        longitude = site.longitude
                    )
                    Log.d("SolarRepository", "Open-Meteo response: $response")
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

        val nowMs = System.currentTimeMillis()
        var closestIndex = 0
        var minDiff = Long.MAX_VALUE

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone(response.timezone ?: "UTC")

        for (i in times.indices) {
            val tStr = times[i]
            val date = try { sdf.parse(tStr) } catch(_: Exception) { null }
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
        val solar = hourly.shortwaveRadiation?.getOrNull(closestIndex) ?: 0.0

        var calculatedMode = site.currentMode
        var calculatedStatus = site.status

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val isNight = isNightAtLocation(site, calendar)

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
            "Following sun" -> calculateSolarAngle(site, calendar)
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
            timezone = response.timezone ?: site.timezone,
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
                    message = "Severe winds detected at ${site.name} (${String.format(Locale.US, "%.1f", wind)} km/h). Panels auto-stowed for protection.",
                    severity = "High"
                )
            )
        }

        // 3. Save tomorrow's hourly predictions (5 AM to 8 PM)
        val forecastEntities = mutableListOf<ForecastHourEntity>()
        
        val targetTz = TimeZone.getTimeZone(site.timezone)
        val tomorrowCal = Calendar.getInstance(targetTz)
        tomorrowCal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowYear = tomorrowCal.get(Calendar.YEAR)
        val tomorrowDay = tomorrowCal.get(Calendar.DAY_OF_YEAR)

        val formatOutput = SimpleDateFormat("hh:00 a", Locale.US)
        formatOutput.timeZone = targetTz

        for (i in times.indices) {
            val tStr = times[i]
            val date = try { sdf.parse(tStr) } catch(e: Exception) { null }
            if (date != null) {
                val cal = Calendar.getInstance(targetTz).apply { time = date }
                
                if (cal.get(Calendar.YEAR) == tomorrowYear && cal.get(Calendar.DAY_OF_YEAR) == tomorrowDay) {
                    val hr = cal.get(Calendar.HOUR_OF_DAY)

                    if (hr in 5..20) {
                        val hourLabel = try {
                            formatOutput.format(date).uppercase()
                        } catch (e: Exception) {
                            "${String.format("%02d", if (hr % 12 == 0) 12 else hr % 12)}:00 ${if (hr < 12) "AM" else "PM"}"
                        }

                        val fTemp = hourly.temperature2m?.getOrNull(i) ?: 28.0
                        val fWind = hourly.windSpeed10m?.getOrNull(i) ?: 12.0
                        val fCloud = hourly.cloudCover?.getOrNull(i) ?: 15.0
                        val fPrecip = hourly.precipitationProbability?.getOrNull(i) ?: 0.0
                        val fSolar = hourly.shortwaveRadiation?.getOrNull(i) ?: 0.0

                        val (action, color) = calculateActionAndColor(site, cal, fWind, fPrecip, fCloud)

                        forecastEntities.add(
                            ForecastHourEntity(
                                siteId = site.id,
                                hourTime = hourLabel,
                                timestamp = date.time,
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
        // Tomorrow.io returns windSpeed in m/s, convert to km/h
        val wind = currentVals?.windSpeed?.let { it * 3.6 } ?: site.currentWindSpeed
        val cloud = currentVals?.cloudCover ?: site.currentCloudCover
        val solar = currentVals?.solarGHI ?: site.currentSolarGHI

        // 2. Map actions & status
        var calculatedMode = site.currentMode
        var calculatedStatus = site.status

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val isNight = isNightAtLocation(site, calendar)

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
            "Following sun" -> calculateSolarAngle(site, calendar)
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
                    message = "Severe winds detected at ${site.name} (${String.format(Locale.US, "%.1f", wind)} km/h). Panels auto-stowed for protection.",
                    severity = "High"
                )
            )
        }

        // 3. Save tomorrow's hourly predictions (5 AM to 8 PM)
        val forecastEntities = mutableListOf<ForecastHourEntity>()
        
        val targetTz = TimeZone.getTimeZone(site.timezone)
        val tomorrowCal = Calendar.getInstance(targetTz)
        tomorrowCal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowYear = tomorrowCal.get(Calendar.YEAR)
        val tomorrowDay = tomorrowCal.get(Calendar.DAY_OF_YEAR)

        val formatInput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val formatOutput = SimpleDateFormat("hh:00 a", Locale.US)
        formatOutput.timeZone = targetTz

        for (item in hourlyList) {
            val date = try {
                formatInput.parse(item.time ?: "")
            } catch (e: Exception) {
                null
            } ?: continue
            
            val cal = Calendar.getInstance(targetTz).apply { time = date }
            
            if (cal.get(Calendar.YEAR) == tomorrowYear && cal.get(Calendar.DAY_OF_YEAR) == tomorrowDay) {
                val hr = cal.get(Calendar.HOUR_OF_DAY)

                // Hour filter: 5AM to 8PM
                    if (hr in 5..20) {
                        val hourLabel = try {
                            formatOutput.format(date).uppercase()
                        } catch (e: Exception) {
                            "${String.format("%02d", if (hr % 12 == 0) 12 else hr % 12)}:00 ${if (hr < 12) "AM" else "PM"}"
                        }

                        val vals = item.values
                // Tomorrow.io returns windSpeed in m/s, convert to km/h
                val fWind = vals?.windSpeed?.let { it * 3.6 } ?: 12.0
                val fCloud = vals?.cloudCover ?: 15.0
                val fPrecip = vals?.precipitationProbability ?: 0.0
                val fSolar = vals?.solarGHI ?: 0.0
                val fTemp = vals?.temperature ?: 28.0

                val (action, color) = calculateActionAndColor(site, cal, fWind, fPrecip, fCloud)

                forecastEntities.add(
                    ForecastHourEntity(
                        siteId = site.id,
                        hourTime = hourLabel,
                        timestamp = date?.time ?: System.currentTimeMillis(),
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
            }

        if (forecastEntities.isNotEmpty()) {
            solarDao.deleteForecastForSite(site.id)
            solarDao.insertForecastHours(forecastEntities)
        }
    }

    private suspend fun simulateWeatherForSite(site: Site) {
        if (site.isManualSetup) return
        Log.w("SolarRepository", "Simulating weather for ${site.name} (Falling back from API failure)")
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
            baseSolarIndexByHour(hour) * peakFactor
        } else {
            0.0
        }
        val cloudMod = (100.0 - currentCloud) / 100.0
        var currentSolar = (solarBase * cloudMod).coerceIn(0.0, 1000.0)

        // Mode logic
        var calculatedStatus = "Active"
        var calculatedMode = "Following sun"

        val isNight = isNightAtLocation(site, cal)

        if (site.id == "global_3") { // Sahara Desert Plant is a good candidate for high-wind simulation if needed, or use a more descriptive check
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
            "Following sun" -> calculateSolarAngle(site, cal)
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
        if (currentWind > 50.0 && calculatedStatus == "Storm mode" && site.id == "global_3") {
            val alertCount = solarDao.getAllAlertsFlow().first().count { it.siteId == site.id && it.alertType == "Storm warning" }
            if (alertCount == 0) {
                solarDao.insertAlert(
                    Alert(
                        siteId = site.id,
                        siteName = site.name,
                        alertType = "Storm warning",
                        message = "High storm warning: Local winds exceeded 50 km/h. Array safe-stowed.",
                        severity = "High"
                    )
                )
            }
        }
    }

    private fun baseSolarIndexByHour(hour: Int): Double {
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

    /**
     * Calculate solar tracking angle based on site longitude and local time.
     * Angle range: -50 (East) to +50 (West)
     */
    private fun calculateSolarAngle(site: Site, calendar: Calendar): Float {
        // Solar time = local time + (4 * longitude + timezone_adjustment)
        // Simplified: Local Solar Time (LST)
        // Each 15 degrees of longitude is ~1 hour.
        
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcCalendar.time = calendar.time
        
        val utcHour = utcCalendar.get(Calendar.HOUR_OF_DAY)
        val utcMinute = utcCalendar.get(Calendar.MINUTE)
        val utcDecimalHour = utcHour + (utcMinute / 60f)
        
        // Solar decimal hour = UTC decimal hour + (longitude / 15)
        var solarDecimalHour = utcDecimalHour + (site.longitude / 15f)
        if (solarDecimalHour < 0) solarDecimalHour += 24f
        if (solarDecimalHour >= 24) solarDecimalHour -= 24f
        
        // Tracking from 6 AM (-50 deg) to 6 PM (+50 deg) solar time
        // 12 PM solar time is 0 degrees.
        if (solarDecimalHour < 6f || solarDecimalHour >= 18f) return -50f // Reset to east at night
        
        // Map 6..18 to -50..+50
        return ((solarDecimalHour - 12f) * (100f / 12f)).toFloat()
    }

    private fun isNightAtLocation(site: Site, calendar: Calendar): Boolean {
        val angle = calculateSolarAngle(site, calendar)
        return angle == -50f
    }

    private fun calculateActionAndColor(
        site: Site,
        calendar: Calendar,
        windSpeed: Double,
        precipProb: Double,
        cloudCover: Double
    ): Pair<String, String> {
        val isNight = isNightAtLocation(site, calendar)
        return when {
            windSpeed > 50.0 -> Pair("Auto stow (Storm)", "red")
            precipProb > 80.0 -> Pair("Auto stow (Rain)", "red")
            isNight -> Pair("Reset to east position", "gray")
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
                id = "global_1",
                name = "London Solar Hub",
                latitude = 51.5074,
                longitude = -0.1278,
                currentTemp = 18.4,
                currentWindSpeed = 12.5,
                currentCloudCover = 45.0,
                currentSolarGHI = 320.0,
                currentAngle = calculateSolarAngle(Site("", "", 51.5074, -0.1278, 0.0, 0.0, 0.0, 0.0), Calendar.getInstance()),
                currentMode = "Following sun",
                status = "Active",
                timezone = "Europe/London",
                lastUpdated = 0L
            ),
            Site(
                id = "global_2",
                name = "New York Array",
                latitude = 40.7128,
                longitude = -74.0060,
                currentTemp = 24.1,
                currentWindSpeed = 15.1,
                currentCloudCover = 28.0,
                currentSolarGHI = 580.0,
                currentAngle = calculateSolarAngle(Site("", "", 40.7128, -74.0060, 0.0, 0.0, 0.0, 0.0), Calendar.getInstance()),
                currentMode = "Following sun",
                status = "Active",
                timezone = "America/New_York",
                lastUpdated = 0L
            ),
            Site(
                id = "global_3",
                name = "Sahara Desert Plant",
                latitude = 23.8000,
                longitude = 11.3000,
                currentTemp = 42.5,
                currentWindSpeed = 25.2,
                currentCloudCover = 5.0,
                currentSolarGHI = 910.0,
                currentAngle = calculateSolarAngle(Site("", "", 23.8000, 11.3000, 0.0, 0.0, 0.0, 0.0), Calendar.getInstance()),
                currentMode = "Following sun",
                status = "Active",
                timezone = "Africa/Cairo",
                lastUpdated = 0L
            ),
            Site(
                id = "global_4",
                name = "Tokyo Research Hub",
                latitude = 35.6762,
                longitude = 139.6503,
                currentTemp = 28.2,
                currentWindSpeed = 10.5,
                currentCloudCover = 65.0,
                currentSolarGHI = 410.0,
                currentAngle = 0f,
                currentMode = "Safe mode",
                status = "Active",
                timezone = "Asia/Tokyo",
                lastUpdated = 0L
            ),
            Site(
                id = "global_5",
                name = "Sydney Energy Unit",
                latitude = -33.8688,
                longitude = 151.2093,
                currentTemp = 21.0,
                currentWindSpeed = 18.0,
                currentCloudCover = 15.0,
                currentSolarGHI = 0.0,
                currentAngle = 0f,
                currentMode = "Holding",
                status = "Offline",
                timezone = "Australia/Sydney",
                lastUpdated = 0L
            ),
            Site(
                id = "global_6",
                name = "Atacama Solar Field",
                latitude = -23.8634,
                longitude = -69.1359,
                currentTemp = 33.5,
                currentWindSpeed = 12.2,
                currentCloudCover = 2.0,
                currentSolarGHI = 980.0,
                currentAngle = 15f,
                currentMode = "Holding",
                status = "Fault",
                timezone = "America/Santiago",
                lastUpdated = 0L
            ),
            Site(
                id = "global_7",
                name = "Srinagar Grid Station",
                latitude = 34.0837,
                longitude = 74.7973,
                currentTemp = 22.8,
                currentWindSpeed = 8.5,
                currentCloudCover = 40.0,
                currentSolarGHI = 530.0,
                currentAngle = -10f,
                currentMode = "Holding",
                status = "Active",
                timezone = "Asia/Kolkata",
                lastUpdated = 0L
            ),
            Site(
                id = "global_8",
                name = "Rajkot Solar Center",
                latitude = 22.3039,
                longitude = 70.8022,
                currentTemp = 33.1,
                currentWindSpeed = 8.5,
                currentCloudCover = 5.0,
                currentSolarGHI = 850.0,
                currentAngle = calculateSolarAngle(Site("", "", 22.3039, 70.8022, 0.0, 0.0, 0.0, 0.0), Calendar.getInstance()),
                currentMode = "Following sun",
                status = "Active",
                timezone = "Asia/Kolkata",
                lastUpdated = 0L
            )
        )

        solarDao.insertSites(initialSites)

        // Seed default alerts for faulty or offline systems
        solarDao.insertAlert(
            Alert(
                siteId = "global_4",
                siteName = "Tokyo Research Hub",
                alertType = "Storm warning",
                message = "High storm warning: Local winds exceeded 50 km/h. Tokyo array safe-stowed.",
                severity = "High",
                timestamp = System.currentTimeMillis() - 600000
            )
        )
        solarDao.insertAlert(
            Alert(
                siteId = "global_6",
                siteName = "Atacama Solar Field",
                alertType = "Motor fault",
                message = "Motor hardware failure: Axis gear lock issue on secondary panel row 3. Status set to HOLDING.",
                severity = "Medium",
                timestamp = System.currentTimeMillis() - 1500000
            )
        )
        solarDao.insertAlert(
            Alert(
                siteId = "global_5",
                siteName = "Sydney Energy Unit",
                alertType = "Site offline",
                message = "Telemetry loss: Gateway failed to ping for past 45 minutes. Checking wireless transceivers.",
                severity = "Low",
                timestamp = System.currentTimeMillis() - 3600000
            )
        )

        // Seed 5AM to 8PM hour prediction lists for all 8 sites
        val hours = (5..20)
        val now = Calendar.getInstance()
        val tomorrow = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        
        for (site in initialSites) {
            val forecastList = mutableListOf<ForecastHourEntity>()
            for (hr in hours) {
                val hourStr = calendarHourLabel(hr)
                val forecastTime = (tomorrow.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, hr)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // Produce dynamic, realistic variation values per site
                val siteSeed = try { site.id.toInt() } catch (e: Exception) { site.id.hashCode() }
                val baseWind = if (site.id.contains("3")) 42.0 else 10.0
                val hourlyWind = (baseWind + (hr % 5) * 4.5 + Math.random() * 2).coerceIn(4.0, 58.0)
                val hourlyCloud = ((hr * 4 + siteSeed * 10) % 100).toDouble().let { if (it < 0) -it else it }
                val hourlyPrecip = if (hourlyCloud > 60 && hr % 3 == 0) 85.0 else 10.0
                val hourlyTemp = (25 + (hr % 4) * 3 + Math.random()).coerceIn(22.0, 41.0)

                val peakFactor = sin((hr - 6) * Math.PI / 12).coerceAtLeast(0.0)
                val solarValue = if (hr in 6..18) {
                    baseSolarIndexByHour(hr) * peakFactor * ((100.0 - hourlyCloud) / 100.0)
                } else {
                    0.0
                }

                val (action, color) = calculateActionAndColor(site, forecastTime, hourlyWind, hourlyPrecip, hourlyCloud)

                forecastList.add(
                    ForecastHourEntity(
                        siteId = site.id,
                        hourTime = hourStr,
                        timestamp = forecastTime.timeInMillis,
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
        
        // After seeding initial sites, perform an immediate real weather refresh from API
        refreshWeather()
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
