package com.example.moodmelody

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.core.app.ActivityCompat
import com.example.moodmelody.network.RetrofitClient
import com.example.moodmelody.viewmodel.MusicViewModel
import androidx.compose.material3.Text
import androidx.navigation.compose.rememberNavController
import com.example.moodmelody.ui.theme.MoodmelodyTheme
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
