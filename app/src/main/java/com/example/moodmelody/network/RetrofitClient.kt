package com.example.moodmelody.network

import com.example.moodmelody.BuildConfig
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
    private const val WEATHER_BASE_URL = "https://nq3aankvdt.re.qweatherapi.com/"
    private const val WEATHER_API_KEY = "9dcd04aca58344429b8e4d7ec4c0ecd3"
    
    // OpenAI API constants
    private const val OPENAI_BASE_URL = "https://api.openai.com/"
    private val OPENAI_API_KEY = BuildConfig.OPENAI_API_KEY 

    // Spotify token handling
    private var _spotifyToken: String = ""

    // Public getter
    val spotifyToken: String
        get() = _spotifyToken

    // Check if token exists
    fun hasToken(): Boolean = _spotifyToken.isNotEmpty()

    // Spotify auth interceptor
    private val spotifyAuthInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("Authorization", "Bearer $_spotifyToken")
            .method(original.method, original.body)
        chain.proceed(requestBuilder.build())
    }

    // Logging interceptor for debugging
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Spotify OkHttp client with auth
    private val spotifyOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(spotifyAuthInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Weather OkHttp client (no auth interceptor needed)
    private val weatherOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
        
    // OpenAI OkHttp client
    private val openAIOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)  // 更长的超时时间，因为AI生成可能需要更多时间
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Spotify Retrofit instance
    private val spotifyRetrofit = Retrofit.Builder()
        .baseUrl(SPOTIFY_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(spotifyOkHttpClient)
        .build()

    // Weather Retrofit instance
    private val weatherRetrofit = Retrofit.Builder()
        .baseUrl(WEATHER_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(weatherOkHttpClient)
        .build()
        
    // OpenAI Retrofit instance
    private val openAIRetrofit = Retrofit.Builder()
        .baseUrl(OPENAI_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(openAIOkHttpClient)
        .build()

    // Create Spotify API service
    val spotifyApiService: SpotifyApiService by lazy {
        spotifyRetrofit.create(SpotifyApiService::class.java)
    }

    // Create Weather API service
    val weatherApiService: WeatherApiService by lazy {
        weatherRetrofit.create(WeatherApiService::class.java)
    }
    
    // Create OpenAI API service
    val openAIApiService: OpenAIApiService by lazy {
        openAIRetrofit.create(OpenAIApiService::class.java)
    }

    // Weather API key getter
    val weatherApiKey: String
        get() = WEATHER_API_KEY
        
    // OpenAI API key getter
    val openAIApiKey: String
        get() = OPENAI_API_KEY

    // Update Spotify token
    fun updateSpotifyToken(token: String) {
        _spotifyToken = token
    }
}