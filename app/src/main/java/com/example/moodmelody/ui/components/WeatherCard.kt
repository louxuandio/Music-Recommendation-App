package com.example.moodmelody.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WeatherCard(
    city: String,
    temperature: String,
    weather: String,
    emoji: String,
    modifier: Modifier = Modifier
) {
    // Select different gradient colors based on weather type
    val (startColor, endColor) = when {
        emoji.contains("☀️") -> Pair(Color(0xFFFFA726), Color(0xFFFFCC80)) // Sunny orange
        emoji.contains("🌧️") || emoji.contains("☔") -> Pair(Color(0xFF5C6BC0), Color(0xFF9FA8DA)) // Rainy blue-purple
        emoji.contains("☁️") -> Pair(Color(0xFF78909C), Color(0xFFB0BEC5)) // Cloudy gray
        emoji.contains("❄️") -> Pair(Color(0xFF4FC3F7), Color(0xFFB3E5FC)) // Snowy light blue
        else -> Pair(Color(0xFF7E57C2), Color(0xFFB39DDB)) // Default purple
    }
    
    val elevation by animateFloatAsState(targetValue = 2f)
    
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation.dp,
                shape = RoundedCornerShape(24.dp)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        startColor.copy(alpha = 0.8f),
                        endColor.copy(alpha = 0.6f)
                    )
                )
            )
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side weather information
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = city,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = temperature,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = "$weather & Cozy",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            
            // Right side weather icon
            Text(
                text = emoji,
                style = MaterialTheme.typography.displayMedium,
                fontSize = 64.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
} 