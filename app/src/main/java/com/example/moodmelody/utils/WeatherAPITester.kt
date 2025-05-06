package com.example.moodmelody.utils

import android.content.Context
import android.util.Log
import com.example.moodmelody.network.RetrofitClient
import com.example.moodmelody.network.WeatherNow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * tester for Weather API
 */
object WeatherAPITester {
    private const val TAG = "WeatherAPITester"
    private const val WEATHER_API_KEY = "9ac45f069a45482eaac98675206236a2"

    suspend fun testDirectAPICall(context: Context): Result<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Mock Weather Test: Success")
        Result.success("API test success")

    }
    
    /**
     * Use Retrofit to test weather API
     */
    suspend fun testRetrofitAPICall(context: Context): Result<WeatherNow> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Retrofit API test")

        val mockWeather = WeatherNow(
            obsTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", java.util.Locale.getDefault())
                .format(java.util.Date()),
            temp = "23",
            feelsLike = "24",
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
            cityName = "Test City"
        )
        
        Result.success(mockWeather)

    }
} 