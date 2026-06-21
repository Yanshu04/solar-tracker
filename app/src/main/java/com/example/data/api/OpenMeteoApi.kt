package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    val latitude: Double?,
    val longitude: Double?,
    val hourly: OpenMeteoHourly?
)

@JsonClass(generateAdapter = true)
data class OpenMeteoHourly(
    val time: List<String>?,
    @Json(name = "temperature_2m") val temperature2m: List<Double>?,
    @Json(name = "wind_speed_10m") val windSpeed10m: List<Double>?,
    @Json(name = "cloud_cover") val cloudCover: List<Double>?,
    @Json(name = "precipitation_probability") val precipitationProbability: List<Double>?,
    @Json(name = "shortwave_radiation") val shortwaveRadiation: List<Double>?
)

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = "temperature_2m,wind_speed_10m,cloud_cover,precipitation_probability,shortwave_radiation",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 2
    ): OpenMeteoResponse

    companion object {
        private const val BASE_URL = "https://api.open-meteo.com/"

        fun create(): OpenMeteoApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(OpenMeteoApi::class.java)
        }
    }
}

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val results: List<GeocodingResult>?
)

@JsonClass(generateAdapter = true)
data class GeocodingResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    @Json(name = "country_code") val countryCode: String?,
    val admin1: String?
)

interface OpenMeteoGeocodingApi {
    @GET("v1/search")
    suspend fun searchCity(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): GeocodingResponse

    companion object {
        private const val BASE_URL = "https://geocoding-api.open-meteo.com/"

        fun create(): OpenMeteoGeocodingApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(OpenMeteoGeocodingApi::class.java)
        }
    }
}
