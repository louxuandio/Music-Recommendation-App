package com.example.moodmelody

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext           // 关键: 导入LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import com.example.moodmelody.network.RetrofitClient
import com.example.moodmelody.ui.SongPlayer
import com.example.moodmelody.viewmodel.MusicViewModel

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
fun MoodTestScreen(
    paddingValues: PaddingValues,
    currentQuestion: Int,
    onAnswerSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Question #$currentQuestion", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(16.dp))
            // 假设答案只有五个选项
            for (i in 1..5) {
                Button(onClick = { onAnswerSelected(i) }, modifier = Modifier.padding(4.dp)) {
                    Text("Answer $i")
                }
            }
        }
    }
}

@Composable
fun StatsScreen(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Stats Screen (未实现)")
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取ViewModel实例
        musicViewModel = (application as MoodMelodyApp).musicViewModel

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
    // 在MoodMelodyApp composable中的这行
    var hasSpotifyToken by remember {
        mutableStateOf(RetrofitClient.hasToken())
    }

    // 2) 从 ViewModel 读取搜索/推荐/错误/加载等状态
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()

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

    // 模拟天气
    val currentWeather = "Sunny, 72°F"

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
                            currentWeather = currentWeather,
                            selectedMood = selectedMood,
                            moodIntensity = moodIntensity,
                            showMusicRecommendations = showMusicRecommendations,
                            isLoading = isLoading,
                            errorMessage = errorMessage,
                            recommendations = recommendations,
                            onMoodSelected = { mood -> selectedMood = mood },
                            onIntensityChanged = { intensity -> moodIntensity = intensity },
                            onGetRecommendations = {
                                viewModel.getRecommendations(
                                    mood = selectedMood ?: "neutral",
                                    intensity = moodIntensity
                                )
                                showMusicRecommendations = true
                            },
                            onBackToMoodSelection = { showMusicRecommendations = false },
                            onSongClick = { song -> viewModel.playSong(song) }
                        )
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
                    }
                    2 -> {
                        // Mood Test Tab
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
                    Text("Search")
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
