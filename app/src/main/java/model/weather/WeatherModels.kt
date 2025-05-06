package com.example.moodmelody.model

// 和风天气API返回的数据模型
data class WeatherResponse(
    val code: String,
    val updateTime: String,
    val fxLink: String,
    val now: NowWeather,
    val refer: Refer
)

data class NowWeather(
    val obsTime: String,
    val temp: String,        // 温度，摄氏度
    val feelsLike: String,   // 体感温度
    val icon: String,        // 天气状况图标代码
    val text: String,        // 天气状况文字描述
    val wind360: String,     // 风向角度
    val windDir: String,     // 风向
    val windScale: String,   // 风力等级
    val windSpeed: String,   // 风速，公里/小时
    val humidity: String,    // 相对湿度，百分比
    val precip: String,      // 降水量
    val pressure: String,    // 大气压强
    val vis: String,         // 能见度，公里
    val cloud: String,       // 云量，百分比
    val dew: String          // 露点温度
)

data class Refer(
    val sources: List<String>,
    val license: List<String>
)

// 简化的天气展示模型
data class WeatherInfo(
    val temperature: String,
    val weatherDescription: String,
    val icon: String,
    val humidity: String,
    val windSpeed: String,
    val windDirection: String
) {
    fun toDisplayString(): String {
        return "$weatherDescription, $temperature°C"
    }
}