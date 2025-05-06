package com.example.moodmelody.ui.screens.test

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
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
import com.example.moodmelody.MoodTestScreen
import com.example.moodmelody.navigation.Screen
import com.example.moodmelody.viewmodel.MusicViewModel
import com.example.moodmelody.repository.SpotifyRepository
import com.example.moodmelody.repository.WeatherRepository
import com.example.moodmelody.player.SpotifyPlayerManager
import com.example.moodmelody.network.RetrofitClient
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.icu.text.SimpleDateFormat
import com.example.moodmelody.data.MoodEntry
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontStyle

// 定义情绪数据类
data class Emotions(val happy: Float, val sad: Float, val calm: Float, val excited: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(navController: NavController) {
    // 使用LocalContext获取上下文
    val context = LocalContext.current
    
    // 创建各种依赖
    val spotifyRepository = SpotifyRepository(RetrofitClient.spotifyApiService)
    val weatherRepository = WeatherRepository(RetrofitClient.weatherApiService)
    val playerManager = remember { SpotifyPlayerManager(context) }
    
    // 创建ViewModel实例
    val viewModel = remember { 
        MusicViewModel(
            spotifyRepository = spotifyRepository,
            weatherRepository = weatherRepository,
            playerManager = playerManager,
            applicationContext = context
        )
    }
    
    // 使用自定义测试屏幕替代原来的MoodTestScreen
    CustomTestScreen(
        viewModel = viewModel,
        navController = navController
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTestScreen(
    viewModel: MusicViewModel,
    navController: NavController
) {
    // 状态变量
    var currentPage by remember { mutableStateOf(1) }
    var moodValue by remember { mutableStateOf(0.5f) }
    var selectedKeywords by remember { mutableStateOf(listOf<String>()) }
    var selectedLyric by remember { mutableStateOf<String?>(null) }
    var textNote by remember { mutableStateOf("") }
    
    // 天气数据
    val currentWeather by viewModel.currentWeather.collectAsStateWithLifecycle()
    
    // 测试数据
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    
    // 测试中使用的关键词和歌词选项
    val keywordOptions = listOf(
        "雨中漫步" to Emotions(happy=0.30f, sad=0.50f, calm=0.70f, excited=0.20f),
        "窗前读书" to Emotions(happy=0.50f, sad=0.10f, calm=0.90f, excited=0.10f),
        "草地奔跑" to Emotions(happy=0.80f, sad=0.00f, calm=0.20f, excited=0.90f),
        "海边漫步" to Emotions(happy=0.70f, sad=0.05f, calm=0.85f, excited=0.40f),
        "咖啡时光" to Emotions(happy=0.60f, sad=0.10f, calm=0.75f, excited=0.20f),
        "音乐陪伴" to Emotions(happy=0.65f, sad=0.20f, calm=0.60f, excited=0.50f),
        "与朋友聊天" to Emotions(happy=0.85f, sad=0.05f, calm=0.30f, excited=0.70f),
        "独处沉思" to Emotions(happy=0.30f, sad=0.40f, calm=0.80f, excited=0.10f),
        "森林徒步" to Emotions(happy=0.60f, sad=0.05f, calm=0.70f, excited=0.50f),
        "城市夜景" to Emotions(happy=0.50f, sad=0.30f, calm=0.40f, excited=0.60f)
    )

    val lyricOptions = listOf(
        "我跳舞时如同雷鸣，而我最喜欢的是之后的寂静" to Emotions(happy=0.20f, sad=0.40f, calm=0.60f, excited=0.50f),
        "我很平静，因为我知道没有人能真正理解我" to Emotions(happy=0.10f, sad=0.70f, calm=0.80f, excited=0.00f),
        "今天的阳光感觉太刺眼" to Emotions(happy=0.10f, sad=0.60f, calm=0.30f, excited=0.20f),
        "思念一个人的味道，像咖啡般苦涩却回味无穷" to Emotions(happy=0.20f, sad=0.80f, calm=0.40f, excited=0.10f)
    )
    
    // 功能函数
    fun calculateMoodResultDetailed(
        moodValue: Float,
        selectedKeywords: List<String>,
        selectedLyric: String?
    ): Emotions {
        // 从滑块获取基础情绪值
        val baseEmotions = when {
            moodValue < 0.2f -> Emotions(happy=0.10f, sad=0.90f, calm=0.40f, excited=0.05f)
            moodValue < 0.4f -> Emotions(happy=0.30f, sad=0.60f, calm=0.60f, excited=0.20f)
            moodValue < 0.6f -> Emotions(happy=0.50f, sad=0.30f, calm=0.50f, excited=0.40f)
            moodValue < 0.8f -> Emotions(happy=0.70f, sad=0.10f, calm=0.40f, excited=0.60f)
            else -> Emotions(happy=0.90f, sad=0.05f, calm=0.20f, excited=0.80f)
        }
        
        // 收集所有情绪值
        val allEmotions = mutableListOf(baseEmotions)
        
        // 添加关键词对应的情绪值
        allEmotions.addAll(
            keywordOptions
                .filter { selectedKeywords.contains(it.first) }
                .map { it.second }
        )
        
        // 添加歌词对应的情绪值
        selectedLyric?.let { lyric ->
            lyricOptions.find { it.first == lyric }?.let {
                allEmotions.add(it.second)
            }
        }
        
        // 计算平均情绪值
        val count = allEmotions.size
        val sum = allEmotions.reduce { acc, emotions ->
            Emotions(
                happy = acc.happy + emotions.happy,
                sad = acc.sad + emotions.sad,
                calm = acc.calm + emotions.calm,
                excited = acc.excited + emotions.excited
            )
        }
        
        return Emotions(
            happy = sum.happy / count,
            sad = sum.sad / count,
            calm = sum.calm / count,
            excited = sum.excited / count
        )
    }
    
    // 获取主导情绪
    fun getDominantMood(emotions: Emotions): String {
        val moodMap = mapOf(
            "happy" to emotions.happy,
            "sad" to emotions.sad, 
            "relaxed" to emotions.calm,
            "excited" to emotions.excited
        )
        
        return moodMap.maxByOrNull { it.value }?.key ?: "neutral"
    }
    
    // 获取情绪得分 (0-100)
    fun getMoodScore(emotions: Emotions): Float {
        // 正面情绪(happy, excited)增加分数，负面情绪(sad)减少分数
        // calm情绪轻微增加分数
        return ((emotions.happy * 30f) + 
                (emotions.excited * 25f) + 
                (emotions.calm * 15f) - 
                (emotions.sad * 30f) + 50f)
            .coerceIn(0f, 100f)
    }
    
    // 完成测试并保存结果
    fun completeTest() {
        val emotions = calculateMoodResultDetailed(
            moodValue,
            selectedKeywords,
            selectedLyric
        )
        val dominantMood = getDominantMood(emotions)
        val moodScore = getMoodScore(emotions)
        
        // 创建并保存MoodEntry
        val entry = MoodEntry(
            date = today,
            calm = emotions.calm,
            excited = emotions.excited,
            happy = emotions.happy,
            sad = emotions.sad,
            result = dominantMood,
            keywords = selectedKeywords,
            activity = selectedLyric,
            note = textNote
        )
        
        // 保存到数据库
        viewModel.saveMoodEntry(entry)
        
        // 请求音乐推荐
        val weatherText = currentWeather?.text ?: "Unknown"
        
        // 使用AI推荐获取音乐建议
        viewModel.getAIRecommendation(
            moodScore = moodScore,
            keywords = selectedKeywords,
            lyric = selectedLyric ?: "",
            weather = weatherText
        )
        
        // 跳转到主页显示推荐结果
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Home.route) { inclusive = true }
        }
    }
    
    // UI构建
    Scaffold(
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { 
                            if (currentPage > 1) {
                                currentPage--
                            }
                        },
                        enabled = currentPage > 1,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            "上一步",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    Button(
                        onClick = {
                            if (currentPage < 3) {
                                currentPage++
                            } else {
                                // 完成测试
                                if (selectedLyric != null) {
                                    completeTest()
                                }
                            }
                        },
                        enabled = when (currentPage) {
                            2 -> selectedKeywords.isNotEmpty()
                            3 -> selectedLyric != null
                            else -> true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(start = 8.dp)
                    ) {
                        Text(
                            if (currentPage == 3) "查看结果" else "下一步",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 页面指示器
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "步骤 $currentPage / 3",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // 进度指示器
            StepProgressIndicator(
                steps = 3,
                currentStep = currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            
            // 测试内容
            when (currentPage) {
                1 -> {
                    // 第一步：心情滑块
                    MoodSliderStep(
                        moodValue = moodValue,
                        onMoodValueChange = { moodValue = it }
                    )
                }
                2 -> {
                    // 第二步：选择关键词
                    KeywordSelectionStep(
                        options = keywordOptions.map { it.first },
                        selectedKeywords = selectedKeywords,
                        onSelectionChanged = { selectedKeywords = it }
                    )
                }
                3 -> {
                    // 第三步：选择歌词
                    LyricSelectionStep(
                        options = lyricOptions.map { it.first },
                        selectedLyric = selectedLyric,
                        onSelectionChanged = { selectedLyric = it },
                        textNote = textNote,
                        onNoteChanged = { textNote = it }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
    
    // 浮动按钮
    Box(modifier = Modifier.fillMaxSize()) {
        if (currentPage == 1) {
            FloatingActionButton(
                onClick = { currentPage = 2 },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 80.dp, end = 16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "下一步",
                    tint = Color.White
                )
            }
        } else if (currentPage == 2) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 80.dp, end = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = { currentPage = 1 },
                    modifier = Modifier.padding(end = 8.dp),
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "上一步",
                        tint = Color.White
                    )
                }
                
                FloatingActionButton(
                    onClick = { 
                        if (selectedKeywords.isNotEmpty()) {
                            currentPage = 3
                        }
                    },
                    containerColor = if (selectedKeywords.isNotEmpty()) 
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "下一步",
                        tint = Color.White
                    )
                }
            }
        } else if (currentPage == 3) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 80.dp, end = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = { currentPage = 2 },
                    modifier = Modifier.padding(end = 8.dp),
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "上一步",
                        tint = Color.White
                    )
                }
                
                FloatingActionButton(
                    onClick = { 
                        if (selectedLyric != null) {
                            completeTest()
                        }
                    },
                    containerColor = if (selectedLyric != null)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "完成",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun KeywordSelectionStep(
    options: List<String>,
    selectedKeywords: List<String>,
    onSelectionChanged: (List<String>) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "选择你最近的共鸣体验",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Text(
            text = "点击卡片选择，可多选",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // 关键词选项列表
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            options.forEachIndexed { index, keyword ->
                val isSelected = selectedKeywords.contains(keyword)
                val emoji = when (index % 10) {
                    0 -> "🌧️"
                    1 -> "📚"
                    2 -> "🏃"
                    3 -> "🏖️"
                    4 -> "☕"
                    5 -> "🎵"
                    6 -> "👫"
                    7 -> "🧘"
                    8 -> "🌲"
                    else -> "🌃"
                }
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clickable { 
                            if (isSelected) {
                                onSelectionChanged(selectedKeywords - keyword)
                            } else {
                                onSelectionChanged(selectedKeywords + keyword)
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surface,
                    shadowElevation = if (isSelected) 4.dp else 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        
                        Text(
                            text = keyword,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LyricSelectionStep(
    options: List<String>,
    selectedLyric: String?,
    onSelectionChanged: (String?) -> Unit,
    textNote: String,
    onNoteChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
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
        
        // 歌词选项列表
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            options.forEach { lyric ->
                val isSelected = lyric == selectedLyric
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectionChanged(lyric) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surface,
                    shadowElevation = if (isSelected) 4.dp else 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "\"$lyric\"",
                                style = MaterialTheme.typography.bodyLarge,
                                fontStyle = FontStyle.Italic
                            )
                        }
                        
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // 笔记输入框
        OutlinedTextField(
            value = textNote,
            onValueChange = onNoteChanged,
            label = { Text("想分享点什么吗？") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp),
            maxLines = 3
        )
    }
}

@Composable
fun StepProgressIndicator(
    steps: Int,
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (i in 1..steps) {
            val isActive = i <= currentStep
            val color = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun MoodSliderStep(
    moodValue: Float,
    onMoodValueChange: (Float) -> Unit
) {
    val emojis = listOf("😢", "😕", "😐", "🙂", "😊")
    val gradientColors = listOf(
        Color(0xFFE53935), // 红色 - 很难过
        Color(0xFFFFB300), // 橙色 - 有点难过
        Color(0xFFFFEB3B), // 黄色 - 一般
        Color(0xFF8BC34A), // 浅绿色 - 开心
        Color(0xFF43A047)  // 绿色 - 很开心
    )
    
    // 根据滑杆值选择显示的表情
    val emojiIndex = (moodValue * (emojis.size - 1)).toInt().coerceIn(0, emojis.size - 1)
    
    // 表情大小动画
    val emojiSize by animateDpAsState(
        targetValue = if (moodValue > 0.8f) 120.dp else if (moodValue < 0.2f) 80.dp else 100.dp
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "今天感觉如何？",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 40.dp)
        )
        
        // 表情显示
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            gradientColors[emojiIndex].copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emojis[emojiIndex],
                fontSize = emojiSize.value.sp,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // 滑杆
        Text(
            text = when (emojiIndex) {
                0 -> "很难过"
                1 -> "有点难过"
                2 -> "感觉一般"
                3 -> "有点开心"
                else -> "非常开心"
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    brush = Brush.horizontalGradient(colors = gradientColors)
                )
        )
        
        Slider(
            value = moodValue,
            onValueChange = onMoodValueChange,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            )
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("很难过", color = gradientColors.first())
            Text("一般", color = gradientColors[gradientColors.size / 2])
            Text("很开心", color = gradientColors.last())
        }
    }
}
