package com.example.moodmelody.ui.screens.airecommend

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.moodmelody.model.UserData
import com.example.moodmelody.viewmodel.AIRecommendationViewModel
import com.example.moodmelody.viewmodel.MusicViewModel
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIRecommendScreen(
    navController: NavController,
    aiViewModel: AIRecommendationViewModel,
    musicViewModel: MusicViewModel,
    paddingValues: PaddingValues
) {
    // 收集ViewModel中的状态
    val recommendation by aiViewModel.recommendation.collectAsStateWithLifecycle()
    val isLoading by aiViewModel.isLoading.collectAsStateWithLifecycle()
    val error by aiViewModel.error.collectAsStateWithLifecycle()
    val currentWeather by musicViewModel.currentWeather.collectAsStateWithLifecycle()
    
    // UI状态
    var moodScore by remember { mutableStateOf(50f) }
    var keywordsText by remember { mutableStateOf("") }
    var lyricText by remember { mutableStateOf("") }
    
    // 页面内容
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 标题
        Text(
            text = "AI音乐推荐",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 心情评分滑块
        Text(
            text = "当前心情评分: ${moodScore.toInt()}/100",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Slider(
            value = moodScore,
            onValueChange = { moodScore = it },
            valueRange = 0f..100f,
            steps = 100,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 关键词输入
        OutlinedTextField(
            value = keywordsText,
            onValueChange = { keywordsText = it },
            label = { Text("关键词 (用逗号分隔)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        
        // 歌词输入
        OutlinedTextField(
            value = lyricText,
            onValueChange = { lyricText = it },
            label = { Text("喜欢的歌词") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            minLines = 2
        )
        
        // 当前天气显示
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "当前天气：",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = currentWeather?.let { "${it.text}" } ?: "未知",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        
        // 获取推荐按钮
        Button(
            onClick = {
                val keywords = keywordsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val weatherText = currentWeather?.text ?: "Unknown"
                
                val userData = UserData(
                    moodScore = moodScore,
                    keywords = keywords,
                    lyric = lyricText,
                    weather = weatherText
                )
                
                aiViewModel.getRecommendation(userData)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("获取个性化音乐推荐")
        }
        
        // 错误显示
        error?.let {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "错误: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        // 推荐结果显示
        recommendation?.let { rec ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "AI音乐推荐",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text(
                        text = rec.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = "推荐歌曲：",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // 歌曲列表
                    rec.suggestedSongs.forEach { song ->
                        SongItem(
                            songTitle = song,
                            onSongClick = {
                                // 这里可以调用音乐播放功能
                                // musicViewModel.playSong(song)
                            }
                        )
                    }
                }
            }
        }
        
        // 底部空间
        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
fun SongItem(
    songTitle: String,
    onSongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = songTitle,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            IconButton(onClick = onSongClick) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "播放"
                )
            }
        }
    }
} 