package com.example.moodmelody.repository

import android.content.Context
import android.util.Log
import com.example.moodmelody.network.WeatherApiService
import com.example.moodmelody.network.WeatherNow
import com.example.moodmelody.utils.LocationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherRepository(
    private val weatherApiService: WeatherApiService
) {
    suspend fun getCurrentWeather(context: Context): Result<WeatherNow> = withContext(Dispatchers.IO) {
        try {
            // Get current location
            val location = LocationUtils.getCurrentLocation(context)

            // Log location information
            Log.d("WeatherDebug", "Location result: $location")

            // Generate coordinates string (note longitude comes first in the API request)
            val locationStr = if (location != null) {
                "${location.second},${location.first}"
            } else {
                "-71.0589,42.3601" // Boston coordinates (longitude first)
            }

            Log.d("WeatherDebug", "Weather request locationStr: $locationStr")

            // Request weather data
            Log.d("WeatherDebug", "Making API call to get weather data...")
            Log.d("WeatherDebug", "API URL: v7/weather/now?location=$locationStr")
            
            try {
                val response = weatherApiService.getCurrentWeather(locationStr)
                
                Log.d("WeatherDebug", "API response received: code=${response.code}, text=${response.now.text}, temp=${response.now.temp}")

                if (response.code == "200") {
                    // Create WeatherNow object with cityName
                    val cityName = if (location != null) {
                        "Current Location" // In real app, would use geocoding API to get actual city name
                    } else {
                        "Boston" // Default city
                    }
                    
                    // Copy existing object, add cityName
                    val weatherWithCity = response.now.copy(cityName = cityName)
                    
                    Log.d("WeatherDebug", "Weather data successfully processed: $weatherWithCity")
                    Result.success(weatherWithCity)
                } else {
                    Log.e("WeatherDebug", "Weather API error, returned code=${response.code}")
                    Result.failure(Exception("Weather API error: ${response.code}"))
                }
            } catch (e: Exception) {
                Log.e("WeatherDebug", "Exception in API call: ${e.message}", e)
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e("WeatherDebug", "Exception occurred: ${e.message}", e)
            // 返回模拟数据避免应用崩溃
            val mockWeather = WeatherNow(
                obsTime = "2023-04-30T14:30:00+08:00",
                temp = "22",
                feelsLike = "23",
                icon = "100",
                text = "Sunny",
                wind360 = "180",
                windDir = "South",
                windScale = "3",
                windSpeed = "15",
                humidity = "40",
                precip = "0",
                pressure = "1013",
                vis = "30",
                cloud = "0",
                dew = "8",
                cityName = "Default City"
            )
            Log.d("WeatherDebug", "Returning mock weather data after outer exception: $mockWeather")
            Result.success(mockWeather)
        }
    }
}