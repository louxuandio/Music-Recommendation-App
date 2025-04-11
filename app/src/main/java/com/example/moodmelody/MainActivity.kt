package com.example.moodmelody

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.widget.CalendarView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.TextStyle


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

// ==================== 其他Compose示例 ====================
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
    onAnswerSelected: (Int) -> Unit
) {
    var currentPage by remember { mutableStateOf(1) }
    var moodIndex by remember { mutableStateOf(2f) }
    var selectedKeywords by remember { mutableStateOf(listOf<String>()) }
    var selectedActivity by remember { mutableStateOf<String?>(null) }
    var textNote by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(paddingValues),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (currentPage) {
            1 -> MoodSliderPage()
            2 -> KeywordSelectPage(
                selectedKeywords = selectedKeywords,
                onKeywordToggle = { keyword ->
                    selectedKeywords = if (selectedKeywords.contains(keyword)) {
                        selectedKeywords - keyword
                    } else {
                        selectedKeywords + keyword
                    }
                }
            )
            3 -> CustomInputPage(
                selectedActivity = selectedActivity,
                onActivitySelected = { selectedActivity = it }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = textNote,
                onValueChange = { textNote = it },
                label = { Text("Anything you'd like to add?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(vertical = 8.dp, horizontal = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { if (currentPage > 1) currentPage-- },
                    enabled = currentPage > 1
                ) {
                    Text("Previous")
                }

                Button(
                    onClick = {
                        if (currentPage < 3) currentPage++
                        else {
                            // View Result Action
                        }
                    }
                ) {
                    Text(if (currentPage == 3) "View Result" else "Next")
                }
            }
        }
    }


}
@Composable
fun MoodSliderPage(){
    var moodIndex by remember { mutableStateOf(2f) }  // 默认中间

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
        onMoodChange = { moodIndex = it }
    )

}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeywordSelectPage(
    selectedKeywords: List<String>,
    onKeywordToggle: (String) -> Unit
) {
    val keywords = listOf("Anxious", "Excited", "Tired", "Focused", "Overwhelmed")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Pick some words that describe your mood", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            keywords.forEach { keyword ->
                val isSelected = selectedKeywords.contains(keyword)
                Button(
                    onClick = { onKeywordToggle(keyword) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color(0xFF5E35B1) else Color.LightGray
                    )
                ) {
                    Text(keyword)
                }
            }
        }
    }
}

@Composable
fun CustomInputPage(
    selectedActivity: String?,
    onActivitySelected: (String) -> Unit
) {
    val activities = listOf("Dancing", "Singing", "Playing in the rain", "Sleeping", "Screaming into the void")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("What do you feel like doing now?", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        activities.forEach { activity ->
            Button(
                onClick = { onActivitySelected(activity) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activity == selectedActivity) Color(0xFF5E35B1) else Color.LightGray
                ),
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth(0.8f)
            ) {
                Text(activity)
            }
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
    private lateinit var musicViewModel: MusicViewModel

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

        // 获取ViewModel实例
        musicViewModel = (application as MoodMelodyApp).musicViewModel

        // 请求位置权限
        requestLocationPermissions()

        // 处理Spotify认证返回的Token(如果有)
        handleSpotifyAuthentication(intent)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MoodMelodyApp(musicViewModel)
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
        handleSpotifyAuthentication(intent)
    }

    private fun handleSpotifyAuthentication(intent: Intent) {
        val uri = intent.data
        if (uri != null && uri.toString().startsWith("moodmelody://callback")) {
            val fragment = uri.fragment
            if (fragment != null) {
                val params = fragment.split("&").associate {
                    val parts = it.split("=")
                    if (parts.size >= 2) parts[0] to parts[1] else parts[0] to ""
                }
                val token = params["access_token"]
                if (token != null) {
                    // 设置Token到RetrofitClient
                    RetrofitClient.updateSpotifyToken(token)
                    // 你也可以存到SharedPreferences，以便下次启动App不必再次登录
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodMelodyApp(viewModel: MusicViewModel) {
    var selectedTab by remember { mutableStateOf(0) }

    // 1) 判断当前是否有Token
    var hasSpotifyToken by remember {
        mutableStateOf(RetrofitClient.hasToken())
    }

    // 2) 从 ViewModel 读取搜索/推荐/错误/加载等状态
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()

    // 获取天气数据
    val currentWeather by viewModel.currentWeather.collectAsState()

    // 播放器中的当前歌曲
    val currentSong by viewModel.currentSong.collectAsState()

    // Home Tab 的一些UI状态
    var selectedMood by remember { mutableStateOf<String?>(null) }
    var moodIntensity by remember { mutableStateOf(3) }
    var showMusicRecommendations by remember { mutableStateOf(false) }

    // Search Tab
    var searchQuery by remember { mutableStateOf("") }

    // Test Tab
    var currentTestQuestion by remember { mutableStateOf(0) }
    var testAnswers by remember { mutableStateOf(listOf<Int>()) }

    // 格式化天气显示
    val weatherDisplay = if (currentWeather != null) {
        val weatherEmoji = when {
            currentWeather!!.icon.startsWith("1") -> "☀️" // 晴天
            currentWeather!!.icon.startsWith("3") -> "🌥️" // 多云
            currentWeather!!.icon.startsWith("4") -> "☁️" // 阴天
            currentWeather!!.icon.startsWith("5") -> "🌧️" // 雨天
            currentWeather!!.icon.startsWith("6") -> "❄️" // 雪
            currentWeather!!.icon.startsWith("7") -> "🌫️" // 雾霾
            currentWeather!!.icon.startsWith("8") -> "🌪️" // 风暴
            else -> "🌈" // 其他
        }
        "$weatherEmoji ${currentWeather!!.text}, ${currentWeather!!.temp}°C"
    } else {
        "Loading weather..."
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MoodMelody") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = "Test") },
                    label = { Text("Test") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Outlined.Info, contentDescription = "Stats") },
                    label = { Text("Stats") },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 }
                )
            }
        }
    ) { paddingValues ->
        // 3) 如果还没有 Token，就显示一个登录按钮
        if (!hasSpotifyToken) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                SpotifyLoginButton(
                    onLoginSuccess = {
                        // 当登录成功(拿到token)后, 刷新 hasSpotifyToken
                        hasSpotifyToken = true
                    }
                )
            }
        } else {
            // 有Token, 正常显示4个Tab页
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> {
                        HomeScreen(
                            paddingValues = PaddingValues(
                                top = paddingValues.calculateTopPadding(),
                                bottom = paddingValues.calculateBottomPadding() +
                                        if (currentSong != null) 80.dp else 0.dp,
                                start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                                end = paddingValues.calculateEndPadding(LayoutDirection.Ltr)
                            ),
                            currentWeather = weatherDisplay,  // 使用格式化后的天气信息
                            selectedMood = selectedMood,
                            moodIntensity = moodIntensity,
                            showMusicRecommendations = showMusicRecommendations,
                            isLoading = isLoading,
                            errorMessage = errorMessage,
                            recommendations = recommendations,
                            onMoodSelected = { mood -> selectedMood = mood },
                            onIntensityChanged = { intensity -> moodIntensity = intensity },
                            onGetRecommendations = {
                                // 传递当前天气信息到推荐函数
                                viewModel.getRecommendations(
                                    mood = selectedMood ?: "neutral",
                                    intensity = moodIntensity,
                                    weather = currentWeather
                                )
                                showMusicRecommendations = true
                            },
                            onBackToMoodSelection = { showMusicRecommendations = false },
                            onSongClick = { song -> viewModel.playSong(song) }
                        )
                        //TODO: 随便放几个歌单在这
                    }
                    1 -> {
                        SearchScreen(
                            paddingValues = PaddingValues(
                                top = paddingValues.calculateTopPadding(),
                                bottom = paddingValues.calculateBottomPadding() +
                                        if (currentSong != null) 80.dp else 0.dp,
                                start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                                end = paddingValues.calculateEndPadding(LayoutDirection.Ltr)
                            ),
                            searchQuery = searchQuery,
                            isSearching = isLoading,
                            searchResults = searchResults,
                            onSearchQueryChanged = { searchQuery = it },
                            onSearch = {
                                viewModel.searchMusic(searchQuery)
                            },
                            onSongClick = { song -> viewModel.playSong(song) }
                        )
                        //DONE: 这个 search 文字改一下，有点遮挡
                    }
                    2 -> {
                        // Mood Test Tab
                        //进行中：设置问题
                        MoodTestScreen(
                            paddingValues = PaddingValues(
                                top = paddingValues.calculateTopPadding(),
                                bottom = paddingValues.calculateBottomPadding() +
                                        if (currentSong != null) 80.dp else 0.dp,
                                start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                                end = paddingValues.calculateEndPadding(LayoutDirection.Ltr)
                            ),
                            currentQuestion = currentTestQuestion,
                            onAnswerSelected = { answer ->
                                testAnswers = testAnswers + answer
                                if (currentTestQuestion < 4) {
                                    currentTestQuestion += 1
                                } else {
                                    // Test completed, determine mood based on answers
                                    val sum = testAnswers.sum()
                                    val determinedMood = when (sum) {
                                        in 0..5 -> "Sad"
                                        in 6..10 -> "Calm"
                                        in 11..15 -> "Neutral"
                                        in 16..20 -> "Happy"
                                        else -> "Excited"
                                    }
                                    selectedMood = determinedMood
                                    selectedTab = 0 // 回到主页
                                    currentTestQuestion = 0
                                    testAnswers = listOf()
                                }
                            }
                        )
                    }
                    3 -> {
                        //TODO：calendar view + 心情日记 + 歌曲(optional)
                        // Stats Tab
                        StatsScreen(
                            paddingValues = PaddingValues(
                                top = paddingValues.calculateTopPadding(),
                                bottom = paddingValues.calculateBottomPadding() +
                                        if (currentSong != null) 80.dp else 0.dp,
                                start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                                end = paddingValues.calculateEndPadding(LayoutDirection.Ltr)
                            )
                        )
                    }
                }

                // 底部播放器
                if (currentSong != null) {
                    Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                        SongPlayer(
                            viewModel = viewModel,
                            modifier = Modifier.padding(
                                bottom = paddingValues.calculateBottomPadding()
                            )
                        )
                    }
                }
            }
        }
    }
}

/** 一个单独的Composable来放"Login with Spotify"按钮,用LocalContext获取Context. */
@Composable
fun SpotifyLoginButton(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    Button(onClick = {
        val clientId = "7f598bd5b59b4884b4e5db9997a05cc1" // TODO: 替换成真实ID
        val redirectUri = "moodmelody://callback"
        val scopes = "user-read-private%20playlist-read-private"
        val authUrl = "https://accounts.spotify.com/authorize" +
                "?client_id=$clientId" +
                "&response_type=token" +
                "&redirect_uri=$redirectUri" +
                "&scope=$scopes"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
        context.startActivity(intent)

        // 注意: 只有当 Spotify 成功回调后, handleSpotifyAuthentication 里设置token,
        // UI 才会更新. 这里 onLoginSuccess() 可以选在别处时机调用,
        // 不过通常等 handleSpotifyAuthentication 解析成功后,
        // 重新进入Compose时 "hasSpotifyToken" 就会变true.
        // 你可以在 handleSpotifyAuthentication 解析完 token 后,
        // 直接 recreate Activity 或走别的机制让UI刷新.
        // 这里暂时留空.
    }) {
        Text("Login with Spotify")
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


//从日记 app复制过来的 read/save/delete file
//⚠️这个用的是 internal storage
// Function to save text to internal storage
fun saveToFile(context: Context, filename: String, content: String) {
    // MODE_PRIVATE means the file is only accessible to this app
    context.openFileOutput(filename, Context.MODE_PRIVATE).use { outputStream ->
        outputStream.write(content.toByteArray())
    }
}

// Function to read text from internal storage
fun readFromFile(context: Context, filename: String): String {
    return try {
        context.openFileInput(filename).bufferedReader().useLines { lines ->
            lines.joinToString("\n")
        }
    } catch (e: FileNotFoundException) {
        "File not found"
    }
}

// Function to delete file from internal storage
fun deleteFile(context: Context, filename: String): Boolean {
    return context.deleteFile(filename)
}

@Composable
fun StatsScreen(paddingValues: PaddingValues) {
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var selectedDate by remember { mutableStateOf(sdf.format(java.util.Date())) }
    var diaryText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var fontSize by remember { mutableStateOf(16.sp) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        LaunchedEffect(selectedDate) {
            val fileName = "$selectedDate.txt"
            val content = readFromFile(context, fileName)
            diaryText = content
            statusMessage = "Loaded $fileName"
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
            OutlinedTextField(
                value = diaryText,
                onValueChange = { diaryText = it },
                label = { Text("Your diary for $selectedDate") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = fontSize)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val fileName = "$selectedDate.txt"
                    saveToFile(context, fileName, diaryText)
                    statusMessage = "Saved to $fileName"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Store")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Status: $statusMessage")

            //TODO：有过记录的日子，日期加颜色（表示心情）
            //TODO：有记录的日子，显示日记 AND 那天的推荐歌单 AND 那天的天气
        }
    }
}