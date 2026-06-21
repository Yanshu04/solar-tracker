package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Alert
import com.example.data.model.ForecastHourEntity
import com.example.data.model.Site
import com.example.data.repository.SolarRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SolarViewModel(
    application: Application,
    private val repository: SolarRepository
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
    private val _selectedSiteId = MutableStateFlow("1")
    val selectedSiteId = _selectedSiteId.asStateFlow()

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
        // Initial refresh
        refreshWeather()
    }

    fun selectSite(siteId: String) {
        _selectedSiteId.value = siteId
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
        status: String = "Active"
    ) {
        viewModelScope.launch {
            val uniqueId = java.util.UUID.randomUUID().toString()
            val newSite = com.example.data.model.Site(
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
                lastUpdated = System.currentTimeMillis(),
                hasSolarPlant = hasSolarPlant,
                isManualSetup = isManualSetup
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

    // Provider Factory mapping
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val database = AppDatabase.getDatabase(application)
            val repository = SolarRepository(application, database.solarDao())
            return SolarViewModel(application, repository) as T
        }
    }
}
