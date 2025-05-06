package com.example.moodmelody

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.moodmelody.network.RetrofitClient
import com.example.moodmelody.viewmodel.MusicViewModel
import androidx.compose.material3.Text
import androidx.navigation.compose.rememberNavController
import com.example.moodmelody.ui.theme.MoodmelodyTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.moodmelody.navigation.Navigation
import com.example.moodmelody.navigation.Screen
import com.example.moodmelody.ui.components.MiniPlayer
import com.example.moodmelody.player.SpotifyPlayerManager
import com.example.moodmelody.repository.SpotifyRepository
import com.example.moodmelody.repository.WeatherRepository
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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

// 修改lyricOptions定义为包含英文和中文的数据类
data class LyricOption(
    val english: String,
    val chinese: String,
    val emotions: Emotions
)

// ==================== TestScreen ====================
// Experience and lyric option lists
val keywordOptions = listOf(
    "Walking in the rain" to Emotions(happy=0.30f, sad=0.50f, calm=0.70f, excited=0.20f),
    "Reading by the window" to Emotions(happy=0.50f, sad=0.10f, calm=0.90f, excited=0.10f),
    "Running on grass" to Emotions(happy=0.80f, sad=0.00f, calm=0.20f, excited=0.90f),
    "Walking on the beach" to Emotions(happy=0.70f, sad=0.05f, calm=0.85f, excited=0.40f),
    "Coffee time" to Emotions(happy=0.60f, sad=0.10f, calm=0.75f, excited=0.20f),
    "Listening to music" to Emotions(happy=0.65f, sad=0.20f, calm=0.60f, excited=0.50f),
    "Chatting with friends" to Emotions(happy=0.85f, sad=0.05f, calm=0.30f, excited=0.70f),
    "Contemplating alone" to Emotions(happy=0.30f, sad=0.40f, calm=0.80f, excited=0.10f),
    "Hiking in the forest" to Emotions(happy=0.60f, sad=0.05f, calm=0.70f, excited=0.50f),
    "City nightscape" to Emotions(happy=0.50f, sad=0.30f, calm=0.40f, excited=0.60f)
)

val lyricOptions = listOf(
    LyricOption(
        english = "I dance like thunder, and I love the silence after",
        chinese = "我跳舞时如同雷鸣，而我最喜欢的是之后的寂静",
        emotions = Emotions(happy=0.20f, sad=0.40f, calm=0.60f, excited=0.50f)
    ),
    LyricOption(
        english = "I feel calm, knowing no one truly understands me",
        chinese = "我很平静，因为我知道没有人能真正理解我",
        emotions = Emotions(happy=0.10f, sad=0.70f, calm=0.80f, excited=0.00f)
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
    LyricOption(
        english = "We are children unafraid of this world",
        chinese = "我们都是不畏惧这世界的孩子",
        emotions = Emotions(happy=0.70f, sad=0.10f, calm=0.20f, excited=0.80f)
    ),
    LyricOption(
        english = "When time stops, feel the resonance of heartbeats",
        chinese = "当时间停止，感受心跳的共鸣",
        emotions = Emotions(happy=0.50f, sad=0.20f, calm=0.90f, excited=0.30f)
    ),
    LyricOption(
        english = "Never afraid of loneliness, because music is eternal company",
        chinese = "从来不曾害怕孤独，因为音乐是永恒的陪伴",
        emotions = Emotions(happy=0.60f, sad=0.30f, calm=0.70f, excited=0.40f)
    )
)

fun calculateMoodResult(
    selectedKeywords: List<String>,
    selectedLyric: String?
): Pair<Emotions, String> {
    val picked = keywordOptions
        .filter { selectedKeywords.contains(it.first) }
        .map { it.second } +
            lyricOptions.firstOrNull { it.english == selectedLyric || it.chinese == selectedLyric }?.emotions?.let { listOf(it) }.orEmpty()

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
                        musicViewModel = musicViewModel
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

@Composable
fun MainScreen(
    musicViewModel: MusicViewModel
) {
    val navController = rememberNavController()
    val currentSong by musicViewModel.playerManager.currentSong.collectAsStateWithLifecycle()
    val isPlaying by musicViewModel.playerManager.isPlaying.collectAsStateWithLifecycle()
    
    Scaffold(
        bottomBar = { 
            BottomNavBar(navController = navController)
        },
        content = { paddingValues ->
            Navigation(
                navController = navController,
                musicViewModel = musicViewModel,
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
        Screen.Player to R.drawable.ic_music_note
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
        Screen.Home -> "Home"
        Screen.Search -> "Search"
        Screen.Stats -> "Stats"
        Screen.Test -> "Test"
        Screen.Player -> "Player"
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