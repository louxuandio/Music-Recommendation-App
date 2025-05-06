package com.example.moodmelody.data

import com.example.moodmelody.Song

/**
 * SampleData 提供示例数据，用于开发和测试阶段
 */
object SampleData {
    // 示例歌曲列表
    val sampleSongs = listOf(
        Song(
            title = "阳光心情",
            artist = "轻音乐合集",
            coverUrl = "https://example.com/cover1.jpg",
            genre = "轻音乐"
        ),
        Song(
            title = "雨天思绪",
            artist = "钢琴曲集",
            coverUrl = "https://example.com/cover2.jpg",
            genre = "古典"
        ),
        Song(
            title = "深夜独处",
            artist = "爵士乐选",
            coverUrl = "https://example.com/cover3.jpg",
            genre = "爵士"
        ),
        Song(
            title = "晨间清醒",
            artist = "自然音乐",
            coverUrl = "https://example.com/cover4.jpg",
            genre = "环境"
        ),
        Song(
            title = "冥想放松",
            artist = "环境音效",
            coverUrl = "https://example.com/cover5.jpg",
            genre = "环境"
        )
    )
    
    // 示例心情提示
    val moodPrompts = mapOf(
        "happy" to "阳光般的积极情绪让你充满活力",
        "sad" to "感到低落是自然的，音乐可以陪伴你",
        "relaxed" to "保持平静的心态，享受宁静时刻",
        "excited" to "兴奋的情绪可以激发创造力",
        "angry" to "让音乐帮助你释放压力和情绪"
    )
    
    // 测试问题
    val testQuestions = listOf(
        "你今天的总体心情如何？",
        "有什么特别的事件影响了你的情绪吗？",
        "你现在喜欢听什么类型的音乐？"
    )
    
    // 根据心情获取推荐歌曲
    fun getRecommendationsByMood(mood: String): List<Song> {
        return when (mood) {
            "happy" -> sampleSongs.filter { it.genre == "轻音乐" || it.genre == "流行" }
            "sad" -> sampleSongs.filter { it.genre == "古典" || it.genre == "爵士" }
            "relaxed" -> sampleSongs.filter { it.genre == "环境" || it.genre == "古典" }
            "excited" -> sampleSongs.filter { it.genre == "流行" || it.genre == "电子" }
            "angry" -> sampleSongs.filter { it.genre == "摇滚" || it.genre == "电子" }
            else -> sampleSongs.shuffled().take(3)
        }
    }
    
    // 用于搜索的模拟函数
    fun searchSongs(query: String): List<Song> {
        if (query.isBlank()) return emptyList()
        
        return sampleSongs.filter { song ->
            song.title.contains(query, ignoreCase = true) ||
            song.artist.contains(query, ignoreCase = true) ||
            song.genre?.contains(query, ignoreCase = true) == true
        }
    }
} 