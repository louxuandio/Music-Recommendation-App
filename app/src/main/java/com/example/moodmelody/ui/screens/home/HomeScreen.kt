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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: MusicViewModel,
    paddingValues: PaddingValues
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    
    // Collect ViewModel data
    val currentWeather by viewModel.currentWeather.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Check Spotify token status
    val hasSpotifyToken = remember { RetrofitClient.hasToken() }
    
    // Auto-load recommendations when page loads
    LaunchedEffect(Unit) {
        // If no recommendations yet, get based on default mood
        if (recommendations.isEmpty() && hasSpotifyToken) {
            viewModel.getRecommendations(mood = "happy", intensity = 3)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
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
            Text("Take Detailed Mood Test")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Recommendations
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recommended for You",
                style = MaterialTheme.typography.headlineMedium
            )
            
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (recommendations.isNotEmpty()) {
            // Display recommendations from ViewModel
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(recommendations) { song ->
                    PlaylistCard(
                        title = song.title,
                        coverUrl = song.coverUrl ?: "https://example.com/default.jpg",
                        onClick = { viewModel.playSong(song) }
                    )
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
                    text = if (hasSpotifyToken) "Select a mood to get music recommendations" else "Connect to Spotify for recommendations",
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