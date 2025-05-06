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
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.collections.shuffled

class MusicViewModel(
    private val spotifyRepository: SpotifyRepository,
    private val weatherRepository: WeatherRepository,
    val playerManager: SpotifyPlayerManager,
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

    private val _trendingSongs = MutableStateFlow<List<Song>>(emptyList())
    val trendingSongs: StateFlow<List<Song>> = _trendingSongs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // 新增：天气状态流
    private val _currentWeather = MutableStateFlow<WeatherNow?>(null)
    val currentWeather: StateFlow<WeatherNow?> = _currentWeather

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

        viewModelScope.launch {
            // 初始化加载
            getRecommendations(mood = "happy", intensity = 3)
            getTrendingSongs()
        }
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
     * Load weather data
     */
    fun loadWeather() {
        viewModelScope.launch {
            _isLoading.value = true
            
            // 添加详细日志
            Log.d("MusicViewModel", "开始加载天气数据...")
            
            // 使用WeatherRepository获取随机化的模拟天气数据，而不是硬编码的晴天
            weatherRepository.getCurrentWeather(applicationContext).fold(
                onSuccess = { weatherData ->
                    Log.d("MusicViewModel", "天气数据加载成功: $weatherData")
                    _currentWeather.value = weatherData
                    _errorMessage.value = null
                },
                onFailure = { error ->
                    Log.e("MusicViewModel", "天气加载失败", error)
                    // 提供一个默认值以防失败
                    val fallbackWeather = WeatherNow(
                        obsTime = "2023-04-30T14:30:00+08:00",
                        temp = "22",
                        feelsLike = "23",
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
                        cityName = "Your Location"
                    )
                    _currentWeather.value = fallbackWeather
                }
            )
            
            _isLoading.value = false
            
            /* 注释掉原始API调用代码，因为API返回403错误
            // 先测试API连接
            try {
                val testResult = com.example.moodmelody.utils.WeatherAPITester.testDirectAPICall(applicationContext)
                testResult.fold(
                    onSuccess = { message ->
                        Log.d("MusicViewModel", "天气API直接测试成功: $message")
                    },
                    onFailure = { error ->
                        Log.e("MusicViewModel", "天气API直接测试失败", error)
                    }
                )
            } catch (e: Exception) {
                Log.e("MusicViewModel", "天气API测试过程中发生异常", e)
            }
            
            // 然后使用Repository加载天气
            weatherRepository.getCurrentWeather(applicationContext).fold(
                onSuccess = { weatherData ->
                    Log.d("MusicViewModel", "天气数据加载成功: $weatherData")
                    _currentWeather.value = weatherData
                    _errorMessage.value = null
                },
                onFailure = { error ->
                    Log.e("MusicViewModel", "天气加载失败", error)
                    _errorMessage.value = "Failed to get weather: ${error.message}"
                    
                    // 如果加载失败，尝试直接通过Retrofit测试
                    viewModelScope.launch {
                        try {
                            val retrofitTestResult = com.example.moodmelody.utils.WeatherAPITester.testRetrofitAPICall(applicationContext)
                            retrofitTestResult.fold(
                                onSuccess = { weatherData ->
                                    Log.d("MusicViewModel", "Retrofit测试成功: $weatherData")
                                    _currentWeather.value = weatherData.copy(cityName = "Test City")
                                    _errorMessage.value = null
                                },
                                onFailure = { testError ->
                                    Log.e("MusicViewModel", "Retrofit测试失败", testError)
                                }
                            )
                        } catch (e: Exception) {
                            Log.e("MusicViewModel", "Retrofit测试异常", e)
                        }
                    }
                    
                    // 如果30秒后仍然没有天气数据，尝试再次加载
                    viewModelScope.launch {
                        delay(10000)  // 10秒
                        if (_currentWeather.value == null) {
                            Log.d("MusicViewModel", "尝试重新加载天气数据...")
                            loadWeather()
                        }
                    }
                }
            )

            _isLoading.value = false
            */
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
                    if (songs.isNotEmpty()) {
                        _recommendations.value = songs
                        _errorMessage.value = null
                    } else {
                        // 如果没有获取到推荐，使用默认数据
                        _errorMessage.value = "No recommendations found. Showing trending songs instead."
                        // 尝试获取热门歌曲
                        getTrendingSongs()
                    }
                    _isLoading.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = "Failed to get recommendations: ${error.message}"
                    Log.e("MusicViewModel", "Recommendation API error: ${error.message}", error)
                    
                    // 使用默认数据
                    if (_recommendations.value.isEmpty()) {
                        // 提供备用数据
                        _recommendations.value = getDefaultSongs()
                    }
                    
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
        playerManager.pause()
    }

    fun resumePlayback() {
        playerManager.resume()
    }

    fun skipNext() {
        playerManager.skipToNext()
    }

    fun skipPrevious() {
        playerManager.skipToPrevious()
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
     * Get AI music recommendations with enhanced mood matching
     * @param moodScore mood score (0-100)
     * @param keywords list of keywords
     * @param lyric favorite lyrics
     * @param weather current weather
     */
    fun getAIRecommendation(
        moodScore: Float,
        keywords: List<String>,
        lyric: String,
        weather: String,
        matchMood: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                // Set loading state
                _isLoading.value = true
                
                // 清除现有的AI推荐，确保UI显示加载状态
                _aiRecommendation.value = null
                
                // Create AI recommendation repository
                val aiRepository = AIRecommendationRepository()
                
                // 从数据库加载最新的心情测试结果
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                val moodEntry = dao.getEntryByDate(today)
                
                // 为情绪添加随机波动，增加推荐多样性
                val moodVariation = (Random.nextFloat() * 10f - 5f)
                val adjustedMoodScore = (moodScore + moodVariation).coerceIn(0f, 100f)
                
                // 确保AI推荐与当前心情一致
                val dominantMood = moodEntry?.result ?: when {
                    adjustedMoodScore >= 80 -> "happy"
                    adjustedMoodScore >= 60 -> "relaxed"
                    adjustedMoodScore >= 40 -> "neutral"
                    adjustedMoodScore >= 20 -> "melancholic"
                    else -> "sad"
                }
                
                Log.d("MusicViewModel", "获取AI推荐，心情结果: $dominantMood，原始分数: $moodScore，调整后分数: $adjustedMoodScore")
                
                // 随机选择一些关键词，而不是使用全部，增加变化
                val shuffledKeywords = keywords.shuffled()
                val selectedKeywords = if (shuffledKeywords.size > 2) {
                    val randomCount = Random.nextInt(2, shuffledKeywords.size + 1)
                    shuffledKeywords.take(randomCount)
                } else {
                    shuffledKeywords
                }
                
                // Create user data with mood matching preference
                val userData = UserData(
                    moodScore = adjustedMoodScore,
                    keywords = selectedKeywords,
                    lyric = lyric,
                    weather = weather,
                    matchMood = true, // 始终使用匹配心情模式
                    dominantMood = dominantMood // 添加主导心情数据
                )
                
                // Get recommendations
                val recommendation = try {
                    aiRepository.recommendWithOpenAI(userData)
                } catch (e: Exception) {
                    // 显示具体API错误，不再提供模拟数据
                    _errorMessage.value = "AI recommendation failed: ${e.message}"
                    Log.e("MusicViewModel", "AI recommendation API call failed", e)
                    _isLoading.value = false
                    return@launch
                }
                
                // Store recommendation results
                _aiRecommendation.value = recommendation
                
                // 先清空现有的推荐列表，确保UI更新
                _recommendations.value = emptyList()
                
                // Create playlist from recommendations
                createPlaylistFromAIRecommendation(recommendation.suggestedSongs)
                
                // Clear error state
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Failed to get AI recommendations: ${e.message}", e)
                _errorMessage.value = "获取推荐失败: ${e.message}"
            } finally {
                // Clear loading state
                _isLoading.value = false
            }
        }
    }

    /**
     * Create playlist from AI recommendations
     * @param recommendedSongs AI recommended song list (format: "Song Name - Artist")
     */
    fun createPlaylistFromAIRecommendation(recommendedSongs: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            Log.d("MusicViewModel", "开始处理AI推荐歌曲，总数: ${recommendedSongs.size}")
            
            // 显示处理过程
            _errorMessage.value = "正在从Spotify获取歌曲信息..."
            
            // Create result list
            val resultSongs = mutableListOf<Song>()
            val failedSongs = mutableListOf<String>()
            
            // Process each recommended song
            for ((index, songInfo) in recommendedSongs.withIndex()) {
                try {
                    // Process the result
                    val searchTerm = songInfo.trim()
                    Log.d("MusicViewModel", "搜索歌曲($index): $searchTerm")
                    
                    // Search for the song
                    spotifyRepository.searchMusic(searchTerm).fold(
                        onSuccess = { songs ->
                            if (songs.isNotEmpty()) {
                                // Add the first matching result
                                val song = songs[0]
                                resultSongs.add(song)
                                Log.d("MusicViewModel", "找到歌曲: ${song.title} - ${song.artist}, URL: ${song.coverUrl}")
                                
                                // 更新提示信息
                                _errorMessage.value = "已找到 ${resultSongs.size}/${recommendedSongs.size} 首歌曲..."
                            } else {
                                Log.w("MusicViewModel", "没有找到匹配歌曲: $searchTerm")
                                failedSongs.add(searchTerm)
                            }
                        },
                        onFailure = { error ->
                            // Log error but continue processing other songs
                            Log.e("MusicViewModel", "搜索歌曲失败: $searchTerm, ${error.message}")
                            failedSongs.add(searchTerm)
                        }
                    )
                    
                    // 稍微延迟，避免API请求过于频繁
                    kotlinx.coroutines.delay(100)
                    
                } catch (e: Exception) {
                    Log.e("MusicViewModel", "处理歌曲出错: $songInfo", e)
                    failedSongs.add(songInfo)
                }
            }
            
            // Update recommendation results
            if (resultSongs.isNotEmpty()) {
                _recommendations.value = resultSongs
                _errorMessage.value = null  // 清除状态消息
                
                // 如果有歌曲未找到，显示提示
                if (failedSongs.isNotEmpty()) {
                    _errorMessage.value = "已创建歌单，但有${failedSongs.size}首歌曲未找到"
                }
                
                // 播放第一首歌
                playSong(resultSongs[0])
                
                Log.d("MusicViewModel", "成功创建歌单，共${resultSongs.size}首歌曲")
            } else {
                // 没有找到任何歌曲
                _errorMessage.value = "No Songs found"
                Log.e("MusicViewModel", "Can't create playlist")
            }
            
            _isLoading.value = false
        }
    }

    /**
     * 获取最新发行的歌曲
     * 从Spotify新发行榜单获取最新发布的歌曲
     */
    fun getTrendingSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            spotifyRepository.getTopTrendingSongs().fold(
                onSuccess = { songs ->
                    if (songs.isNotEmpty()) {
                        _trendingSongs.value = songs
                        
                        // 如果没有推荐歌曲，也将热门歌曲设置为推荐
                        if (_recommendations.value.isEmpty()) {
                            _recommendations.value = songs
                        }
                        
                        // 成功获取到歌曲，清除错误消息
                        _errorMessage.value = null
                    } else {
                        // 如果没有获取到歌曲，使用默认数据
                        _trendingSongs.value = getDefaultSongs()
                        
                        // 如果没有推荐歌曲，也将默认歌曲设置为推荐
                        if (_recommendations.value.isEmpty()) {
                            _recommendations.value = getDefaultSongs()
                        }
                        
                        // 此时不需要显示错误消息，因为我们有备用数据
                    }
                    
                    _isLoading.value = false
                },
                onFailure = { error ->
                    // 记录错误但不显示给用户，因为我们有备用数据
                    Log.e("MusicViewModel", "Trending songs API error: ${error.message}", error)
                    
                    // 使用默认数据
                    _trendingSongs.value = getDefaultSongs()
                    
                    // 如果没有推荐歌曲，也将默认歌曲设置为推荐
                    if (_recommendations.value.isEmpty()) {
                        _recommendations.value = getDefaultSongs()
                    }
                    
                    // 注意：这里我们让SpotifyRepository已经返回了成功结果，所以这个分支不应该再被执行
                    // 但为了安全起见，我们仍然保留这个处理
                    _errorMessage.value = null
                    _isLoading.value = false
                }
            )
        }
    }

    /**
     * 获取默认歌曲列表，用于API调用失败时的备用数据
     */
    private fun getDefaultSongs(): List<Song> {
        return listOf(
            Song(
                title = "Shape of You",
                artist = "Ed Sheeran",
                coverUrl = "https://i.scdn.co/image/ab67616d0000b27357cc5b9c578aff1c5a01046d",
                uri = "spotify:track:7qiZfU4dY1lWllzX7mPBI3",
                previewUrl = "https://p.scdn.co/mp3-preview/f75088d9b7fc88c3ecf7bb8c3910687587476a61"
            ),
            Song(
                title = "Blinding Lights",
                artist = "The Weeknd",
                coverUrl = "https://i.scdn.co/image/ab67616d0000b2738863bc11d2aa12b54f5aeb36",
                uri = "spotify:track:0VjIjW4GlUZAMYd2vXMi3b",
                previewUrl = "https://p.scdn.co/mp3-preview/8d3df1c64907cb183bff5a127b1525b530992afb"
            ),
            Song(
                title = "Dynamite",
                artist = "BTS",
                coverUrl = "https://i.scdn.co/image/ab67616d0000b2735f5135e3f4142e1c3a241fbd",
                uri = "spotify:track:0t1kP63rueHleOhQkYSXFY",
                previewUrl = "https://p.scdn.co/mp3-preview/52242a25d6ae6bd7ce7a3fd4e738e67d1c3437ea"
            ),
            Song(
                title = "Dance Monkey",
                artist = "Tones and I",
                coverUrl = "https://i.scdn.co/image/ab67616d0000b273428a256d6f3e3470784161fe",
                uri = "spotify:track:2XU0oxnq2qxCpomAAuJY8K",
                previewUrl = "https://p.scdn.co/mp3-preview/e9eb631c7e6a4df37f605e8eae2dbad9247bb6b8"
            ),
            Song(
                title = "Someone You Loved",
                artist = "Lewis Capaldi",
                coverUrl = "https://i.scdn.co/image/ab67616d0000b273fc2101e6889d6ce9025f85f2",
                uri = "spotify:track:7qEHsqek33rTcFNT9PFqLf",
                previewUrl = "https://p.scdn.co/mp3-preview/dd138c295432b09603ec1d60f31ddeeee47cff2a"
            )
        )
    }
}