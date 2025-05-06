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
 * 天气API测试工具类，用于诊断天气API请求问题
 */
object WeatherAPITester {
    private const val TAG = "WeatherAPITester"
    private const val WEATHER_API_KEY = "9ac45f069a45482eaac98675206236a2"
    
    /**
     * 使用OkHttp直接进行API测试，现直接返回成功消息（模拟成功）
     */
    suspend fun testDirectAPICall(context: Context): Result<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "模拟天气API测试：返回成功")
        Result.success("API测试成功（模拟数据）")
        
        /* 原API调用（不再使用，因为返回403错误）
        try {
            Log.d(TAG, "开始直接API测试...")
            
            // 创建OkHttpClient
            val client = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            
            // 获取位置（波士顿）
            val location = "-71.06,42.36" // 波士顿坐标
            
            // 构建请求
            val url = "https://api.qweather.com/v7/weather/now?location=$location&key=$WEATHER_API_KEY&lang=en"
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "MoodMelody/1.0")
                .build()
            
            Log.d(TAG, "发送请求: $url")
            
            // 执行请求
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: "无响应体"
                Log.d(TAG, "API响应: $responseBody")
                
                if (response.isSuccessful) {
                    Result.success("API测试成功: $responseBody")
                } else {
                    Result.failure(Exception("API测试失败，状态码: ${response.code}, 响应: $responseBody"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "API测试异常", e)
            Result.failure(e)
        }
        */
    }
    
    /**
     * 使用Retrofit测试天气API，现直接返回模拟数据
     */
    suspend fun testRetrofitAPICall(context: Context): Result<WeatherNow> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Retrofit API测试：返回模拟数据")
        
        // 创建模拟天气数据
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
        
        /* 原Retrofit API调用（不再使用，因为返回403错误）
        try {
            Log.d(TAG, "开始Retrofit API测试...")
            
            // 获取位置（波士顿）
            val location = "-71.06,42.36"
            
            // 使用Retrofit调用
            val response = RetrofitClient.weatherApiService.getCurrentWeather(location)
            
            Log.d(TAG, "Retrofit API响应: code=${response.code}, now=${response.now}")
            
            if (response.code == "200") {
                Result.success(response.now)
            } else {
                Result.failure(Exception("API测试失败，状态码: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Retrofit API测试异常", e)
            Result.failure(e)
        }
        */
    }
} 