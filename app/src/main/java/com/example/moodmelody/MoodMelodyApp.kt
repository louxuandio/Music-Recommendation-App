package com.example.moodmelody

import android.app.Application
import com.example.moodmelody.network.RetrofitClient
import com.example.moodmelody.player.SpotifyPlayerManager
import com.example.moodmelody.repository.SpotifyRepository
import com.example.moodmelody.repository.WeatherRepository
import com.example.moodmelody.viewmodel.MusicViewModel

class MoodMelodyApp : Application() {
    lateinit var spotifyRepository: SpotifyRepository
    lateinit var weatherRepository: WeatherRepository
    lateinit var playerManager: SpotifyPlayerManager
    lateinit var musicViewModel: MusicViewModel

    override fun onCreate() {
        super.onCreate()

        // 初始化播放器管理器
        playerManager = SpotifyPlayerManager(this)

        // 初始化Repository
        spotifyRepository = SpotifyRepository(RetrofitClient.spotifyApiService)
        weatherRepository = WeatherRepository(RetrofitClient.weatherApiService)

        // 初始化ViewModel
        musicViewModel = MusicViewModel(
            spotifyRepository = spotifyRepository,
            weatherRepository = weatherRepository,
            playerManager = playerManager,
            applicationContext = this
        )
    }
}