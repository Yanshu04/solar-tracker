package com.example.data.db

import androidx.room.*
import com.example.data.model.Alert
import com.example.data.model.ForecastHourEntity
import com.example.data.model.Site
import kotlinx.coroutines.flow.Flow

@Dao
interface SolarDao {
    @Query("SELECT * FROM sites ORDER BY name ASC")
    fun getAllSitesFlow(): Flow<List<Site>>

    @Query("SELECT * FROM sites ORDER BY name ASC")
    suspend fun getAllSites(): List<Site>

    @Query("SELECT * FROM sites WHERE id = :siteId")
    fun getSiteByIdFlow(siteId: String): Flow<Site?>

    @Query("SELECT * FROM sites WHERE id = :siteId")
    suspend fun getSiteById(siteId: String): Site?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSites(sites: List<Site>)

    @Update
    suspend fun updateSite(site: Site)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: Site)

    // Forecasts Queries
    @Query("SELECT * FROM forecast_hours WHERE siteId = :siteId ORDER BY timestamp ASC")
    fun getForecastForSiteFlow(siteId: String): Flow<List<ForecastHourEntity>>

    @Query("SELECT * FROM forecast_hours WHERE siteId = :siteId ORDER BY timestamp ASC")
    suspend fun getForecastForSite(siteId: String): List<ForecastHourEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForecastHours(hours: List<ForecastHourEntity>)

    @Query("DELETE FROM forecast_hours WHERE siteId = :siteId")
    suspend fun deleteForecastForSite(siteId: String)

    // Alerts Queries
    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAllAlertsFlow(): Flow<List<Alert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: Alert)

    @Query("DELETE FROM alerts WHERE id = :id")
    suspend fun deleteAlertById(id: Int)

    @Query("DELETE FROM alerts")
    suspend fun clearAllAlerts()
}
