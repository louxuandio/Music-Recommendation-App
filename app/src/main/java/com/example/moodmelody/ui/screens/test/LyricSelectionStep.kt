package com.example.moodmelody.ui.screens.test

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LyricSelectionStep(
    selectedLyric: String?,
    onLyricSelected: (String) -> Unit
) {
    val lyricOptions = listOf(
        "我跳舞时如同雷鸣，而我最喜欢的是之后的寂静",
        "我很平静，因为我知道没有人能真正理解我",
        "今天的阳光感觉太刺眼",
        "思念一个人的味道，像咖啡般苦涩却回味无穷",
        "我们都是不畏惧这世界的孩子",
        "当时间停止，感受心跳的共鸣",
        "从来不曾害怕孤独，因为音乐是永恒的陪伴"
    )
    
    var showMore by remember { mutableStateOf(false) }
    val displayLyrics = if (showMore) lyricOptions else lyricOptions.take(3)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "选择最触动你的歌词",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Text(
            text = "这些歌词与你的心情最契合吗？",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(displayLyrics) { lyric ->
                LyricCard(
                    lyric = lyric,
                    isSelected = lyric == selectedLyric,
                    onClick = { onLyricSelected(lyric) }
                )
            }
            
            if (!showMore && lyricOptions.size > 3) {
                item {
                    OutlinedButton(
                        onClick = { showMore = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多歌词"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("显示更多歌词")
                    }
                }
            }
        }
    }
}

@Composable
fun LyricCard(
    lyric: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 4.dp else 1.dp
    )
    
    val gradientBrush = Brush.verticalGradient(
        colors = if (isSelected) {
            listOf(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            )
        } else {
            listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surface
            )
        }
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(24.dp)
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .padding(20.dp)
        ) {
            Text(
                text = "\"$lyric\"",
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
} 