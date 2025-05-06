package com.example.moodmelody

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.CalendarView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.example.moodmelody.network.RetrofitClient
import com.example.moodmelody.ui.SongPlayer
import com.example.moodmelody.viewmodel.MusicViewModel
import java.io.FileNotFoundException
import java.util.Calendar
import java.util.Locale
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Tag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import com.example.moodmelody.data.MoodEntry
import java.util.Date
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import java.util.*
import androidx.navigation.compose.rememberNavController
import com.example.moodmelody.ui.theme.MoodmelodyTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.moodmelody.navigation.Navigation
import com.example.moodmelody.navigation.Screen
import com.example.moodmelody.ui.components.MiniPlayer
import com.example.moodmelody.network.SpotifyApiService
import com.example.moodmelody.player.SpotifyPlayerManager
import com.example.moodmelody.repository.SpotifyRepository
import com.example.moodmelody.repository.WeatherRepository
import com.example.moodmelody.viewmodel.AIRecommendationViewModel
import com.example.moodmelody.model.UserData
import com.example.moodmelody.model.Recommendation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

/**
 * 数据类Song(若已在其它地方定义，可去掉这里)
 */
data class Song(
    val title: String,
    val artist: String,
    val coverUrl: String? = null,
    val uri: String? = null,
    val previewUrl: String? = null,
    val genre: String? = null
)
data class Emotions(val happy: Float, val sad: Float, val calm: Float, val excited: Float)


// ==================== TestScreen ====================
// Experience and lyric option lists
val keywordOptions = listOf(
    "Dance in the rain" to Emotions(happy=0.60f, sad=0.00f, calm=0.00f, excited=0.79f),
    "Running barefoot on grass" to Emotions(happy=0.72f, sad=0.00f, calm=0.00f, excited=0.55f),
    "Reading by a campfire" to Emotions(happy=0.85f, sad=0.00f, calm=0.72f, excited=0.00f),
    "Listening to rain in bed" to Emotions(happy=0.70f, sad=0.00f, calm=0.80f, excited=0.00f),
    "Sitting on a rainy bus ride" to Emotions(happy=0.00f, sad=0.72f, calm=0.60f, excited=0.00f)
)

val lyricOptions = listOf(
    "I danced alone with thunder, and I liked the silence after." to Emotions(happy=0.00f, sad=0.70f, calm=0.00f, excited=0.50f),
    "There's peace in knowing I don't need to be understood." to Emotions(happy=0.60f, sad=0.00f, calm=0.80f, excited=0.00f),
    "Even the sun feels too loud today." to Emotions(happy=0.00f, sad=0.90f, calm=0.70f, excited=0.00f),
    "I never knew missing someone could taste like coffee." to Emotions(happy=0.00f, sad=0.88f, calm=0.00f, excited=0.42f)
)

fun calculateMoodResult(
    selectedKeywords: List<String>,
    selectedLyric: String?
): Pair<Emotions, String> {
    val picked = keywordOptions
        .filter { selectedKeywords.contains(it.first) }
        .map { it.second } +
            lyricOptions.firstOrNull { it.first == selectedLyric }?.second?.let { listOf(it) }.orEmpty()

    val count = picked.size.coerceAtLeast(1)
    val sum = picked.fold(Emotions(0f, 0f, 0f, 0f)) { acc, e ->
        Emotions(
            happy = acc.happy + e.happy,
            sad = acc.sad + e.sad,
            calm = acc.calm + e.calm,
            excited = acc.excited + e.excited
        )
    }

    val result = when {
        sum.happy >= sum.sad && sum.happy >= sum.calm && sum.happy >= sum.excited -> "happy"
        sum.sad >= sum.happy && sum.sad >= sum.calm && sum.sad >= sum.excited -> "sad"
        sum.excited >= sum.happy && sum.excited >= sum.sad && sum.excited >= sum.calm -> "excited"
        sum.calm >= sum.happy && sum.calm >= sum.sad && sum.calm >= sum.excited -> "relaxed"
        else -> "angry"
    }

    return (Emotions(
        happy = sum.happy / count,
        sad = sum.sad / count,
        calm = sum.calm / count,
        excited = sum.excited / count
    ) to result)
}

@Composable
fun MoodSliderWithGradient(
    moodIndex: Float,
    onMoodChange: (Float) -> Unit
) {
    val gradientColors = listOf(
        Color(0xFFE53935), // Red
        Color(0xFFFB8C00), // Orange
        Color(0xFFFFEB3B), // Yellow
        Color(0xFF8BC34A), // Light Green
        Color(0xFF43A047)  // Green
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // 背景 gradient bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .align(Alignment.Center)
        ) {
            drawRoundRect(
                brush = Brush.horizontalGradient(gradientColors),
                cornerRadius = CornerRadius(100f, 100f)
            )
        }

        // 叠加的透明轨道 slider
        Slider(
            value = moodIndex,
            onValueChange = onMoodChange,
            valueRange = 0f..4f,
            steps = 3,
            modifier = Modifier
                .fillMaxSize()
                .height(8.dp)
                .align(Alignment.Center), // 关键点：对齐中线
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF5E35B1),
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            )
        )
    }
}


@Composable
fun MoodTestScreen(
    paddingValues: PaddingValues,
    currentQuestion: Int,
    onAnswerSelected: (Int) -> Unit,
    viewModel: MusicViewModel
) {
    var currentPage by remember { mutableStateOf(1) }
    var moodIndex by remember { mutableStateOf(2f) }
    var selectedKeywords by remember { mutableStateOf(listOf<String>()) }
    var selectedLyric by remember { mutableStateOf<String?>(null) }
    var textNote by remember { mutableStateOf("") }
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    
    // AI推荐相关状态
    val aiViewModel = remember { AIRecommendationViewModel() }
    val recommendation by aiViewModel.recommendation.collectAsStateWithLifecycle()
    val isAiLoading by aiViewModel.isLoading.collectAsStateWithLifecycle()
    val aiError by aiViewModel.error.collectAsStateWithLifecycle()
    val currentWeather by viewModel.currentWeather.collectAsStateWithLifecycle()
    
    // 是否显示结果页面
    var showResult by remember { mutableStateOf(false) }

    if (isPortrait) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 显示结果页面或测试页面
            if (showResult) {
                // 结果和推荐页面
                TestResultScreen(
                    selectedKeywords = selectedKeywords,
                    selectedLyric = selectedLyric,
                    textNote = textNote,
                    recommendation = recommendation,
                    isAiLoading = isAiLoading,
                    aiError = aiError,
                    onBackClick = { 
                        showResult = false 
                        currentPage = 1
                        selectedKeywords = listOf()
                        selectedLyric = null
                        textNote = ""
                    },
                    viewModel = viewModel
                )
            } else {
                // 测试流程页面
                when (currentPage) {
                    1 -> MoodSliderPage(moodIndex, onMoodChange = { moodIndex = it })
                    2 -> KeywordSelectPage(keywordOptions, selectedKeywords) {
                        selectedKeywords = if (selectedKeywords.contains(it)) selectedKeywords - it else selectedKeywords + it
                    }
                    3 -> CustomInputPage(lyricOptions, selectedLyric) { selectedLyric = it }
                }

                Spacer(modifier = Modifier.weight(1f))

                Column(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = textNote,
                        onValueChange = { textNote = it },
                        label = { Text("Anything you'd like to add?") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(8.dp)
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Button(onClick = { if (currentPage > 1) currentPage-- }, enabled = currentPage > 1) {
                            Text("Previous")
                        }
                        Button(onClick = {
                            if (currentPage < 3) {
                                currentPage++
                            } else {
                                val (avg, result) = calculateMoodResult(selectedKeywords, selectedLyric)
                                val entry = MoodEntry(
                                    date = today,
                                    calm = avg.calm,
                                    excited = avg.excited,
                                    happy = avg.happy,
                                    sad = avg.sad,
                                    result = result,
                                    keywords = selectedKeywords,
                                    activity = selectedLyric,
                                    note = textNote
                                )
                                viewModel.saveMoodEntry(entry)
                                
                                // 测试完成，请求AI推荐
                                val weatherText = currentWeather?.text ?: "Unknown"
                                val userData = UserData(
                                    moodScore = when(result) {
                                        "happy" -> 80f
                                        "excited" -> 90f
                                        "relaxed" -> 60f
                                        "sad" -> 20f
                                        else -> 50f
                                    },
                                    keywords = selectedKeywords,
                                    lyric = selectedLyric ?: "",
                                    weather = weatherText
                                )
                                
                                aiViewModel.getRecommendation(userData)
                                showResult = true
                            }
                        }) {
                            Text(if (currentPage == 3) "View Result" else "Next")
                        }
                    }
                }
            }
        }
    } else {
        // 横屏布局
        if (showResult) {
            // 结果和推荐页面 - 横屏
            TestResultScreen(
                selectedKeywords = selectedKeywords,
                selectedLyric = selectedLyric,
                textNote = textNote,
                recommendation = recommendation,
                isAiLoading = isAiLoading,
                aiError = aiError,
                onBackClick = { 
                    showResult = false 
                    currentPage = 1
                    selectedKeywords = listOf()
                    selectedLyric = null
                    textNote = ""
                },
                viewModel = viewModel
            )
        } else {
            LandscapeTestScreen(
                selectedStep = currentPage,
                onStepSelected = { currentPage = it },
                textNote = textNote,
                onNoteChange = { textNote = it },
                selectedKeywords = selectedKeywords,
                onKeywordToggle = {
                    selectedKeywords = if (selectedKeywords.contains(it)) selectedKeywords - it else selectedKeywords + it
                },
                selectedActivity = selectedLyric,
                onActivitySelected = { selectedLyric = it },
                moodIndex = moodIndex,
                onMoodChange = { moodIndex = it },
                viewModel = viewModel,
                onViewResult = { 
                    val (avg, result) = calculateMoodResult(selectedKeywords, selectedLyric)
                    val entry = MoodEntry(
                        date = today,
                        calm = avg.calm,
                        excited = avg.excited,
                        happy = avg.happy,
                        sad = avg.sad,
                        result = result,
                        keywords = selectedKeywords,
                        activity = selectedLyric,
                        note = textNote
                    )
                    viewModel.saveMoodEntry(entry)
                    
                    // 测试完成，请求AI推荐
                    val weatherText = currentWeather?.text ?: "Unknown"
                    val userData = UserData(
                        moodScore = when(result) {
                            "happy" -> 80f
                            "excited" -> 90f
                            "relaxed" -> 60f
                            "sad" -> 20f
                            else -> 50f
                        },
                        keywords = selectedKeywords,
                        lyric = selectedLyric ?: "",
                        weather = weatherText
                    )
                    
                    aiViewModel.getRecommendation(userData)
                    showResult = true
                }
            )
        }
    }
}

@Composable
fun TestResultScreen(
    selectedKeywords: List<String>,
    selectedLyric: String?,
    textNote: String,
    recommendation: Recommendation?,
    isAiLoading: Boolean,
    aiError: String?,
    onBackClick: () -> Unit,
    viewModel: MusicViewModel 
) {
    // 状态
    var isCreatingPlaylist by remember { mutableStateOf(false) }
    val spotifyRecommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val spotifyIsLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val spotifyError by viewModel.errorMessage.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 标题
        Text(
            text = "你的心情分析",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 心情分析结果
        val (emotions, result) = calculateMoodResult(selectedKeywords, selectedLyric)
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "当前心情: $result",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = getMoodMotivationalText(result),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                if (selectedKeywords.isNotEmpty()) {
                    Text(
                        text = "选择的关键词：${selectedKeywords.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                if (selectedLyric != null) {
                    Text(
                        text = "选择的歌词：\"$selectedLyric\"",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                if (textNote.isNotEmpty()) {
                    Text(
                        text = "你的笔记：$textNote",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }
        
        // AI推荐音乐
        Text(
            text = "为你推荐的音乐",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
        when {
            isAiLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("正在为你生成个性化音乐推荐...")
                    }
                }
            }
            aiError != null -> {
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
                        text = "抱歉，获取推荐时出错: $aiError",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            recommendation != null -> {
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
                            text = recommendation.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Text(
                            text = "推荐歌曲：",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        // 歌曲列表
                        recommendation.suggestedSongs.forEach { song ->
                            SongRecommendation(songTitle = song)
                        }
                        
                        // 创建播放列表按钮
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                isCreatingPlaylist = true
                                viewModel.createPlaylistFromAIRecommendation(recommendation.suggestedSongs)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isCreatingPlaylist && !spotifyIsLoading
                        ) {
                            if (spotifyIsLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("在Spotify上播放推荐歌曲")
                        }
                    }
                }
                
                // 显示Spotify搜索结果
                if (isCreatingPlaylist && spotifyRecommendations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
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
                                text = "已为您创建播放列表：",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            spotifyRecommendations.forEach { song ->
                                SpotifySongItem(
                                    song = song, 
                                    onSongClick = { viewModel.playSong(song) }
                                )
                            }
                        }
                    }
                }
                
                // 显示Spotify错误
                if (spotifyError != null && isCreatingPlaylist) {
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
                            text = "创建播放列表时出错: $spotifyError",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("等待获取音乐推荐...")
                }
            }
        }
        
        // 返回按钮
        Button(
            onClick = onBackClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text("返回")
        }
        
        // 底部空间
        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
fun SongRecommendation(songTitle: String) {
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = songTitle,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun SpotifySongItem(
    song: Song,
    onSongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onSongClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 专辑封面
            AsyncImage(
                model = song.coverUrl ?: "https://place-hold.it/40x40",
                contentDescription = "专辑封面",
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 歌曲信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            // 播放按钮
            IconButton(onClick = onSongClick) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "播放",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun LandscapeTestScreen(
    selectedStep: Int,
    onStepSelected: (Int) -> Unit,
    textNote: String,
    onNoteChange: (String) -> Unit,
    selectedKeywords: List<String>,
    onKeywordToggle: (String) -> Unit,
    selectedActivity: String?,
    onActivitySelected: (String) -> Unit,
    moodIndex: Float,
    onMoodChange: (Float) -> Unit,
    viewModel: MusicViewModel,
    onViewResult: () -> Unit
) {
    val steps = listOf("Mood", "Keyword", "Action")
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    Row(Modifier.fillMaxSize().padding(16.dp)) {

        // 左边步骤列表
        Column(Modifier.width(160.dp)) {
            steps.forEachIndexed { index, label ->
                val selected = index == selectedStep
                val bg = if (selected) Color(0xFFD1C4E9) else Color.Transparent
                val color = if (selected) Color(0xFF311B92) else Color.Black
                Row(
                    Modifier.fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(bg)
                        .clickable { onStepSelected(index) }
                        .padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (index) {
                            0 -> Icons.Default.EmojiEmotions
                            1 -> Icons.Default.Tag
                            else -> Icons.Default.Check
                        },
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(label, color = color)
                }
            }
        }

        Spacer(Modifier.width(16.dp))

        // 中间内容区域
        Box(Modifier.weight(1f)) {
            when (selectedStep) {
                0 -> MoodSliderPage(moodIndex, onMoodChange)
                1 -> KeywordSelectPage(keywordOptions, selectedKeywords, onKeywordToggle)
                2 -> CustomInputPage(lyricOptions, selectedActivity, onActivitySelected)
            }

            if (selectedStep == 2) {
                Button(
                    onClick = onViewResult,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.8f)
                ) {
                    Text("View Result")
                }
            }
        }

        Spacer(Modifier.width(16.dp))

        // 右边文字输入
        Column(Modifier.width(260.dp)) {
            OutlinedTextField(
                value = textNote,
                onValueChange = onNoteChange,
                label = { Text("Anything you'd like to add?") },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                singleLine = false
            )
        }
    }
}


@Composable
fun MoodSliderPage(
    moodIndex: Float,
    onMoodChange: (Float) -> Unit
){
    val emojiList = listOf(
        R.drawable.emoji_fixed_1,
        R.drawable.emoji_fixed_2,
        R.drawable.emoji_fixed_3,
        R.drawable.emoji_fixed_4,
        R.drawable.emoji_fixed_5
    )
    Text(text = "How are you feeling today?",
        style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(top = 16.dp)
    )

    Image(
        painter = painterResource(id = emojiList[moodIndex.toInt()]),
        contentDescription = null,
        modifier = Modifier.size(360.dp)
    )

    Spacer(modifier = Modifier.height(32.dp))

    MoodSliderWithGradient(
        moodIndex = moodIndex,
        onMoodChange = onMoodChange
    )

}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeywordSelectPage(
    options: List<Pair<String, Emotions>>,
    selectedKeywords: List<String>,
    onKeywordToggle: (String) -> Unit
) {
    Text("Pick the experiences that resonate:")
    Spacer(Modifier.height(8.dp))
    options.forEach { (label, _) ->
        val checked = selectedKeywords.contains(label)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    onValueChange = { onKeywordToggle(label) }
                )
                .padding(8.dp)
        ) {
            Checkbox(checked = checked, onCheckedChange = null)
            Spacer(Modifier.width(8.dp))
            Text(label)
        }
    }
}

@Composable
fun CustomInputPage(
    options: List<Pair<String, Emotions>>,
    selectedLyric: String?,
    onLyricSelected: (String) -> Unit
) {
    Text("Which lyric speaks to you most?")
    Spacer(Modifier.height(8.dp))
    options.forEach { (lyric, _) ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = (selectedLyric == lyric),
                    onValueChange = { onLyricSelected(lyric) }
                )
                .padding(8.dp)
        ) {
            RadioButton(
                selected = (selectedLyric == lyric),
                onClick = null
            )
            Spacer(Modifier.width(8.dp))
            Text(lyric)
        }
    }
}



@Composable
fun MoodSelectionGrid(
    onMoodSelected: (String) -> Unit,
    selectedMood: String?
) {
    val moods = listOf("Sad", "Calm", "Neutral", "Happy", "Excited")
    LazyVerticalGrid(columns = GridCells.Fixed(3), content = {
        items(moods) { mood ->
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (mood == selectedMood) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onMoodSelected(mood) }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mood,
                    color = if (mood == selectedMood) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    })
}

// ==================== MainActivity ====================

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    
    // 创建存储库
    private val weatherRepository by lazy {
        WeatherRepository(RetrofitClient.weatherApiService)
    }
    
    private val spotifyRepository by lazy {
        SpotifyRepository(RetrofitClient.spotifyApiService)
    }
    
    private val playerManager by lazy {
        SpotifyPlayerManager(this)
    }
    
    // 创建ViewModel
    private val musicViewModel by lazy {
        MusicViewModel(
            spotifyRepository = spotifyRepository,
            weatherRepository = weatherRepository,
            playerManager = playerManager,
            applicationContext = applicationContext
        )
    }
    
    private val aiRecommendationViewModel by lazy {
        AIRecommendationViewModel()
    }

    // 位置权限请求启动器
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (locationGranted) {
            // 权限获取成功，重新加载天气
            musicViewModel.loadWeather()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 请求位置权限
        requestLocationPermissions()

        // 处理Spotify认证返回的Token(如果有)
        handleIntent(intent)

        setContent {
            MoodmelodyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        musicViewModel = musicViewModel,
                        aiViewModel = aiRecommendationViewModel
                    )
                }
            }
        }
    }

    private fun requestLocationPermissions() {
        // 检查是否已经有位置权限
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 请求位置权限
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val data = intent.data
        
        if (action == Intent.ACTION_VIEW && data != null) {
            val uri = intent.data ?: return
            
            // 检查是否是Spotify回调
            if (uri.toString().startsWith("moodmelody://callback")) {
                val fragment = uri.fragment
                if (fragment != null && fragment.startsWith("access_token=")) {
                    // 提取访问令牌
                    val tokenParts = fragment.split("&")
                    val tokenValue = tokenParts[0].replace("access_token=", "")
                    
                    Log.d(TAG, "从URI获取令牌: $tokenValue")
                    
                    // 保存令牌
                    RetrofitClient.updateSpotifyToken(tokenValue)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    musicViewModel: MusicViewModel,
    aiViewModel: AIRecommendationViewModel
) {
    val navController = rememberNavController()
    val currentSong by musicViewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by musicViewModel.isPlaying.collectAsStateWithLifecycle()
    
    Scaffold(
        bottomBar = { 
            BottomNavBar(navController = navController)
        },
        content = { paddingValues ->
            Navigation(
                navController = navController,
                musicViewModel = musicViewModel,
                aiViewModel = aiViewModel,
                padding = paddingValues
            )
            
            // 如果有正在播放的歌曲，显示迷你播放器
            currentSong?.let { song ->
                MiniPlayer(
                    songTitle = song.title,
                    artistName = song.artist,
                    isPlaying = isPlaying,
                    onPlayPauseClick = {
                        if (isPlaying) {
                            musicViewModel.pausePlayback()
                        } else {
                            musicViewModel.resumePlayback()
                        }
                    },
                    onNextClick = {
                        musicViewModel.skipNext()
                    }
                )
            }
        }
    )
}

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        Screen.Home to R.drawable.ic_home,
        Screen.Search to R.drawable.ic_search,
        Screen.Stats to R.drawable.ic_stats,
        Screen.AIRecommend to R.drawable.ic_recommend
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    NavigationBar {
        items.forEach { (screen, icon) ->
            NavigationBarItem(
                icon = { Icon(painterResource(id = icon), contentDescription = null) },
                label = { Text(getScreenName(screen)) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        // 避免创建多个相同屏幕的实例
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

@Composable
fun getScreenName(screen: Screen): String {
    return when (screen) {
        Screen.Home -> "首页"
        Screen.Search -> "搜索"
        Screen.Stats -> "统计"
        Screen.Test -> "测试"
        Screen.AIRecommend -> "AI推荐"
    }
}

// 与心灵鸡汤相关函数
fun getMoodMotivationalText(mood: String?): String {
    return when (mood?.lowercase()) {
        "sad" -> "It's okay to be sad sometimes, allow yourself to rest."
        "calm" -> "Take a deep breath and enjoy the tranquility."
        "neutral" -> "Sometimes a quiet mind is a peaceful mind."
        "happy" -> "Share your joy with the world around you."
        "excited" -> "Keep up the energy and let the good vibes roll!"
        else -> "Music can help shape your mood—explore and find what's best for you!"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    paddingValues: PaddingValues,
    searchQuery: String,
    isSearching: Boolean,
    searchResults: List<Song>,
    onSearchQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onSongClick: (Song) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            label = { Text("Search for music") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = onSearch) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                }
            }
        )

        if (isSearching) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else if (searchResults.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(searchResults) { song ->
                    SongItem(song = song, onSongClick = onSongClick)
                }
            }
        } else if (searchQuery.isNotEmpty()) {
            Text(
                text = "No results found for '$searchQuery'",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun SongItem(
    song: Song,
    onSongClick: (Song) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onSongClick(song) }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Song cover placeholder
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text("🎵")
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Song info
            Column {
                Text(
                    text = song.title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (song.genre != null) {
                    Text(
                        text = song.genre,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** HomeScreen: 不用模拟推荐, 全部基于ViewModel返回的recommendations, errorMessage, isLoading来展示 */
@Composable
fun HomeScreen(
    paddingValues: PaddingValues,
    currentWeather: String,
    selectedMood: String?,
    moodIntensity: Int,
    showMusicRecommendations: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    recommendations: List<Song>,
    onMoodSelected: (String) -> Unit,
    onIntensityChanged: (Int) -> Unit,
    onGetRecommendations: () -> Unit,
    onBackToMoodSelection: () -> Unit,
    onSongClick: (Song) -> Unit
) {
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    if (isPortrait){
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Weather display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Current Weather",
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = currentWeather)
                }
            }

            if (showMusicRecommendations) {
                // Recommendations Screen
                Text(
                    text = "Recommendations For You",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Based on your ${selectedMood?.lowercase() ?: "current"} mood",
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = getMoodMotivationalText(selectedMood),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(recommendations) { song ->
                            SongItem(
                                song = song,
                                onSongClick = onSongClick
                            )
                        }
                    }
                }

                Button(
                    onClick = onBackToMoodSelection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text("Change Mood")
                }
            } else {
                // Mood Selection Screen
                Text(
                    text = "How are you feeling today?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Mood grid
                MoodSelectionGrid(
                    onMoodSelected = onMoodSelected,
                    selectedMood = selectedMood
                )

                // Mood intensity slider if mood is selected
                if (selectedMood != null) {
                    Text(
                        text = "Mood Intensity:",
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    Slider(
                        value = moodIntensity.toFloat(),
                        onValueChange = { onIntensityChanged(it.toInt()) },
                        valueRange = 1f..5f,
                        steps = 3,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Mild")
                        Text("Moderate")
                        Text("Intense")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onGetRecommendations,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Get Music Recommendations")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { /* Voice input would be implemented here */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Record Voice Note")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { /* Text diary would be implemented here */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Write Mood Journal")
                    }
                }
            }
        }
    }
    else{
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("How are you feeling today?", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                MoodSelectionGrid(
                    onMoodSelected = onMoodSelected,
                    selectedMood = selectedMood
                )
                if (selectedMood != null) {
                    Text("Mood Intensity")
                    Slider(
                        value = moodIntensity.toFloat(),
                        onValueChange = { onIntensityChanged(it.toInt()) },
                        valueRange = 1f..5f,
                        steps = 3,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Button(onClick = onGetRecommendations) {
                        Text("Get Recommendations")
                    }
                }
            }

            Column(
                modifier = Modifier.width(300.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Current Weather", fontWeight = FontWeight.Bold)
                        Text(currentWeather)
                    }
                }

                if (showMusicRecommendations) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Recommendations", fontWeight = FontWeight.Bold)
                    LazyColumn(modifier = Modifier.fillMaxHeight()) {
                        items(recommendations) { song ->
                            SongItem(song = song, onSongClick = onSongClick)
                        }
                    }
                }
            }
        }
    }
}


//从日记 app复制过来的 read/save/delete file
//⚠️这个用的是 internal storage
// Function to save text to internal storage
fun saveToFile(context: Context, filename: String, content: String) {
    // MODE_PRIVATE means the file is only accessible to this app
    context.openFileOutput(filename, Context.MODE_PRIVATE).use { outputStream ->
        outputStream.write(content.toByteArray())
    }
}


@Composable
fun StatsScreen(paddingValues: PaddingValues,viewModel: MusicViewModel) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var selectedDate by remember { mutableStateOf(sdf.format(Date())) }
    val loadedEntry by viewModel.loadedEntry.collectAsStateWithLifecycle()
    var diaryText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        LaunchedEffect(selectedDate) {
            viewModel.loadEntryByDate(selectedDate)
            Log.d("StatsScreen", "Loading entry for date: $selectedDate")
        }

        LaunchedEffect(loadedEntry) {
            diaryText = if (loadedEntry != null) {
                val entry = loadedEntry!!
                """
            Date: ${entry.date}
            Mood
            : ${entry.result}
            Keywords: ${entry.keywords.joinToString(", ")}
            Activity: ${entry.activity ?: "None"}
            Note: ${entry.note}
            """.trimIndent()
            } else {
                "No record found for $selectedDate"
            }
            Log.d("StatsScreen", "Loaded note: $diaryText")

        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    CalendarView(ctx).apply {
                        setOnDateChangeListener { _, year, month, dayOfMonth ->
                            // 月份从 0 开始，这里直接用 Calendar 格式化成 yyyy-MM-dd
                            val cal = Calendar.getInstance().apply {
                                set(year, month, dayOfMonth)
                            }
                            selectedDate = sdf.format(cal.time)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )


            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = diaryText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .border(1.dp, Color.LightGray)
                    .padding(8.dp)
            )


            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your recommended music for $selectedDate is: ...",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )

            //TODO：有过记录的日子，日期加颜色（表示心情）
            //TODO：有记录的日子，显示日记 AND 那天的推荐歌单 AND 那天的天气
        }
    }
}