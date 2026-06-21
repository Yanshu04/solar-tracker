package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TomorrowForecastResponse(
    val timelines: TomorrowTimelines?
)

@JsonClass(generateAdapter = true)
data class TomorrowTimelines(
    val hourly: List<TomorrowHourlyItem>?
)

@JsonClass(generateAdapter = true)
data class TomorrowHourlyItem(
    val time: String?,
    val values: TomorrowValues?
)

@JsonClass(generateAdapter = true)
data class TomorrowValues(
    val temperature: Double?,
    val windSpeed: Double?,
    val cloudCover: Double?,
    val precipitationProbability: Double?,
    val solarGHI: Double? = null,
    val uvIndex: Int? = null
)
