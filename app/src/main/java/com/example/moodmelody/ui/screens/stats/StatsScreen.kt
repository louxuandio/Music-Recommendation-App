package com.example.moodmelody.ui.screens.stats

import android.icu.text.SimpleDateFormat
import android.util.Log
import android.widget.CalendarView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
            // 标签页选择器
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("情绪日历") },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("情绪趋势") },
                    icon = { Icon(Icons.Default.ShowChart, contentDescription = null) }
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
    val loadedEntry by viewModel.loadedEntry.collectAsState()
    
    // 当选择日期变化时加载数据
    LaunchedEffect(selectedDate) {
        viewModel.loadEntryByDate(selectedDate)
    }
    
    // 模拟日历数据
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    val days = (1..daysInMonth).map { day ->
        Day(
            date = day,
            mood = when {
                day % 5 == 0 -> "excited"
                day % 5 == 1 -> "happy"
                day % 5 == 2 -> "neutral"
                day % 5 == 3 -> "calm"
                else -> "sad"
            }
        )
    }
    
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
        
        // 日历选择
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
        
        // 显示选中日期的情绪记录
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
                    .padding(16.dp)
            ) {
                Text(
                    text = "日期: $selectedDate",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (loadedEntry != null) {
                    val entry = loadedEntry!!
                    Text("情绪: ${entry.result}")
                    Text("关键词: ${entry.keywords.joinToString(", ")}")
                    if (entry.activity != null) {
                        Text("活动: ${entry.activity}")
                    }
                    Text("笔记: ${entry.note}")
                } else {
                    Text("该日期没有情绪记录")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 情绪颜色图例
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MoodLegend("低落", getMoodColor("sad"))
            MoodLegend("平静", getMoodColor("calm"))
            MoodLegend("一般", getMoodColor("neutral"))
            MoodLegend("开心", getMoodColor("happy"))
            MoodLegend("兴奋", getMoodColor("excited"))
        }
    }
}

@Composable
fun MoodTrendTab() {
    // 模拟过去7天的情绪数据
    val moodData = listOf(
        0.2f, 0.4f, 0.6f, 0.3f, 0.7f, 0.9f, 0.5f
    )
    val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "过去七天情绪趋势",
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
            MoodStatCard("平均情绪", "积极", Color(0xFF8BC34A))
            MoodStatCard("最佳日", "周六", Color(0xFF1DB954))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MoodStatCard("情绪波动", "稳定", Color(0xFF6B4EFF))
            MoodStatCard("建议活动", "音乐疗愈", Color(0xFF8B7AFF))
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
fun DayCell(day: Day) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(getMoodColor(day.mood)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.date.toString(),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
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

fun getMonthName(month: Int): String {
    return when (month) {
        Calendar.JANUARY -> "一月"
        Calendar.FEBRUARY -> "二月"
        Calendar.MARCH -> "三月"
        Calendar.APRIL -> "四月"
        Calendar.MAY -> "五月"
        Calendar.JUNE -> "六月"
        Calendar.JULY -> "七月"
        Calendar.AUGUST -> "八月"
        Calendar.SEPTEMBER -> "九月"
        Calendar.OCTOBER -> "十月"
        Calendar.NOVEMBER -> "十一月"
        Calendar.DECEMBER -> "十二月"
        else -> ""
    }
}

data class Day(
    val date: Int,
    val mood: String
) 