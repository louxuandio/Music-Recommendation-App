package com.example.moodmelody.ui.screens.test

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
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
import com.example.moodmelody.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(navController: NavController) {
    var currentStep by remember { mutableStateOf(1) }
    var moodValue by remember { mutableStateOf(0.5f) }
    var selectedKeywords by remember { mutableStateOf(listOf<String>()) }
    var selectedLyric by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("情绪检测 $currentStep/3") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 进度指示器
            StepProgressIndicator(
                steps = 3,
                currentStep = currentStep,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
            
            // 当前步骤内容
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (currentStep) {
                    1 -> MoodSliderStep(
                        moodValue = moodValue,
                        onMoodValueChange = { moodValue = it }
                    )
                    2 -> KeywordSelectionStep(
                        selectedKeywords = selectedKeywords,
                        onKeywordSelected = { keyword ->
                            selectedKeywords = if (selectedKeywords.contains(keyword)) {
                                selectedKeywords - listOf(keyword)
                            } else {
                                selectedKeywords + listOf(keyword)
                            }
                        }
                    )
                    3 -> LyricSelectionStep(
                        selectedLyric = selectedLyric,
                        onLyricSelected = { selectedLyric = it }
                    )
                }
            }
            
            // 导航按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep > 1) {
                    Button(
                        onClick = { currentStep-- },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "上一步")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("上一步")
                    }
                } else {
                    Spacer(modifier = Modifier.width(120.dp))
                }
                
                Button(
                    onClick = {
                        if (currentStep < 3) {
                            currentStep++
                        } else {
                            // 测试完成，跳转到主页
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = when (currentStep) {
                        2 -> selectedKeywords.isNotEmpty()
                        3 -> selectedLyric != null
                        else -> true
                    }
                ) {
                    Text(if (currentStep == 3) "查看结果" else "下一步")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = "下一步")
                }
            }
        }
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
