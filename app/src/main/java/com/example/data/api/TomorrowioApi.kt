package com.example.data.api

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface TomorrowioApi {
    @GET("v4/weather/forecast")
    suspend fun getForecast(
        @Query("location") location: String,
        @Query("fields") fields: String = "temperature,windSpeed,cloudCover,precipitationProbability,solarGHI",
        @Query("timesteps") timesteps: String = "1h",
        @Query("apikey") apikey: String
    ): TomorrowForecastResponse

    companion object {
        private const val BASE_URL = "https://api.tomorrow.io/"

        fun create(): TomorrowioApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(TomorrowioApi::class.java)
        }
    }
}
