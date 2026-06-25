package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Alert
import com.example.data.model.ForecastHourEntity
import com.example.data.model.Site
import com.example.data.repository.SolarRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.example.BuildConfig
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SolarViewModel(
    application: Application,
    private val repository: SolarRepository,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    // All available sites
    val sites: StateFlow<List<Site>> = repository.allSites
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // All alert logs
    val alerts: StateFlow<List<Alert>> = repository.allAlerts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Refreshing status indicator
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    // Screen navigation tracking for current selected site detail view
    // Persist selected ID across process death
    private val _selectedSiteId = savedStateHandle.getStateFlow("selected_site_id", "global_8")
    val selectedSiteId = _selectedSiteId

    // Detail Site
    val selectedSite: StateFlow<Site?> = _selectedSiteId
        .flatMapLatest { id -> repository.getSiteById(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Forecast items for selected site
    val selectedSiteForecast: StateFlow<List<ForecastHourEntity>> = _selectedSiteId
        .flatMapLatest { id -> repository.getForecastForSite(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Initial refresh and ensure a site is selected
        viewModelScope.launch {
            sites.collectLatest { siteList ->
                if (siteList.isNotEmpty() && _selectedSiteId.value == "global_8") {
                    // If default isn't found, pick the first one
                    if (siteList.none { it.id == "global_8" }) {
                        savedStateHandle["selected_site_id"] = siteList.first().id
                    }
                }
            }
        }
        refreshWeather()
    }

    fun selectSite(siteId: String) {
        savedStateHandle["selected_site_id"] = siteId
        _aiInsight.value = null // Clear previous AI result when site changes
    }

    fun refreshWeather() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.refreshWeather()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun applyManualOverride(siteId: String, override: String) {
        viewModelScope.launch {
            repository.updateSiteOverride(siteId, override)
        }
    }

    fun updateSiteLocation(
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
    ) {
        viewModelScope.launch {
            repository.updateSiteLocation(
                siteId = siteId,
                name = name,
                latitude = latitude,
                longitude = longitude,
                hasSolarPlant = hasSolarPlant,
                isManualSetup = isManualSetup,
                temp = temp,
                wind = wind,
                cloud = cloud,
                ghi = ghi,
                status = status
            )
            // Note: repository.updateSiteLocation doesn't currently update timezone, 
            // but refreshWeather will fetch it from API if not manual.
            if (!isManualSetup) {
                refreshWeather()
            }
        }
    }

    fun insertCustomSite(
        name: String,
        latitude: Double,
        longitude: Double,
        hasSolarPlant: Boolean = true,
        isManualSetup: Boolean = false,
        temp: Double = 24.5,
        wind: Double = 12.0,
        cloud: Double = 15.0,
        ghi: Double = 350.0,
        status: String = "Active",
        timezone: String = "Asia/Kolkata"
    ) {
        viewModelScope.launch {
            val uniqueId = java.util.UUID.randomUUID().toString()
            val newSite = Site(
                id = uniqueId,
                name = name,
                latitude = latitude,
                longitude = longitude,
                currentTemp = temp,
                currentWindSpeed = wind,
                currentCloudCover = cloud,
                currentSolarGHI = ghi,
                currentAngle = 0f,
                currentMode = if (status == "Offline") "Holding" else "Following sun",
                status = status,
                manualOverride = "None",
                lastUpdated = 0L, // Set to 0 to trigger immediate weather fetch
                hasSolarPlant = hasSolarPlant,
                isManualSetup = isManualSetup,
                timezone = timezone
            )
            repository.insertSite(newSite)
            if (!isManualSetup) {
                refreshWeather()
            }
        }
    }

    fun postCustomAlert(siteId: String, siteName: String, alertType: String, message: String, severity: String) {
        viewModelScope.launch {
            repository.insertAlert(
                Alert(
                    siteId = siteId,
                    siteName = siteName,
                    alertType = alertType,
                    message = message,
                    severity = severity
                )
            )
        }
    }

    fun clearAlerts() {
        viewModelScope.launch {
            repository.clearAllAlerts()
        }
    }

    // --- Weather Explorer state ---
    private val _exploreSearchQuery = MutableStateFlow("")
    val exploreSearchQuery = _exploreSearchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<com.example.data.api.GeocodingResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    // Current explored/loaded weather state
    private val _exploredWeather = MutableStateFlow<com.example.data.api.OpenMeteoResponse?>(null)
    val exploredWeather = _exploredWeather.asStateFlow()

    private val _isExploreLoading = MutableStateFlow(false)
    val isExploreLoading = _isExploreLoading.asStateFlow()

    // --- AI Insights State ---
    private val _aiInsight = MutableStateFlow<String?>(null)
    val aiInsight = _aiInsight.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading = _isAiLoading.asStateFlow()

    private val _gpsStatus = MutableStateFlow<String?>(null)
    val gpsStatus = _gpsStatus.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    fun generateAiInsight(site: Site, forecast: List<ForecastHourEntity>) {
        if (BuildConfig.GEMINI_API_KEY == "ADD_YOUR_GEMINI_KEY_HERE" || BuildConfig.GEMINI_API_KEY.isEmpty()) {
            _aiInsight.value = "Please add a valid Gemini API key to your .env file."
            return
        }

        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                // Take only daylight hours to keep prompt concise
                val forecastSummary = forecast.take(12).joinToString("\n") { 
                    "${it.hourTime}: Temp ${it.temperature}°C, GHI ${it.solarGHI}W/m², ${it.weatherCondition}"
                }
                
                val prompt = """
                    You are a solar energy expert. Analyze this forecast for '${site.name}':
                    $forecastSummary
                    Site Status: ${site.status}
                    Provide a 2-sentence outlook on tomorrow's production and a technical tip.
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                _aiInsight.value = response.text
            } catch (e: Exception) {
                val errorMsg = e.message ?: ""
                _aiInsight.value = when {
                    errorMsg.contains("404") -> "Model not found. Please ensure 'Gemini API' is enabled in your Google AI Studio project."
                    errorMsg.contains("429") -> "AI rate limit reached. Please try again in a minute."
                    else -> "AI Error: ${e.localizedMessage ?: "Connection failed"}"
                }
                e.printStackTrace()
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    private val _exploredLocationName = MutableStateFlow("")
    val exploredLocationName = _exploredLocationName.asStateFlow()

    private val _exploredLat = MutableStateFlow(0.0)
    val exploredLat = _exploredLat.asStateFlow()

    private val _exploredLng = MutableStateFlow(0.0)
    val exploredLng = _exploredLng.asStateFlow()

    fun updateExploreQuery(query: String) {
        _exploreSearchQuery.value = query
        if (query.trim().length < 2) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            try {
                _searchResults.value = repository.searchCity(query)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun loadExploredWeather(name: String, lat: Double, lng: Double) {
        _exploredLocationName.value = name
        _exploredLat.value = lat
        _exploredLng.value = lng
        viewModelScope.launch {
            _isExploreLoading.value = true
            try {
                _exploredWeather.value = repository.fetchRawWeather(lat, lng)
            } catch (e: Exception) {
                e.printStackTrace()
                _exploredWeather.value = null
            } finally {
                _isExploreLoading.value = false
            }
        }
    }

    fun clearExploredWeather() {
        _exploredWeather.value = null
        _exploredLocationName.value = ""
        _exploredLat.value = 0.0
        _exploredLng.value = 0.0
    }

    fun getCurrentLocation(onLocationFound: (Double, Double) -> Unit) {
        val application = getApplication<Application>()
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
        val locationManager = application.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager

        // Check if GPS or Network location is actually enabled
        val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            _gpsStatus.value = "GPS is OFF. Please turn it ON."
            android.util.Log.e("GPS_DEBUG", "Location services are disabled on the device.")
            return
        }
        
        _gpsStatus.value = "Detecting live location..."
        try {
            // ONLY request a fresh high-accuracy location (Removing cached fallback)
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        android.util.Log.d("GPS_DEBUG", "Found fresh location: ${location.latitude}, ${location.longitude}")
                        _gpsStatus.value = "Live location detected"
                        onLocationFound(location.latitude, location.longitude)
                    } else {
                        _gpsStatus.value = "Waiting for satellite fix..."
                        android.util.Log.w("GPS_DEBUG", "Location is null - signal might be weak.")
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("GPS_DEBUG", "Location request failed", e)
                    _gpsStatus.value = "GPS Error: Check connection"
                }
        } catch (e: SecurityException) {
            android.util.Log.e("GPS_DEBUG", "SecurityException: Permission missing", e)
            _gpsStatus.value = "Permission denied"
        }
    }

    // Provider Factory mapping
    class Factory(
        private val application: Application,
        private val owner: androidx.savedstate.SavedStateRegistryOwner,
        private val defaultArgs: android.os.Bundle? = null
    ) : androidx.lifecycle.AbstractSavedStateViewModelFactory(owner, defaultArgs) {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T {
            val database = AppDatabase.getDatabase(application)
            val repository = SolarRepository(application, database.solarDao())
            return SolarViewModel(application, repository, handle) as T
        }
    }
}
