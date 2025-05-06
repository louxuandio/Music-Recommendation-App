package com.example.moodmelody.network

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("v7/weather/now")
    suspend fun getCurrentWeather(
        @Query("location") location: String,
        @Query("lang") lang: String = "en"
    ): WeatherResponse
}

// 和风天气API响应模型
data class WeatherResponse(
    val code: String,
    val updateTime: String,
    val fxLink: String,
    val now: WeatherNow,
    val refer: WeatherRefer
)

data class WeatherNow(
    val obsTime: String,
    val temp: String,
    val feelsLike: String,
    val icon: String,
    val text: String,
    val wind360: String,
    val windDir: String,
    val windScale: String,
    val windSpeed: String,
    val humidity: String,
    val precip: String,
    val pressure: String,
    val vis: String,
    val cloud: String,
    val dew: String,
    val cityName: String? = null
)

data class WeatherRefer(
    val sources: List<String>,
    val license: List<String>
)