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
            // 获取当前位置
            val location = LocationUtils.getCurrentLocation(context)

            // 打印定位信息
            Log.d("WeatherDebug", "定位结果：$location")

            // 生成坐标字符串（注意经纬度顺序）
            val locationStr = if (location != null) {
                "${location.second},${location.first}"
            } else {
                "-71.0997,42.3496" // BU坐标（经度在前）
            }

            Log.d("WeatherDebug", "请求天气的 locationStr: $locationStr")

            // 请求天气数据
            val response = weatherApiService.getCurrentWeather(locationStr)

            Log.d("WeatherDebug", "接口返回 code=${response.code}, text=${response.now.text}, temp=${response.now.temp}")

            if (response.code == "200") {
                // 创建带有cityName的WeatherNow对象
                val cityName = if (location != null) {
                    "Current Location" // 实际情况可以用地理编码API获取真实城市名
                } else {
                    "Boston" // 默认城市
                }
                
                // 复制现有对象，添加cityName
                val weatherWithCity = response.now.copy(cityName = cityName)
                
                Result.success(weatherWithCity)
            } else {
                Log.e("WeatherDebug", "天气API错误，返回code=${response.code}")
                Result.failure(Exception("Weather API error: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e("WeatherDebug", "发生异常：${e.message}", e)
            Result.failure(e)
        }
    }

}