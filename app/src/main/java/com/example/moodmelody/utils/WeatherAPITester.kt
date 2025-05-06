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
    private const val WEATHER_API_KEY = "9dcd04aca58344429b8e4d7ec4c0ecd3"
    
    /**
     * 使用OkHttp直接进行API测试，绕过Retrofit
     */
    suspend fun testDirectAPICall(context: Context): Result<String> = withContext(Dispatchers.IO) {
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
            
            // 获取位置（北京）
            val location = "116.41,39.92" // 北京坐标
            
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
    }
    
    /**
     * 使用Retrofit测试天气API
     */
    suspend fun testRetrofitAPICall(context: Context): Result<WeatherNow> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始Retrofit API测试...")
            
            // 获取位置（北京）
            val location = "116.41,39.92"
            
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
    }
} 