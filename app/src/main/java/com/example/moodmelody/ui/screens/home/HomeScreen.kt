package com.example.moodmelody.ui.screens.home


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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.moodmelody.navigation.Screen
import com.example.moodmelody.network.RetrofitClient
import com.example.moodmelody.ui.components.MoodChip
import com.example.moodmelody.ui.components.WeatherCard
import com.example.moodmelody.viewmodel.MusicViewModel
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Error
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    
    // AI Recommendation Result
    val aiRecommendation by viewModel.aiRecommendation.collectAsStateWithLifecycle()

    val latestMoodEntry by viewModel.loadedEntry.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    // Check Spotify token status
    val hasSpotifyToken = remember { RetrofitClient.hasToken() }
    
    // Auto-load latest mood entry and recommendations when page loads
    LaunchedEffect(Unit) {
        // Load the mood entry
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val today = dateFormat.format(java.util.Date())
        viewModel.loadEntryByDate(today)
        
        // If no recommendations yet, get based on default mood
        if (recommendations.isEmpty() && hasSpotifyToken) {
            viewModel.getRecommendations(mood = "happy", intensity = 3)
        }
    }

    LaunchedEffect(latestMoodEntry) {
        if (latestMoodEntry != null) {
            // Always recommend based on newest test result
            val entry = latestMoodEntry!!
            val weather = currentWeather?.text ?: "sunny"

            val moodScore = when(entry.result) {
                "happy" -> entry.happy
                "sad" -> entry.sad
                "calm" -> entry.calm
                "excited" -> entry.excited
                else -> 0.5f
            }

            Toast.makeText(context, "Updating recommendations based on your new mood...", Toast.LENGTH_SHORT).show()

            viewModel.getAIRecommendation(
                moodScore = moodScore,
                keywords = entry.keywords,
                lyric = entry.note,
                weather = weather,
                matchMood = true
            )

            viewModel.getRecommendations(mood = entry.result, intensity = 3)
        }
    }

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
                    
                    // Display user's text input if available
                    if (entry.note.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(0.85f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "\"${entry.note}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // label
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
                    
                    // detail button
                    Button(
                        onClick = {
                            // navigate to stats page
                            navController.navigate(Screen.Stats.route)
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("View Mood Details")
                    }
                }
            }
        }

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
                        text = "Music Recommendation",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    

                    
                    Text(
                        text = "Suggested Songs:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

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
        
        // Only show Recommendations section when we don't have AI recommendations
        if (aiRecommendation == null) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Recommendations
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        recommendations.isNotEmpty() -> "Recommendations for You"
                        latestMoodEntry != null -> "Recommendations Based on Today's Mood"
                        else -> "Get Music Recommendations Based on Your Mood"
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

            errorMessage?.let { error ->
                if (error.isNotEmpty() && recommendations.isEmpty()) {
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
                // Display recommendations from ViewModel
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Recommended Playlist",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Text(
                        text = "Total: ${recommendations.size} songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recommendations) { song ->
                            EnhancedSongCard(
                                title = song.title,
                                artist = song.artist ,
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

