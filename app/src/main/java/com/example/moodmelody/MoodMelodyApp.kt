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

        // Initialize Player Manager
        playerManager = SpotifyPlayerManager(this)

        // Initialize Repository
        spotifyRepository = SpotifyRepository(RetrofitClient.spotifyApiService)
        weatherRepository = WeatherRepository(RetrofitClient.weatherApiService)

        // Initialize ViewModel
        musicViewModel = MusicViewModel(
            spotifyRepository = spotifyRepository,
            weatherRepository = weatherRepository,
            playerManager = playerManager,
            applicationContext = this
        )
        
        // Try to connect with Spotify Player
        playerManager.connect()
    }
}