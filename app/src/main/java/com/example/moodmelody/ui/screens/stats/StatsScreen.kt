package com.example.moodmelody.ui.screens.stats

import android.icu.text.SimpleDateFormat
import android.util.Log
import android.widget.CalendarView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.moodmelody.viewmodel.MusicViewModel
import com.example.moodmelody.data.MoodEntry
import java.util.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavController,
    paddingValues: PaddingValues
) {
    val context = LocalContext.current
    val app = context.applicationContext as com.example.moodmelody.MoodMelodyApp
    val viewModel = app.musicViewModel
    
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("情绪统计") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = Modifier.padding(paddingValues)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab标签栏
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Mood Calendar") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Mood Trends") }
                )
            }
            
            // 标签页内容
            when (selectedTab) {
                0 -> MoodCalendarTab(viewModel)
                1 -> MoodTrendTab()
            }
        }
    }
}

@Composable
fun MoodCalendarTab(viewModel: MusicViewModel) {
    // 日期格式化器
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var selectedDate by remember { mutableStateOf(sdf.format(Date())) }
    val loadedEntry by viewModel.loadedEntry.collectAsStateWithLifecycle()
    
    // 获取当前月份的记录
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)
    
    // 记录月份情绪数据
    val monthEntries by viewModel.monthEntries.collectAsStateWithLifecycle()
    
    // 当月份改变时加载数据
    LaunchedEffect(currentYear, currentMonth) {
        viewModel.loadEntriesForMonth(currentYear, currentMonth)
    }
    
    // 当选择日期变化时加载数据
    LaunchedEffect(selectedDate) {
        viewModel.loadEntryByDate(selectedDate)
    }
    
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 月份标题
        Text(
            text = "${getMonthName(currentMonth)} $currentYear",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 自定义日历
        AndroidView(
            factory = { ctx ->
                CalendarView(ctx).apply {
                    // 基本设置
                    setOnDateChangeListener { _, year, month, dayOfMonth ->
                        val cal = Calendar.getInstance().apply {
                            set(year, month, dayOfMonth)
                        }
                        selectedDate = sdf.format(cal.time)
                    }
                    
                    // 设置日期变更监听器
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
                .height(300.dp),
            update = { calendarView ->
                // 日历视图更新时的操作
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 情绪标记指示器
        if (monthEntries.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Mood records for this month: ${monthEntries.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // 显示本月的情绪记录日期列表
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("d", Locale.getDefault())
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                // 预处理数据，过滤掉无效日期
                val validEntries = monthEntries.mapNotNull { entry ->
                    try {
                        val date = sdf.parse(entry.date)
                        if (date != null) {
                            calendar.time = date
                            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
                            Pair(dayOfMonth, entry)
                        } else null
                    } catch (e: Exception) {
                        Log.e("StatsScreen", "日期解析错误: ${e.message}")
                        null
                    }
                }
                
                // 显示有效的条目
                items(validEntries) { (dayOfMonth, entry) ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(getMoodColor(entry.result).copy(alpha = 0.5f))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayOfMonth.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 显示选中日期的情绪记录
        val entries = monthEntries.filter { it.result.isNotEmpty() }
        
        if (entries.isNotEmpty()) {
            // Find if there's an entry for the selected date
            val selectedEntry = entries.find { it.date == selectedDate }

            
            // Add subtitle for current selection
            if (selectedEntry != null) {
                Text(
                    text = "Selected date: $selectedDate", 
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(entries) { entry ->
                    val isSelected = entry.date == selectedDate
                    MoodEntryCard(
                        entry = entry, 
                        isSelected = isSelected,
                        onClick = {
                            // Update selected date when clicking on a card
                            selectedDate = entry.date
                        }
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No mood entries for this month")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 情绪颜色图例
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MoodLegend("Sadness", getMoodColor("sad"))
            MoodLegend("Calm", getMoodColor("calm"))
            MoodLegend("Neutral", getMoodColor("neutral"))
            MoodLegend("Joy", getMoodColor("happy"))
            MoodLegend("Excitement", getMoodColor("excited"))
        }
    }
}

@Composable
fun MoodTrendTab() {
    // 模拟过去7天的情绪数据
    val moodData = listOf(
        0.2f, 0.4f, 0.6f, 0.3f, 0.7f, 0.9f, 0.5f
    )
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "Mood Trends for the Past 7 Days",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // 图表
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(bottom = 32.dp)
        ) {
            MoodLineChart(moodData)
        }
        
        // 图例
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 情绪统计卡片
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MoodStatCard("Average Mood", "Positive", Color(0xFF8BC34A))
            MoodStatCard("Best Day", "Saturday", Color(0xFF1DB954))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MoodStatCard("Mood Variability", "Stable", Color(0xFF6B4EFF))
            MoodStatCard("Recommended Activity", "Music Therapy", Color(0xFF8B7AFF))
        }
    }
}

@Composable
fun MoodLineChart(data: List<Float>) {
    val animatedData = data.mapIndexed { index, value ->
        val animatedValue by animateFloatAsState(
            targetValue = value,
            animationSpec = tween(durationMillis = 1000, delayMillis = index * 100)
        )
        animatedValue
    }
    
    // 提前获取主题颜色
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val width = size.width
        val height = size.height
        val xStep = width / (data.size - 1)
        
        // 画线
        for (i in 0 until data.size - 1) {
            val startX = i * xStep
            val startY = height - (animatedData[i] * height)
            val endX = (i + 1) * xStep
            val endY = height - (animatedData[i + 1] * height)
            
            drawLine(
                color = primaryColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }
        
        // 画点
        data.forEachIndexed { index, _ ->
            val x = index * xStep
            val y = height - (animatedData[index] * height)
            
            drawCircle(
                color = primaryColor,
                radius = 8f,
                center = Offset(x, y)
            )
            
            drawCircle(
                color = surfaceColor,
                radius = 4f,
                center = Offset(x, y)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodStatCard(title: String, value: String, color: Color) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun MoodLegend(text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

fun getMoodColor(mood: String): Color {
    return when (mood) {
        "excited" -> Color(0xFF43A047) // 绿色
        "happy" -> Color(0xFF8BC34A)   // 浅绿色
        "neutral" -> Color(0xFFFFEB3B) // 黄色
        "calm" -> Color(0xFF5C6BC0)    // 蓝紫色
        "sad" -> Color(0xFFE53935)     // 红色
        else -> Color.Gray
    }
}

// 获取情绪的显示文本
fun getMoodDisplayText(mood: String): String {
    return when (mood) {
        "excited" -> "Excitement"
        "happy" -> "Joy"
        "neutral" -> "Neutral"
        "calm" -> "Calm"
        "sad" -> "Sadness"
        else -> mood
    }
}

fun getMonthName(month: Int): String {
    return when (month) {
        Calendar.JANUARY -> "January"
        Calendar.FEBRUARY -> "February"
        Calendar.MARCH -> "March"
        Calendar.APRIL -> "April"
        Calendar.MAY -> "May"
        Calendar.JUNE -> "June"
        Calendar.JULY -> "July"
        Calendar.AUGUST -> "August"
        Calendar.SEPTEMBER -> "September"
        Calendar.OCTOBER -> "October"
        Calendar.NOVEMBER -> "November"
        Calendar.DECEMBER -> "December"
        else -> ""
    }
}

@Composable
fun MoodEntryCard(
    entry: MoodEntry, 
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .width(300.dp)
            .padding(vertical = 8.dp)
            .let {
                if (isSelected) {
                    it.then(Modifier.border(
                        width = 2.dp, 
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp)
                    ))
                } else {
                    it
                }
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        ),
        colors = if (isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Date: ${entry.date}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Mood: ${entry.result.replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            // Display user's text input prominently after the mood if available
            if (entry.note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "\"${entry.note}\"",
                            style = MaterialTheme.typography.bodyLarge,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Display emotion values
            Text(
                text = "Joy: ${(entry.happy * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "Sadness: ${(entry.sad * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "Calm: ${(entry.calm * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "Excitement: ${(entry.excited * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (entry.keywords.isNotEmpty()) {
                Text(
                    text = "Keywords: ${entry.keywords.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (entry.activity != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Activity: ${entry.activity}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
} 