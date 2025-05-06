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
import com.example.moodmelody.LyricOption

@Composable
fun LyricSelectionStep(
    options: List<LyricOption>,
    selectedLyric: String?,
    onSelectionChanged: (String?) -> Unit
) {
    var showChinese by remember { mutableStateOf(true) }
    var showMore by remember { mutableStateOf(false) }
    val displayLyrics = if (showMore) options else options.take(3)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (showChinese) "选择最触动你的歌词" else "Select the Lyrics That Most Touch You",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Text(
            text = if (showChinese) "这些歌词与你的心情最契合吗？" else "Do These Lyrics Match Your Mood?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 添加语言切换开关
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(text = "English")
            Switch(
                checked = showChinese,
                onCheckedChange = { showChinese = it }
            )
            Text(text = "中文")
        }
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(displayLyrics) { lyricOption ->
                val displayText = if (showChinese) lyricOption.chinese else lyricOption.english
                val selectedText = if (showChinese) lyricOption.chinese else lyricOption.english
                val isSelected = selectedLyric == lyricOption.chinese || selectedLyric == lyricOption.english
                
                LyricCard(
                    lyric = displayText,
                    isSelected = isSelected,
                    onClick = { onSelectionChanged(selectedText) }
                )
            }
            
            if (!showMore && options.size > 3) {
                item {
                    OutlinedButton(
                        onClick = { showMore = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = if (showChinese) "更多歌词" else "More Lyrics"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (showChinese) "显示更多歌词" else "Show More Lyrics")
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