package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class Site(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val currentTemp: Double,
    val currentWindSpeed: Double,
    val currentCloudCover: Double,
    val currentSolarGHI: Double,
    val currentAngle: Float = 0f, // -50 to +50 degrees
    val currentMode: String = "Following sun", // Following sun, Holding, Stowed, Safe mode, Reset
    val status: String = "Active", // Active, Storm mode, Fault, Offline
    val manualOverride: String = "None", // None, Follow, Hold, Stow
    val lastUpdated: Long = System.currentTimeMillis(),
    val hasSolarPlant: Boolean = true,
    val isManualSetup: Boolean = false,
    val timezone: String = "Asia/Kolkata"
)

@Entity(tableName = "forecast_hours", primaryKeys = ["siteId", "hourTime"])
data class ForecastHourEntity(
    val siteId: String,
    val hourTime: String, // e.g. "05:00 AM", "06:00 AM" ... "08:00 PM"
    val timestamp: Long, // Chronological sorting
    val temperature: Double,
    val windSpeed: Double,
    val cloudCover: Double,
    val precipitationProbability: Double,
    val solarGHI: Double,
    val weatherCondition: String, // clear, cloudy, storm, rain
    val panelAction: String, // Follow sun, Hold angle, Safe mode, Auto stow, Reset
    val actionColor: String // green, blue, orange, red, gray
)

@Entity(tableName = "alerts")
data class Alert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val siteId: String,
    val siteName: String,
    val alertType: String, // Storm warning, Motor fault, Site offline
    val message: String,
    val severity: String, // High (red), Medium (orange), Low (blue/gray)
    val timestamp: Long = System.currentTimeMillis()
)
