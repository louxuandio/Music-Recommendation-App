package com.example.moodmelody

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MoodMelodyApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodMelodyApp() {
    var selectedTab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    // States for Home tab
    var selectedMood by remember { mutableStateOf<String?>(null) }
    var moodIntensity by remember { mutableStateOf(3) }
    var showMusicRecommendations by remember { mutableStateOf(false) }

    // States for Search tab
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Song>>(emptyList()) }

    // States for Test tab
    var currentTestQuestion by remember { mutableStateOf(0) }
    var testAnswers by remember { mutableStateOf(listOf<Int>()) }

    // 现在是初版 故采用的api模拟方法（出自claude） 后续替换为开发者sdk 现在还暂时没有办法搜索到音乐
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var recommendations by remember { mutableStateOf<List<Song>>(emptyList()) }
    // 模拟的天气界面 后续替换为真实api
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
        when (selectedTab) {
            0 -> { // Home Tab
                HomeScreen(
                    paddingValues = paddingValues,
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
                        scope.launch {
                            try {
                                isLoading = true
                                errorMessage = null
                                // Simulate API call
                                delay(1000)
                                recommendations = getMockRecommendations(selectedMood ?: "neutral")
                                showMusicRecommendations = true
                            } catch (e: Exception) {
                                errorMessage = "Failed to get recommendations: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    onBackToMoodSelection = { showMusicRecommendations = false }
                )
            }
            1 -> { // Search Tab
                SearchScreen(
                    paddingValues = paddingValues,
                    searchQuery = searchQuery,
                    isSearching = isSearching,
                    searchResults = searchResults,
                    onSearchQueryChanged = { searchQuery = it },
                    onSearch = {
                        scope.launch {
                            try {
                                isSearching = true
                                // 模拟api访问
                                delay(800)
                                searchResults = searchMusic(searchQuery)
                            } catch (e: Exception) {
                                // 错误信息（后续删除）
                            } finally {
                                isSearching = false
                            }
                        }
                    }
                )
            }
            2 -> { // Test Tab
                MoodTestScreen(
                    paddingValues = paddingValues,
                    currentQuestion = currentTestQuestion,
                    onAnswerSelected = { answer ->
                        testAnswers = testAnswers + answer
                        if (currentTestQuestion < 4) {
                            currentTestQuestion += 1
                        } else {
                            // Test completed, determine mood based on answers
                            val determinedMood = when (testAnswers.sum()) {
                                in 0..5 -> "Sad"
                                in 6..10 -> "Calm"
                                in 11..15 -> "Neutral"
                                in 16..20 -> "Happy"
                                else -> "Excited"
                            }
                            selectedMood = determinedMood
                            selectedTab = 0 // Go back to home tab
                            currentTestQuestion = 0
                            testAnswers = listOf()
                        }
                    }
                )
            }
            3 -> { // Stats Tab
                StatsScreen(paddingValues = paddingValues)
            }
        }
    }
}

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
    onBackToMoodSelection: () -> Unit
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
                        SongItem(song)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    paddingValues: PaddingValues,
    searchQuery: String,
    isSearching: Boolean,
    searchResults: List<Song>,
    onSearchQueryChanged: (String) -> Unit,
    onSearch: () -> Unit
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
                    SongItem(song)
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
fun MoodTestScreen(
    paddingValues: PaddingValues,
    currentQuestion: Int,
    onAnswerSelected: (Int) -> Unit
) {
    val questions = listOf(
        "How energetic do you feel right now?",
        "How social do you feel today?",
        "How focused are you on your tasks?",
        "How optimistic do you feel about today?",
        "How relaxed do you feel currently?"
    )

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Mood Test (${currentQuestion + 1}/5)",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = questions[currentQuestion],
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Answer options
        val options = listOf("Not at all", "Slightly", "Moderately", "Very", "Extremely")
        val colors = listOf(
            Color(0xFFE57373), // Red-ish
            Color(0xFFFFB74D), // Orange-ish
            Color(0xFFFFF176), // Yellow-ish
            Color(0xFF81C784), // Green-ish
            Color(0xFF64B5F6)  // Blue-ish
        )

        options.forEachIndexed { index, option ->
            Card(
                onClick = { onAnswerSelected(index) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors[index].copy(alpha = 0.7f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option,
                        fontWeight = FontWeight.Bold
                    )
                    // 丑陋的ui 后续更改
                    Text(
                        text = when(index) {
                            0 -> "😞"
                            1 -> "😐"
                            2 -> "🙂"
                            3 -> "😊"
                            4 -> "😄"
                            else -> ""
                        },
                        fontSize = 24.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { /* Voice input would be implemented here */ }) {
            Text("Or use voice input instead")
        }

        TextButton(onClick = { /* Text input would be implemented here */ }) {
            Text("Or write how you feel")
        }
    }
}

@Composable
fun StatsScreen(paddingValues: PaddingValues) {
    // This would show charts and statistics in the future
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Mood Statistics",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "This feature will be available in the next update!",
            textAlign = TextAlign.Center
        )

        // Placeholder for future charts
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            Text("Mood trends will appear here")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodSelectionGrid(
    onMoodSelected: (String) -> Unit,
    selectedMood: String?
) {
    val moods = listOf(
        "Happy" to Color(0xFF4CAF50),
        "Calm" to Color(0xFF2196F3),
        "Sad" to Color(0xFF9C27B0),
        "Angry" to Color(0xFFF44336),
        "Anxious" to Color(0xFFFF9800),
        "Tired" to Color(0xFF795548)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(moods) { (mood, color) ->
            val isSelected = selectedMood == mood

            Card(
                onClick = { onMoodSelected(mood) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) color else color.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = mood,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun SongItem(song: Song) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
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
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = song.artist,
                    color = Color.Gray
                )

                if (song.genre != null) {
                    Text(
                        text = song.genre,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// Data model and helper functions
data class Song(
    val title: String,
    val artist: String,
    val coverUrl: String? = null,
    val genre: String? = null
)

// Mock function for music recommendations
fun getMockRecommendations(mood: String): List<Song> {
    return when (mood.lowercase()) {
        "happy" -> listOf(
            Song("Happy", "Pharrell Williams", genre = "Pop"),
            Song("Can't Stop the Feeling!", "Justin Timberlake", genre = "Pop"),
            Song("Walking on Sunshine", "Katrina & The Waves", genre = "Rock"),
            Song("Good as Hell", "Lizzo", genre = "Pop"),
            Song("Uptown Funk", "Mark Ronson ft. Bruno Mars", genre = "Funk")
        )
        "sad" -> listOf(
            Song("Someone Like You", "Adele", genre = "Pop"),
            Song("Fix You", "Coldplay", genre = "Alternative"),
            Song("The Night We Met", "Lord Huron", genre = "Indie"),
            Song("When the Party's Over", "Billie Eilish", genre = "Pop"),
            Song("Falling", "Harry Styles", genre = "Pop")
        )
        "calm" -> listOf(
            Song("Weightless", "Marconi Union", genre = "Ambient"),
            Song("Claire de Lune", "Claude Debussy", genre = "Classical"),
            Song("Breathe Me", "Sia", genre = "Pop"),
            Song("Yellow", "Coldplay", genre = "Alternative"),
            Song("Pure Shores", "All Saints", genre = "Pop")
        )
        "angry" -> listOf(
            Song("Break Stuff", "Limp Bizkit", genre = "Nu Metal"),
            Song("Bulls on Parade", "Rage Against the Machine", genre = "Rock"),
            Song("Du Hast", "Rammstein", genre = "Industrial Metal"),
            Song("Killing in the Name", "Rage Against the Machine", genre = "Rock"),
            Song("Bodies", "Drowning Pool", genre = "Nu Metal")
        )
        "anxious" -> listOf(
            Song("Breathe", "Pink Floyd", genre = "Progressive Rock"),
            Song("Breathe Me", "Sia", genre = "Pop"),
            Song("Everybody's Changing", "Keane", genre = "Alternative"),
            Song("Unsteady", "X Ambassadors", genre = "Alternative"),
            Song("Fix You", "Coldplay", genre = "Alternative")
        )
        "tired" -> listOf(
            Song("Fade Into You", "Mazzy Star", genre = "Dream Pop"),
            Song("Asleep", "The Smiths", genre = "Indie"),
            Song("Sleep", "Eric Whitacre", genre = "Classical"),
            Song("I'm So Tired...", "Lauv & Troye Sivan", genre = "Pop"),
            Song("Tired", "Alan Walker", genre = "Electronic")
        )
        else -> listOf(
            Song("Imagine", "John Lennon", genre = "Rock"),
            Song("What a Wonderful World", "Louis Armstrong", genre = "Jazz"),
            Song("Here Comes the Sun", "The Beatles", genre = "Rock"),
            Song("Somewhere Over the Rainbow", "Israel Kamakawiwo'ole", genre = "Folk"),
            Song("Don't Worry Be Happy", "Bobby McFerrin", genre = "Jazz")
        )
    }
}

// Mock function for music search
fun searchMusic(query: String): List<Song> {
    // Mock music library for search functionality
    val mockMusicLibrary = listOf(
        Song("Shape of You", "Ed Sheeran", genre = "Pop"),
        Song("Blinding Lights", "The Weeknd", genre = "Synth-pop"),
        Song("Dance Monkey", "Tones and I", genre = "Dance-pop"),
        Song("Someone You Loved", "Lewis Capaldi", genre = "Pop"),
        Song("Bad Guy", "Billie Eilish", genre = "Electropop"),
        Song("Watermelon Sugar", "Harry Styles", genre = "Pop rock"),
        Song("Don't Start Now", "Dua Lipa", genre = "Nu-disco"),
        Song("Circles", "Post Malone", genre = "Pop"),
        Song("Everything I Wanted", "Billie Eilish", genre = "Electropop"),
        Song("Memories", "Maroon 5", genre = "Pop"),
        Song("Before You Go", "Lewis Capaldi", genre = "Pop"),
        Song("Adore You", "Harry Styles", genre = "Pop rock"),
        Song("Lose You To Love Me", "Selena Gomez", genre = "Pop"),
        Song("Señorita", "Shawn Mendes, Camila Cabello", genre = "Pop"),
        Song("My Oh My", "Camila Cabello ft. DaBaby", genre = "Pop")
    )

    return if (query.isBlank()) {
        emptyList()
    } else {
        mockMusicLibrary.filter { song ->
            song.title.contains(query, ignoreCase = true) ||
                    song.artist.contains(query, ignoreCase = true) ||
                    (song.genre?.contains(query, ignoreCase = true) ?: false)
        }
    }
}

// Helper function for mood-based motivational texts
fun getMoodMotivationalText(mood: String?): String {
    return when (mood?.lowercase()) {
        "happy" -> "Keep that positive energy flowing! Music can help sustain your joy."
        "sad" -> "It's okay to feel down sometimes. These songs will help you process your emotions."
        "calm" -> "Maintain your peaceful state with these gentle melodies."
        "angry" -> "Channel that energy with these powerful tracks."
        "anxious" -> "Take a deep breath. These songs can help ease your mind."
        "tired" -> "Find your second wind with these carefully selected tunes."
        else -> "Music has the power to transform your day. Enjoy these recommendations!"
    }
}