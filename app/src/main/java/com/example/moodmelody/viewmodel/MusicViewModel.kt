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
import android.util.Log
import com.example.moodmelody.repository.AIRecommendationRepository
import com.example.moodmelody.model.UserData
import com.example.moodmelody.model.Recommendation

class MusicViewModel(
    private val spotifyRepository: SpotifyRepository,
    private val weatherRepository: WeatherRepository,
    private val playerManager: SpotifyPlayerManager,
    private val applicationContext: Context
) : ViewModel() {

    private val dao = MoodDatabase.getDatabase(applicationContext).moodEntryDao()
    private val _loadedEntry = MutableStateFlow<MoodEntry?>(null)
    val loadedEntry: StateFlow<MoodEntry?> = _loadedEntry

    private val _monthEntries = MutableStateFlow<List<MoodEntry>>(emptyList())
    val monthEntries: StateFlow<List<MoodEntry>> = _monthEntries

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

    // AI推荐状态
    private val _aiRecommendation = MutableStateFlow<Recommendation?>(null)
    val aiRecommendation: StateFlow<Recommendation?> = _aiRecommendation

    init {
        // 连接到Spotify播放器
        playerManager.connect()

        // 初始化时加载天气数据
        loadWeather()
        
        // 加载今日心情记录
        loadTodayMoodEntry()
    }

    /**
     * 加载今日的心情记录
     */
    private fun loadTodayMoodEntry() {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val today = dateFormat.format(java.util.Date())
        loadEntryByDate(today)
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

    // 保存心情记录
    fun saveMoodEntry(entry: MoodEntry) {
        viewModelScope.launch {
            try {
                // 使用dao直接访问数据库
                dao.insert(entry)
                Log.d("MusicViewModel", "成功保存心情记录: ${entry.date}")
            } catch (e: Exception) {
                Log.e("MusicViewModel", "保存心情记录失败: ${e.message}", e)
            }
        }
    }

    fun loadEntryByDate(date: String) {
        viewModelScope.launch {
            _loadedEntry.value = dao.getEntryByDate(date)
        }
    }

    fun loadEntriesForMonth(year: Int, month: Int) {
        viewModelScope.launch {
            // 格式化月份，确保单位数月份前面加0
            val monthStr = String.format("%04d-%02d", year, month + 1)
            val monthPattern = "$monthStr%"
            _monthEntries.value = dao.getEntriesForMonth(monthPattern)
            Log.d("MusicViewModel", "Loaded ${_monthEntries.value.size} entries for month: $monthStr")
        }
    }

    /**
     * 从AI推荐创建歌单
     * @param recommendedSongs AI推荐的歌曲列表 (格式: "歌名 - 艺术家")
     */
    fun createPlaylistFromAIRecommendation(recommendedSongs: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            // 创建结果列表
            val resultSongs = mutableListOf<Song>()
            
            // 处理每一首推荐歌曲
            for (songInfo in recommendedSongs) {
                try {
                    // 对结果进行处理
                    val searchTerm = songInfo.trim()
                    
                    // 搜索歌曲
                    spotifyRepository.searchMusic(searchTerm).fold(
                        onSuccess = { songs ->
                            if (songs.isNotEmpty()) {
                                // 添加第一个匹配结果
                                resultSongs.add(songs[0])
                            }
                        },
                        onFailure = { error ->
                            // 记录错误但继续处理其他歌曲
                            Log.e("MusicViewModel", "搜索歌曲失败: $searchTerm, ${error.message}")
                        }
                    )
                } catch (e: Exception) {
                    Log.e("MusicViewModel", "处理歌曲时出错: $songInfo", e)
                }
            }
            
            // 更新推荐结果
            _recommendations.value = resultSongs
            _isLoading.value = false
            
            // 如果找到了歌曲，尝试播放第一首
            if (resultSongs.isNotEmpty()) {
                playSong(resultSongs[0])
            }
        }
    }

    // 获取AI音乐推荐
    fun getAIRecommendation(
        moodScore: Float,
        keywords: List<String>,
        lyric: String,
        weather: String
    ) {
        viewModelScope.launch {
            try {
                // 设置加载状态
                _isLoading.value = true
                
                // 创建AI推荐仓库
                val aiRepository = AIRecommendationRepository()
                
                // 创建用户数据
                val userData = UserData(
                    moodScore = moodScore,
                    keywords = keywords,
                    lyric = lyric,
                    weather = weather
                )
                
                // 获取推荐
                val recommendation = aiRepository.recommendWithOpenAI(userData)
                
                // 存储推荐结果
                _aiRecommendation.value = recommendation
                
                // 基于推荐创建播放列表
                createPlaylistFromAIRecommendation(recommendation.suggestedSongs)
                
                // 清除错误状态
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e("MusicViewModel", "获取AI推荐失败: ${e.message}", e)
                _errorMessage.value = "获取推荐失败: ${e.message}"
            } finally {
                // 清除加载状态
                _isLoading.value = false
            }
        }
    }
}