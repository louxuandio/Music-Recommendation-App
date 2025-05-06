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
import android.util.Log
import com.example.moodmelody.repository.AIRecommendationRepository
import com.example.moodmelody.model.UserData
import com.example.moodmelody.model.Recommendation
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

    private val _currentWeather = MutableStateFlow<WeatherNow?>(null)
    val currentWeather: StateFlow<WeatherNow?> = _currentWeather

    private val _aiRecommendation = MutableStateFlow<Recommendation?>(null)
    val aiRecommendation: StateFlow<Recommendation?> = _aiRecommendation

    init {
        // connect to Spotify Player
        playerManager.connect()

        // Load weather while initialize
        loadWeather()
        
        // Load the record of the mood
        loadTodayMoodEntry()

        viewModelScope.launch {
            // Initialize
            getRecommendations(mood = "happy", intensity = 3)
            getTrendingSongs()
        }
    }

    /**
     * Initialize the mood for the day
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
            
            // Load detailed Note
            Log.d("MusicViewModel", "Start loading Weather data...")

            weatherRepository.getCurrentWeather(applicationContext).fold(
                onSuccess = { weatherData ->
                    Log.d("MusicViewModel", "Weather information loaded: $weatherData")
                    _currentWeather.value = weatherData
                    _errorMessage.value = null
                },
                onFailure = { error ->
                    Log.e("MusicViewModel", "Weather load failure", error)
                    // In Case it fails
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
     * Get Music Recommendation
     * @param mood
     * @param intensity
     * @param weather
     */
    fun getRecommendations(
        mood: String,
        intensity: Int,
        weather: WeatherNow? = _currentWeather.value
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            spotifyRepository.getRecommendations(mood, intensity).fold(
                onSuccess = { songs ->
                    if (songs.isNotEmpty()) {
                        _recommendations.value = songs
                        _errorMessage.value = null
                    } else {
                        _errorMessage.value = "No recommendations found. Showing trending songs instead."
                        getTrendingSongs()
                    }
                    _isLoading.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = "Failed to get recommendations: ${error.message}"
                    Log.e("MusicViewModel", "Recommendation API error: ${error.message}", error)

                    if (_recommendations.value.isEmpty()) {
                        _recommendations.value = getDefaultSongs()
                    }
                    
                    _isLoading.value = false
                }
            )
        }
    }


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
     * Get emoji for weather icon code
     */
    fun getWeatherEmoji(iconCode: String): String {
        return when {
            iconCode.startsWith("1") -> "☀️" // Sunny
            iconCode.startsWith("3") -> "🌥️" // Partly cloudy
            iconCode.startsWith("4") -> "☁️" // Cloudy
            iconCode.startsWith("5") -> "🌧️" // Rainy
            iconCode.startsWith("6") -> "❄️" // Snow
            iconCode.startsWith("7") -> "🌫️" // Fog/haze
            iconCode.startsWith("8") -> "🌪️" // Storm
            else -> "🌈" // Other
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.disconnect()
    }

    // Record the Mood
    fun saveMoodEntry(entry: MoodEntry) {
        viewModelScope.launch {
            try {
                // Access Database
                dao.insert(entry)
                Log.d("MusicViewModel", "Record the mood successfully: ${entry.date}")
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Fail to record mood: ${e.message}", e)
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
                
                // Always update UI
                _aiRecommendation.value = null
                
                // Create AI recommendation repository
                val aiRepository = AIRecommendationRepository()
                
                // Load newest test result
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                val moodEntry = dao.getEntryByDate(today)
                
                // be creative
                val moodVariation = (Random.nextFloat() * 10f - 5f)
                val adjustedMoodScore = (moodScore + moodVariation).coerceIn(0f, 100f)


                val dominantMood = moodEntry?.result ?: when {
                    adjustedMoodScore >= 80 -> "happy"
                    adjustedMoodScore >= 60 -> "relaxed"
                    adjustedMoodScore >= 40 -> "neutral"
                    adjustedMoodScore >= 20 -> "melancholic"
                    else -> "sad"
                }
                
                Log.d("MusicViewModel", "Get AI recommendation, Test result: $dominantMood, origin score: $moodScore, weighted score: $adjustedMoodScore")
                
                // Random choose
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
                    matchMood = true,
                    dominantMood = dominantMood
                )
                
                // Get recommendations
                val recommendation = try {
                    aiRepository.recommendWithOpenAI(userData)
                } catch (e: Exception) {
                    // Display detailed API error message
                    _errorMessage.value = "AI recommendation failed: ${e.message}"
                    Log.e("MusicViewModel", "AI recommendation API call failed", e)
                    _isLoading.value = false
                    return@launch
                }
                
                // Store recommendation results
                _aiRecommendation.value = recommendation
                
                // Always update UI
                _recommendations.value = emptyList()
                
                // Create playlist from recommendations
                createPlaylistFromAIRecommendation(recommendation.suggestedSongs)
                
                // Clear error state
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Failed to get AI recommendations: ${e.message}", e)
                _errorMessage.value = "Can't get recommendation: ${e.message}"
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
            
            Log.d("MusicViewModel", "Start loading AI recommendations, total number: ${recommendedSongs.size}")


            _errorMessage.value = "Loading song information from Spotify..."
            
            // Create result list
            val resultSongs = mutableListOf<Song>()
            val failedSongs = mutableListOf<String>()
            
            // Process each recommended song
            for ((index, songInfo) in recommendedSongs.withIndex()) {
                try {
                    // Process the result
                    val searchTerm = songInfo.trim()
                    Log.d("MusicViewModel", "Search($index): $searchTerm")
                    
                    // Search for the song
                    spotifyRepository.searchMusic(searchTerm).fold(
                        onSuccess = { songs ->
                            if (songs.isNotEmpty()) {
                                // Add the first matching result
                                val song = songs[0]
                                resultSongs.add(song)
                                Log.d("MusicViewModel", "Found: ${song.title} - ${song.artist}, URL: ${song.coverUrl}")
                                
                                // Update msg
                                _errorMessage.value = "Found ${resultSongs.size}/${recommendedSongs.size} songs..."
                            } else {
                                Log.w("MusicViewModel", "No match songs: $searchTerm")
                                failedSongs.add(searchTerm)
                            }
                        },
                        onFailure = { error ->
                            // Log error but continue processing other songs
                            Log.e("MusicViewModel", "Can't find song: $searchTerm, ${error.message}")
                            failedSongs.add(searchTerm)
                        }
                    )

                    kotlinx.coroutines.delay(100)
                    
                } catch (e: Exception) {
                    Log.e("MusicViewModel", "处理歌曲出错: $songInfo", e)
                    failedSongs.add(songInfo)
                }
            }
            
            // Update recommendation results
            if (resultSongs.isNotEmpty()) {
                _recommendations.value = resultSongs
                _errorMessage.value = null
                
                // If can't find songs
                if (failedSongs.isNotEmpty()) {
                    _errorMessage.value = "Playlist created, But ${failedSongs.size} songs missing"
                }
                
                // Play the first song
                playSong(resultSongs[0])
                
                Log.d("MusicViewModel", "Playlist created, ${resultSongs.size} songs in total")
            } else {
                _errorMessage.value = "No Songs found"
                Log.e("MusicViewModel", "Can't create playlist")
            }
            
            _isLoading.value = false
        }
    }

    /**
     * Get the new posted songs from Spotify
     */
    fun getTrendingSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            spotifyRepository.getTopTrendingSongs().fold(
                onSuccess = { songs ->
                    if (songs.isNotEmpty()) {
                        _trendingSongs.value = songs

                        if (_recommendations.value.isEmpty()) {
                            _recommendations.value = songs
                        }

                        _errorMessage.value = null
                    } else {
                        _trendingSongs.value = getDefaultSongs()

                        if (_recommendations.value.isEmpty()) {
                            _recommendations.value = getDefaultSongs()
                        }
                    }
                    
                    _isLoading.value = false
                },
                onFailure = { error ->
                    Log.e("MusicViewModel", "Trending songs API error: ${error.message}", error)

                    _trendingSongs.value = getDefaultSongs()

                    if (_recommendations.value.isEmpty()) {
                        _recommendations.value = getDefaultSongs()
                    }

                    _errorMessage.value = null
                    _isLoading.value = false
                }
            )
        }
    }

    /**
     * Default Song list (hardcoded)
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