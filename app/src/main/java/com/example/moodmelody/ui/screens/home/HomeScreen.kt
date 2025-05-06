package com.example.moodmelody.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.moodmelody.Song
import com.example.moodmelody.navigation.Screen
import com.example.moodmelody.network.RetrofitClient
import com.example.moodmelody.ui.components.MoodChip
import com.example.moodmelody.ui.components.PlaylistCard
import com.example.moodmelody.ui.components.WeatherCard
import com.example.moodmelody.viewmodel.MusicViewModel
import kotlinx.coroutines.launch
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Error
import com.example.moodmelody.model.Recommendation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import android.widget.Toast
import android.util.Log
import com.example.moodmelody.ui.components.EnhancedSongCard

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: MusicViewModel,
    paddingValues: PaddingValues
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Collect ViewModel data
    val currentWeather by viewModel.currentWeather.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    // AI推荐结果
    val aiRecommendation by viewModel.aiRecommendation.collectAsStateWithLifecycle()
    
    // 添加最新的心情测试结果
    val latestMoodEntry by viewModel.loadedEntry.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    // Check Spotify token status
    val hasSpotifyToken = remember { RetrofitClient.hasToken() }
    
    // Auto-load latest mood entry and recommendations when page loads
    LaunchedEffect(Unit) {
        // 加载今天的心情记录
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val today = dateFormat.format(java.util.Date())
        viewModel.loadEntryByDate(today)
        
        // If no recommendations yet, get based on default mood
        if (recommendations.isEmpty() && hasSpotifyToken) {
            viewModel.getRecommendations(mood = "happy", intensity = 3)
        }
    }
    
    // 处理推荐失败的情况
    LaunchedEffect(errorMessage) {
        errorMessage?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            Log.e("HomeScreen", "错误消息: $error")
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // 显示最新的心情测试结果
        latestMoodEntry?.let { entry ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Today's Mood",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 心情大型emoji显示
                    Text(
                        text = when(entry.result) {
                            "happy" -> "😊"
                            "excited" -> "😃"
                            "calm" -> "😌"
                            "sad" -> "😢"
                            "neutral" -> "😐"
                            else -> "🤔"
                        },
                        fontSize = 80.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                    
                    // 显示心情类型
                    Text(
                        text = when(entry.result) {
                            "happy" -> "Happy"
                            "excited" -> "Excited"
                            "calm" -> "Calm"
                            "sad" -> "Sad"
                            "neutral" -> "Neutral"
                            else -> entry.result
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 关键词标签
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        entry.keywords.take(3).forEach { keyword ->
                            SuggestionChip(
                                onClick = { },
                                label = { Text(keyword) },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                    
                    // 查看详情按钮
                    Button(
                        onClick = {
                            // 导航到统计页面
                            navController.navigate(Screen.Stats.route)
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("View Mood Details")
                    }
                }
            }
        }
        
        // AI推荐结果显示区域 - 只在有AI推荐时显示
        aiRecommendation?.let { recommendation ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "AI Music Recommendation",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = recommendation.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text(
                        text = "Suggested Songs:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // 显示推荐歌曲
                    recommendation.suggestedSongs.forEach { songTitle ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = songTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    if (recommendations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "These songs are ready to play",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
        
        // Weather card
        currentWeather?.let { weather ->
            WeatherCard(
                city = weather.cityName ?: "Your City",
                temperature = "${weather.temp}°",
                weather = weather.text,
                emoji = viewModel.getWeatherEmoji(weather.icon),
                modifier = Modifier.fillMaxWidth()
            )
        } ?: run {
            // If weather data not loaded yet, show loading state
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            // Try to load weather data
            LaunchedEffect(Unit) {
                viewModel.loadWeather()
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Spotify login button - show if no token
        if (!hasSpotifyToken) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Connect to Spotify for personalized recommendations",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Use custom Spotify login button
                    SpotifyLoginButton {
                        // Get recommendations after successful login
                        coroutineScope.launch {
                            viewModel.getRecommendations(mood = "happy", intensity = 3)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Quick mood selection
        Text(
            text = "How are you feeling today?",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val moods = listOf(
                "😊" to "happy",
                "😐" to "neutral",
                "😢" to "sad", 
                "😡" to "angry",
                "😴" to "relaxed"
            )
            
            items(moods) { (emoji, mood) ->
                MoodChip(
                    emoji = emoji,
                    onClick = {
                        coroutineScope.launch {
                            viewModel.getRecommendations(
                                mood = mood,
                                intensity = 3
                            )
                        }
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Test button
        Button(
            onClick = { navController.navigate(Screen.Test.route) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test Your Mood and Generate Playlist")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Recommendations
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 根据是否有AI推荐或心情测试结果显示不同标题
            Text(
                text = when {
                    aiRecommendation != null -> "AI Music Recommendations Based on Your Mood"
                    latestMoodEntry != null -> "Recommendations Based on Today's Mood"
                    else -> "Recommendations for You"
                },
                style = MaterialTheme.typography.headlineMedium
            )
            
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }
        
        // 如果有最新的心情测试结果但没有AI推荐，显示生成歌单按钮
        if (latestMoodEntry != null && aiRecommendation == null && !isLoading) {
            // 设置matchMood为true，只提供匹配心情的音乐推荐
            val matchMood = true
            
            Button(
                onClick = {
                    // 根据心情记录生成AI推荐
                    val entry = latestMoodEntry!!
                    val weather = currentWeather?.text ?: "sunny"
                    
                    // 获取主导情绪值作为心情分数
                    val moodScore = when(entry.result) {
                        "happy" -> entry.happy
                        "sad" -> entry.sad
                        "calm" -> entry.calm
                        "excited" -> entry.excited
                        else -> 0.5f
                    }
                    
                    // 显示正在生成的提示
                    Toast.makeText(context, "Generating AI recommendations, please wait...", Toast.LENGTH_SHORT).show()
                    
                    // 获取AI推荐
                    viewModel.getAIRecommendation(
                        moodScore = moodScore,
                        keywords = entry.keywords,
                        lyric = entry.note,
                        weather = weather,
                        matchMood = matchMood
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Generate AI Music Recommendations")
            }
        }
        
        // 显示错误信息（如有）
        errorMessage?.let { error ->
            if (error.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error, 
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (recommendations.isNotEmpty()) {
            // Display recommendations from ViewModel - 使用增强版卡片
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 标题
                Text(
                    text = "Recommended Playlist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                // 歌曲计数
                Text(
                    text = "Total: ${recommendations.size} songs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // 歌曲列表
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(recommendations) { song ->
                        EnhancedSongCard(
                            title = song.title,
                            artist = song.artist ?: "未知艺术家",
                            coverUrl = song.coverUrl,
                            onClick = { viewModel.playSong(song) }
                        )
                    }
                }
            }
        } else if (!isLoading) {
            // Empty state when not loading
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (hasSpotifyToken) "Select a mood or click the generate AI recommendation button to get music recommendations" else "Connect to Spotify for personalized recommendations",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Define SpotifyLoginButton directly in HomeScreen
@Composable
private fun SpotifyLoginButton(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    Button(onClick = {
        val clientId = "7f598bd5b59b4884b4e5db9997a05cc1" 
        val redirectUri = "moodmelody://callback"
        val scopes = "user-read-private%20playlist-read-private"
        val authUrl = "https://accounts.spotify.com/authorize" +
                "?client_id=$clientId" +
                "&response_type=token" +
                "&redirect_uri=$redirectUri" +
                "&scope=$scopes"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
        context.startActivity(intent)
    }) {
        Text("Login with Spotify")
    }
}

private val samplePlaylists = listOf(
    PlaylistData("Sunny Mood", "https://example.com/cover1.jpg"),
    PlaylistData("Rainy Thoughts", "https://example.com/cover2.jpg"),
    PlaylistData("Night Solitude", "https://example.com/cover3.jpg")
)

data class PlaylistData(
    val title: String,
    val coverUrl: String
) 