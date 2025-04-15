package com.example.moodmelody.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodmelody.Song
import com.example.moodmelody.network.WeatherNow
import com.example.moodmelody.player.SpotifyPlayerManager
import com.example.moodmelody.repository.SpotifyRepository
import com.example.moodmelody.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.moodmelody.data.MoodDatabase
import com.example.moodmelody.data.MoodEntry
import android.app.Application

class MusicViewModel(
    private val spotifyRepository: SpotifyRepository,
    private val weatherRepository: WeatherRepository,
    private val playerManager: SpotifyPlayerManager,
    private val applicationContext: Context
) : ViewModel() {

    private val dao = MoodDatabase.getDatabase(applicationContext).moodEntryDao()
    private val _loadedEntry = MutableStateFlow<MoodEntry?>(null)
    val loadedEntry: StateFlow<MoodEntry?> = _loadedEntry

    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults

    private val _recommendations = MutableStateFlow<List<Song>>(emptyList())
    val recommendations: StateFlow<List<Song>> = _recommendations

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // 新增：天气状态流
    private val _currentWeather = MutableStateFlow<WeatherNow?>(null)
    val currentWeather: StateFlow<WeatherNow?> = _currentWeather

    // 播放器状态
    val isPlayerConnected = playerManager.isConnected
    val currentSong = playerManager.currentSong
    val isPlaying = playerManager.isPlaying

    init {
        // 连接到Spotify播放器
        playerManager.connect()

        // 初始化时加载天气数据
        loadWeather()
    }

    /**
     * 加载天气数据
     */
    fun loadWeather() {
        viewModelScope.launch {
            _isLoading.value = true

            weatherRepository.getCurrentWeather(applicationContext).fold(
                onSuccess = { weatherData ->
                    _currentWeather.value = weatherData
                    _errorMessage.value = null
                },
                onFailure = { error ->
                    _errorMessage.value = "获取天气失败: ${error.message}"
                }
            )

            _isLoading.value = false
        }
    }

    fun searchMusic(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            spotifyRepository.searchMusic(query).fold(
                onSuccess = { songs ->
                    _searchResults.value = songs
                    _isLoading.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = "搜索失败: ${error.message}"
                    _isLoading.value = false
                }
            )
        }
    }

    /**
     * 获取音乐推荐
     * @param mood 心情
     * @param intensity 强度
     * @param weather 天气数据（可选，默认使用当前获取的天气）
     */
    fun getRecommendations(
        mood: String,
        intensity: Int,
        weather: WeatherNow? = _currentWeather.value
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // 这里可以基于天气和心情的组合来调整推荐逻辑
            // 例如：如果天气是雨天，且心情是平静/伤心，推荐一些舒缓的音乐
            // 暂时使用原有逻辑，未来可扩展

            spotifyRepository.getRecommendations(mood, intensity).fold(
                onSuccess = { songs ->
                    _recommendations.value = songs
                    _isLoading.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = "获取推荐失败: ${error.message}"
                    _isLoading.value = false
                }
            )
        }
    }

    // 播放控制方法
    fun playSong(song: Song) {
        playerManager.playSong(song)
    }

    fun pausePlayback() {
        playerManager.pausePlayback()
    }

    fun resumePlayback() {
        playerManager.resumePlayback()
    }

    fun skipNext() {
        playerManager.skipNext()
    }

    fun skipPrevious() {
        playerManager.skipPrevious()
    }

    /**
     * 获取天气图标对应的表情
     */
    fun getWeatherEmoji(iconCode: String): String {
        return when {
            iconCode.startsWith("1") -> "☀️" // 晴天
            iconCode.startsWith("3") -> "🌥️" // 多云
            iconCode.startsWith("4") -> "☁️" // 阴天
            iconCode.startsWith("5") -> "🌧️" // 雨天
            iconCode.startsWith("6") -> "❄️" // 雪
            iconCode.startsWith("7") -> "🌫️" // 雾霾
            iconCode.startsWith("8") -> "🌪️" // 风暴
            else -> "🌈" // 其他
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.disconnect()
    }


    fun saveMoodEntry(entry: MoodEntry) {
        viewModelScope.launch {
            dao.insert(entry)
        }
    }
    fun loadEntryByDate(date: String) {
        viewModelScope.launch {
            _loadedEntry.value = dao.getEntryByDate(date)
        }
    }
}