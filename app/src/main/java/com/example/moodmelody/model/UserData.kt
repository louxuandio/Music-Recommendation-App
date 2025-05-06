package com.example.moodmelody.model

/**
 * 用户数据类，包含用于生成推荐的用户情感和环境数据
 */
data class UserData(
    val moodScore: Float,
    val keywords: List<String>,
    val lyric: String,
    val weather: String,
    val matchMood: Boolean = true,  // 是否匹配用户当前情绪（而不是尝试提升情绪）
    val dominantMood: String = ""   // 从数据库读取的主导情绪类型
) 