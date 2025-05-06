package com.example.moodmelody.network

import com.example.moodmelody.BuildConfig
import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // Spotify API constants
    private const val SPOTIFY_BASE_URL = "https://api.spotify.com/v1/"
    private const val CLIENT_ID = "7f598bd5b59b4884b4e5db9997a05cc1"

    // Weather API constants
    private const val WEATHER_BASE_URL = "https://api.qweather.com/"
    private const val WEATHER_API_KEY = "9ac45f069a45482eaac98675206236a2"
    
    // OpenAI API constants
    private const val OPENAI_BASE_URL = "https://api.openai.com/"
    private val OPENAI_API_KEY = BuildConfig.OPENAI_API_KEY 

    init {
        // 输出更详细的API密钥信息用于调试
        val apiKeyInfo = if (OPENAI_API_KEY.length > 10) {
            val prefix = OPENAI_API_KEY.substring(0, 10)
            val suffix = if (OPENAI_API_KEY.length > 15) OPENAI_API_KEY.substring(OPENAI_API_KEY.length - 5) else "..."
            "$prefix...$suffix (长度: ${OPENAI_API_KEY.length})"
        } else {
            "无效密钥 (长度: ${OPENAI_API_KEY.length})"
        }
        Log.d("RetrofitClient", "OpenAI API密钥信息: $apiKeyInfo")
    }

    // Spotify token handling
    private var _spotifyToken: String = ""

    // Public getter
    val spotifyToken: String
        get() = _spotifyToken

    // Check if token exists
    fun hasToken(): Boolean = _spotifyToken.isNotEmpty()

    // Update token
    fun updateSpotifyToken(token: String) {
        _spotifyToken = token
        Log.d("RetrofitClient", "Spotify token updated: ${token.take(10)}...")
    }
    
    // 创建OkHttpClient，带有日志和超时配置
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY  // 改为更详细的日志级别
        })
        .connectTimeout(30, TimeUnit.SECONDS)  // 增加超时时间
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // 创建Spotify API服务
    val spotifyApiService: SpotifyApiService by lazy {
        Log.d("RetrofitClient", "Creating Spotify API service with token: ${spotifyToken.take(10)}...")
        Retrofit.Builder()
            .baseUrl(SPOTIFY_BASE_URL)
            .client(okHttpClient.newBuilder().addInterceptor(Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $spotifyToken")
                    .build()
                Log.d("RetrofitClient", "Spotify API request: ${request.url}")
                chain.proceed(request)
            }).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpotifyApiService::class.java)
    }

    // 创建天气API服务
    val weatherApiService: WeatherApiService by lazy {
        Log.d("RetrofitClient", "Creating Weather API service...")
        val weatherClient = okHttpClient.newBuilder()
            .addInterceptor { chain ->
                // 添加API Key作为查询参数
                val original = chain.request()
                val originalHttpUrl = original.url
                
                val url = originalHttpUrl.newBuilder()
                    .addQueryParameter("key", WEATHER_API_KEY)
                    .build()
                
                val request = original.newBuilder()
                    .url(url)
                    .addHeader("Accept", "application/json")
                    .addHeader("User-Agent", "MoodMelody/1.0")
                    .build()
                
                Log.d("RetrofitClient", "Weather API request: ${request.url}")
                try {
                    val response = chain.proceed(request)
                    Log.d("RetrofitClient", "Weather API response status: ${response.code}")
                    response
                } catch (e: Exception) {
                    Log.e("RetrofitClient", "Weather API request failed: ${e.message}", e)
                    throw e
                }
            }
            .build()
        
        Retrofit.Builder()
            .baseUrl(WEATHER_BASE_URL)
            .client(weatherClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }
    
    // 创建OpenAI API服务
    val openAIApiService: OpenAIApiService by lazy {
        Retrofit.Builder()
            .baseUrl(OPENAI_BASE_URL)
            .client(okHttpClient.newBuilder().addInterceptor(Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $OPENAI_API_KEY")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAIApiService::class.java)
    }
    
    // OpenAI API密钥
    val openAIApiKey: String
        get() = OPENAI_API_KEY
}