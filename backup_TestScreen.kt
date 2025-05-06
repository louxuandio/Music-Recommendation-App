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

// 修改lyricOptions定义为包含英文和中文的数据类
data class LyricOption(
    val english: String,
    val chinese: String,
    val emotions: Emotions
)

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
        "Rainy Walk" to Emotions(happy=0.30f, sad=0.50f, calm=0.70f, excited=0.20f),
        "Reading by the Window" to Emotions(happy=0.50f, sad=0.10f, calm=0.90f, excited=0.10f),
        "Running in the Grass" to Emotions(happy=0.80f, sad=0.00f, calm=0.20f, excited=0.90f),
        "Beach Walk" to Emotions(happy=0.70f, sad=0.05f, calm=0.85f, excited=0.40f),
        "Coffee Time" to Emotions(happy=0.60f, sad=0.10f, calm=0.75f, excited=0.20f),
        "Music Companion" to Emotions(happy=0.65f, sad=0.20f, calm=0.60f, excited=0.50f),
        "Chatting with Friends" to Emotions(happy=0.85f, sad=0.05f, calm=0.30f, excited=0.70f),
        "Solitary Reflection" to Emotions(happy=0.30f, sad=0.40f, calm=0.80f, excited=0.10f),
        "Forest Hike" to Emotions(happy=0.60f, sad=0.05f, calm=0.70f, excited=0.50f),
        "City Night View" to Emotions(happy=0.50f, sad=0.30f, calm=0.40f, excited=0.60f)
    )

    // 新的歌词选项列表，包含不同情绪的歌词和中英文翻译
    val lyricOptions = listOf(
        // 快乐/积极情绪歌词
        LyricOption(
            english = "I dance like thunder, and I love the silence after",
            chinese = "我跳舞时如同雷鸣，而我最喜欢的是之后的寂静",
            emotions = Emotions(happy=0.50f, sad=0.20f, calm=0.60f, excited=0.70f)
        ),
        LyricOption(
            english = "Every day is a canvas waiting for colors",
            chinese = "每一天都是一张等待上色的画布",
            emotions = Emotions(happy=0.80f, sad=0.05f, calm=0.40f, excited=0.60f)
        ),
        LyricOption(
            english = "We are the children who are not afraid of this world",
            chinese = "我们都是不畏惧这世界的孩子",
            emotions = Emotions(happy=0.70f, sad=0.10f, calm=0.30f, excited=0.75f)
        ),
        
        // 平静/放松情绪歌词
        LyricOption(
            english = "When time stops, feel the resonance of heartbeats",
            chinese = "当时间停止，感受心跳的共鸣",
            emotions = Emotions(happy=0.40f, sad=0.10f, calm=0.85f, excited=0.30f)
        ),
        LyricOption(
            english = "Never afraid of solitude, as music is eternal companion",
            chinese = "从来不曾害怕孤独，因为音乐是永恒的陪伴",
            emotions = Emotions(happy=0.35f, sad=0.25f, calm=0.80f, excited=0.25f)
        ),
        LyricOption(
            english = "Listening to raindrops on the leaves, nature's perfect melody",
            chinese = "聆听雨滴落在叶子上，大自然完美的旋律",
            emotions = Emotions(happy=0.30f, sad=0.20f, calm=0.90f, excited=0.15f)
        ),
        
        // 忧郁/伤感情绪歌词
        LyricOption(
            english = "I feel calm, knowing no one truly understands me",
            chinese = "我很平静，因为我知道没有人能真正理解我",
            emotions = Emotions(happy=0.10f, sad=0.70f, calm=0.60f, excited=0.00f)
        ),
        LyricOption(
            english = "Today's sunshine feels too bright",
            chinese = "今天的阳光感觉太刺眼",
            emotions = Emotions(happy=0.10f, sad=0.60f, calm=0.30f, excited=0.20f)
        ),
        LyricOption(
            english = "Missing someone tastes like coffee - bitter yet lingering",
            chinese = "思念一个人的味道，像咖啡般苦涩却回味无穷",
            emotions = Emotions(happy=0.20f, sad=0.80f, calm=0.40f, excited=0.10f)
        ),
        
        // 混合情绪歌词
        LyricOption(
            english = "The stars know my secrets, they shine but never tell",
            chinese = "星星知道我的秘密，它们闪烁却从不诉说",
            emotions = Emotions(happy=0.40f, sad=0.40f, calm=0.60f, excited=0.30f)
        ),
        LyricOption(
            english = "In my silence, there's a storm brewing",
            chinese = "在我的沉默中，有一场风暴正在酝酿",
            emotions = Emotions(happy=0.20f, sad=0.50f, calm=0.30f, excited=0.60f)
        ),
        LyricOption(
            english = "Every goodbye opens a door to new beginnings",
            chinese = "每一次告别都为新的开始打开一扇门",
            emotions = Emotions(happy=0.50f, sad=0.50f, calm=0.40f, excited=0.40f)
        )
    )
    
    // 根据当前心情值过滤歌词选项的函数
    fun getFilteredLyrics(moodValue: Float, selectedKeywords: List<String>): List<LyricOption> {
        // 根据心情滑块值确定情绪倾向
        val isHappy = moodValue >= 0.6f
        val isSad = moodValue <= 0.4f
        val isNeutral = moodValue > 0.4f && moodValue < 0.6f
        
        // 基于关键词计算平均情绪值
        val keywordEmotions = keywordOptions
            .filter { pair -> selectedKeywords.contains(pair.first) }
            .map { pair -> pair.second }
        
        // 如果没有关键词，直接根据情绪滑块值筛选
        if (keywordEmotions.isEmpty()) {
            // 创建两种不同的歌词组：主要匹配当前情绪的和提供情绪平衡的
            val mainMoodLyrics = when {
                isHappy -> lyricOptions.filter { it.emotions.happy > 0.5f }.shuffled()
                isSad -> lyricOptions.filter { it.emotions.sad > 0.5f }.shuffled()
                else -> lyricOptions.filter { it.emotions.calm > 0.5f }.shuffled()
            }
            
            // 平衡情绪的歌词（对比情绪）
            val balancingLyrics = when {
                isHappy -> lyricOptions.filter { it.emotions.calm > 0.6f && it.emotions.happy < 0.5f }.shuffled()
                isSad -> lyricOptions.filter { it.emotions.happy > 0.4f && it.emotions.excited > 0.3f }.shuffled()
                else -> lyricOptions.filter { 
                    (it.emotions.happy > 0.6f || it.emotions.sad > 0.6f) 
                }.shuffled()
            }
            
            // 混合结果：主要是匹配当前情绪的歌词，但添加1-2首平衡情绪的歌词
            val result = mutableListOf<LyricOption>()
            result.addAll(mainMoodLyrics.take(3))
            
            // 添加1-2首平衡情绪的歌词
            if (balancingLyrics.isNotEmpty()) {
                result.addAll(balancingLyrics.take(2))
            }
            
            // 确保总数不超过5首
            return result.take(5)
        }
        
        // 计算关键词的平均情绪值
        val avgKeywordEmotions = keywordEmotions.reduce { acc, emotions ->
            Emotions(
                happy = acc.happy + emotions.happy,
                sad = acc.sad + emotions.sad,
                calm = acc.calm + emotions.calm,
                excited = acc.excited + emotions.excited
            )
        }.let { sum ->
            Emotions(
                happy = sum.happy / keywordEmotions.size,
                sad = sum.sad / keywordEmotions.size,
                calm = sum.calm / keywordEmotions.size,
                excited = sum.excited / keywordEmotions.size
            )
        }
        
        // 主导情绪
        val dominantEmotion = when {
            avgKeywordEmotions.happy >= avgKeywordEmotions.sad && 
            avgKeywordEmotions.happy >= avgKeywordEmotions.calm && 
            avgKeywordEmotions.happy >= avgKeywordEmotions.excited -> "happy"
            
            avgKeywordEmotions.sad >= avgKeywordEmotions.happy && 
            avgKeywordEmotions.sad >= avgKeywordEmotions.calm && 
            avgKeywordEmotions.sad >= avgKeywordEmotions.excited -> "sad"
            
            avgKeywordEmotions.calm >= avgKeywordEmotions.happy && 
            avgKeywordEmotions.calm >= avgKeywordEmotions.sad && 
            avgKeywordEmotions.calm >= avgKeywordEmotions.excited -> "calm"
            
            else -> "excited"
        }
        
        // 创建三组歌词：
        // 1. 高度匹配当前情绪的
        // 2. 提供适度情绪平衡的
        // 3. 随机选择，增加多样性
        
        // 匹配主导情绪的歌词
        val matchingLyrics = lyricOptions.filter { lyric ->
            when (dominantEmotion) {
                "happy" -> lyric.emotions.happy > 0.6f
                "sad" -> lyric.emotions.sad > 0.6f
                "calm" -> lyric.emotions.calm > 0.6f
                else -> lyric.emotions.excited > 0.6f
            }
        }.shuffled()
        
        // 平衡情绪的歌词
        val balancingLyrics = lyricOptions.filter { lyric ->
            when (dominantEmotion) {
                // 如果用户心情开心，提供一些平静的歌词
                "happy" -> lyric.emotions.calm > 0.7f && lyric.emotions.happy < 0.5f
                
                // 如果用户心情悲伤，提供一些略微乐观但不过分开心的歌词
                "sad" -> lyric.emotions.happy in 0.4f..0.6f && lyric.emotions.sad < 0.4f
                
                // 如果用户心情平静，提供一些能唤起情感的歌词
                "calm" -> (lyric.emotions.happy > 0.5f || lyric.emotions.sad > 0.5f) && lyric.emotions.calm < 0.5f
                
                // 如果用户心情兴奋，提供一些能稳定情绪的歌词
                else -> lyric.emotions.calm > 0.6f && lyric.emotions.excited < 0.4f
            }
        }.shuffled()
        
        // 构建最终结果
        val result = mutableListOf<LyricOption>()
        
        // 添加2-3首匹配情绪的歌词
        result.addAll(matchingLyrics.take(3))
        
        // 添加1-2首平衡情绪的歌词
        if (balancingLyrics.isNotEmpty()) {
            result.addAll(balancingLyrics.take(2))
        }
        
        // 随机添加剩余歌词以确保至少有5首推荐
        if (result.size < 5) {
            val remainingOptions = lyricOptions.filter { it !in result }.shuffled()
            result.addAll(remainingOptions.take(5 - result.size))
        }
        
        // 返回最多5首混合情绪的歌词
        return result.take(5)
    }
    
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
            lyricOptions.find { it.english == lyric || it.chinese == lyric }?.let {
                allEmotions.add(it.emotions)
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
        
        // 获取当前天气信息
        val weatherText = currentWeather?.text ?: "Unknown"
        val weatherTemp = currentWeather?.temp ?: 0
        
        // 生成更详细的天气描述
        val detailedWeather = when {
            weatherText.contains("rain", ignoreCase = true) -> "Rainy"
            weatherText.contains("cloud", ignoreCase = true) -> "Cloudy"
            weatherText.contains("sun", ignoreCase = true) || weatherText.contains("clear", ignoreCase = true) -> "Sunny"
            weatherText.contains("snow", ignoreCase = true) -> "Snowy"
            weatherText.contains("fog", ignoreCase = true) || weatherText.contains("mist", ignoreCase = true) -> "Foggy"
            else -> weatherText
        }
        
        // 添加温度信息使天气描述更完整
        val weatherDescription = when {
            weatherTemp < 0 -> "Cold $detailedWeather"
            weatherTemp < 15 -> "Cool $detailedWeather" 
            weatherTemp > 30 -> "Hot $detailedWeather"
            else -> detailedWeather
        }
        
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
        
        // 使用AI推荐获取音乐建议，传递详细的天气描述
        viewModel.getAIRecommendation(
            moodScore = moodScore,
            keywords = selectedKeywords,
            lyric = selectedLyric ?: "",
            weather = weatherDescription
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
                            "Previous",
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
                            if (currentPage == 3) "View Result" else "Next",
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
                    text = "Step $currentPage / 3",
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
                    // 根据当前心情值过滤歌词选项，以更好匹配用户当前情绪
                    val filteredLyrics = getFilteredLyrics(moodValue, selectedKeywords)
                    LyricSelectionStep(
                        options = filteredLyrics,
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
                    contentDescription = "Next",
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
                        contentDescription = "Previous",
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
                        contentDescription = "Next",
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
                        contentDescription = "Previous",
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
                        contentDescription = "Complete",
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
            text = "Select Your Recent共鸣体验",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Text(
            text = "Click Card to Select, Multi-Select",
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
    options: List<LyricOption>,
    selectedLyric: String?,
    onSelectionChanged: (String?) -> Unit,
    textNote: String,
    onNoteChanged: (String) -> Unit
) {
    // 状态变量：是否显示中文
    var showChinese by remember { mutableStateOf(true) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
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
        
        // 歌词选项列表
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            options.forEach { lyricOption ->
                val displayText = if (showChinese) lyricOption.chinese else lyricOption.english
                val selectedText = if (showChinese) lyricOption.chinese else lyricOption.english
                val isSelected = selectedLyric == lyricOption.english || selectedLyric == lyricOption.chinese
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            onSelectionChanged(selectedText) 
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
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "\"$displayText\"",
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
            label = { Text(if (showChinese) "你想分享什么？" else "What Would You Like to Share?") },
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
        targetValue = if (moodValue > 0.8f) 120.dp else if (moodValue < 0.2f) 80.dp else 100.dp,
        label = "emojiSize"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "How Are You Feeling Today?",
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
                0 -> "Very Sad"
                1 -> "A Little Sad"
                2 -> "Neutral"
                3 -> "A Little Happy"
                else -> "Very Happy"
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
            Text("Very Sad", color = gradientColors.first())
            Text("Neutral", color = gradientColors[gradientColors.size / 2])
            Text("Very Happy", color = gradientColors.last())
        }
    }
}
