package com.example.moodmelody.repository

import android.util.Log
import com.example.moodmelody.model.*
import com.example.moodmelody.network.OpenAIApiService
import com.example.moodmelody.network.RetrofitClient
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AI音乐推荐仓库，负责与OpenAI API交互
 */
class AIRecommendationRepository(
    private val openAIApiService: OpenAIApiService = RetrofitClient.openAIApiService,
    private val openAIApiKey: String = RetrofitClient.openAIApiKey
) {
    private val TAG = "AIRecommendRepository"
    private val gson = Gson()
    
    /**
     * 根据用户数据获取音乐推荐
     * @param userData 用户情感和环境数据
     * @return 推荐结果
     * @throws AIRecommendationException 如果API调用失败或解析响应时出错
     */
    suspend fun recommendWithOpenAI(userData: UserData): Recommendation = withContext(Dispatchers.IO) {
        try {
            // 检查API密钥是否无效 (占位符或空值)
            if (openAIApiKey.isBlank() || openAIApiKey.startsWith("sk-your-") || openAIApiKey == "null") {
                Log.w(TAG, "OpenAI API密钥无效，使用模拟数据")
                return@withContext getMockRecommendation(userData)
            }
            
            // 构建system提示
            val systemPrompt = """
                你是一个专业的音乐推荐助手。基于用户提供的情感数据，请生成音乐推荐。
                你需要返回一个JSON格式的回复，包含以下字段：
                1. summary: 对用户当前情感状态的简短分析和音乐推荐理由
                2. suggestedSongs: 一个包含3-5首歌曲的数组，格式为"歌名 - 艺术家"
                
                只返回JSON格式，不要包含任何其他文本。确保返回的是有效的JSON。
            """.trimIndent()
            
            // 构建user提示
            val userPrompt = """
                基于以下数据为我推荐英文流行音乐或纯音乐：
                - 情感得分: ${userData.moodScore}/100（越高表示情绪越积极）
                - 关键词: ${userData.keywords.joinToString(", ")}
                - 用户喜欢的歌词: "${userData.lyric}"
                - 当前天气: ${userData.weather}
                
                请考虑这些因素，推荐最适合我当前情绪的音乐。仅返回JSON格式。
            """.trimIndent()
            
            // 构建OpenAI请求
            val openAIRequest = OpenAIRequest(
                model = "gpt-3.5-turbo",  // 改用更广泛支持的模型
                messages = listOf(
                    OpenAIMessage(role = "system", content = systemPrompt),
                    OpenAIMessage(role = "user", content = userPrompt)
                ),
                temperature = 0.7,
                max_tokens = 500
            )
            
            try {
                // 调用OpenAI API
                val response = openAIApiService.createChatCompletion(
                    authorization = "Bearer $openAIApiKey",
                    openAIRequest = openAIRequest
                )
                
                if (response.isSuccessful) {
                    val openAIResponse = response.body()
                    if (openAIResponse != null && openAIResponse.choices.isNotEmpty()) {
                        // 提取AI回复的内容
                        val aiContent = openAIResponse.choices[0].message.content.trim()
                        Log.d(TAG, "AI原始回复: $aiContent")
                        
                        // 尝试从AI回复中解析JSON
                        try {
                            return@withContext extractRecommendationFromJson(aiContent)
                        } catch (e: JsonSyntaxException) {
                            Log.e(TAG, "解析AI回复JSON时出错: ${e.message}")
                            return@withContext getMockRecommendation(userData)
                        }
                    } else {
                        Log.e(TAG, "OpenAI API返回了空响应")
                        return@withContext getMockRecommendation(userData)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "未知错误"
                    Log.e(TAG, "OpenAI API错误: $errorBody")
                    return@withContext getMockRecommendation(userData)
                }
            } catch (e: Exception) {
                Log.e(TAG, "OpenAI API调用异常: ${e.message}", e)
                return@withContext getMockRecommendation(userData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "推荐过程中发生错误: ${e.message}", e)
            return@withContext getMockRecommendation(userData)
        }
    }
    
    /**
     * 从AI回复中提取推荐信息
     */
    private fun extractRecommendationFromJson(jsonContent: String): Recommendation {
        // 处理可能的格式问题，如JSON前后可能有```json和```标记
        val cleanJson = jsonContent
            .replace("```json", "")
            .replace("```", "")
            .trim()
        
        // 解析JSON
        return try {
            gson.fromJson(cleanJson, Recommendation::class.java)
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "JSON解析错误: $cleanJson", e)
            throw AIRecommendationException("无法解析AI回复", e)
        }
    }
    
    /**
     * 获取基于用户数据的模拟推荐
     */
    private fun getMockRecommendation(userData: UserData): Recommendation {
        val moodScore = userData.moodScore
        val weather = userData.weather
        
        // 基于心情分数确定情感状态
        val mood = when {
            moodScore >= 80 -> "愉快的"
            moodScore >= 60 -> "平静的"
            moodScore >= 40 -> "中性的"
            moodScore >= 20 -> "忧伤的"
            else -> "低落的"
        }
        
        // 基于天气和心情提供不同的推荐
        return when {
            // 雨天+低落情绪
            weather.contains("雨") && moodScore < 40 -> {
                Recommendation(
                    summary = "您似乎正处于" + mood + "情绪，加上今天是雨天，我为您推荐一些舒缓、略带忧伤的歌曲，帮助您沉淀情感。",
                    suggestedSongs = listOf(
                        "Raindrops - Chopin",
                        "The Sound of Silence - Simon & Garfunkel",
                        "Someone Like You - Adele",
                        "Fix You - Coldplay",
                        "I Will Remember You - Sarah McLachlan"
                    )
                )
            }
            // 晴天+愉快情绪
            weather.contains("晴") && moodScore > 60 -> {
                Recommendation(
                    summary = "阳光明媚的日子配上您" + mood + "的心情，适合一些欢快、充满活力的音乐！",
                    suggestedSongs = listOf(
                        "Happy - Pharrell Williams",
                        "Walking On Sunshine - Katrina & The Waves",
                        "Good Feeling - Flo Rida",
                        "Can't Stop the Feeling! - Justin Timberlake",
                        "Uptown Funk - Mark Ronson ft. Bruno Mars"
                    )
                )
            }
            // 阴天+中性情绪
            weather.contains("阴") || weather.contains("多云") -> {
                Recommendation(
                    summary = "今天天气较为阴沉，配合您" + mood + "的心情，为您推荐一些温和但积极向上的歌曲。",
                    suggestedSongs = listOf(
                        "Bitter Sweet Symphony - The Verve",
                        "Clocks - Coldplay",
                        "Shallow - Lady Gaga & Bradley Cooper",
                        "Let It Be - The Beatles",
                        "The Scientist - Coldplay"
                    )
                )
            }
            // 低分情绪
            moodScore < 30 -> {
                Recommendation(
                    summary = "您的心情似乎有些低落，为您推荐一些治愈系音乐，希望能够抚慰您的心灵。",
                    suggestedSongs = listOf(
                        "Breathe Me - Sia",
                        "Skinny Love - Bon Iver",
                        "All I Want - Kodaline",
                        "Let Her Go - Passenger",
                        "Say Something - A Great Big World"
                    )
                )
            }
            // 高分情绪
            moodScore > 70 -> {
                Recommendation(
                    summary = "您的心情非常不错！为您推荐一些欢快、充满活力的音乐来保持这种积极的情绪。",
                    suggestedSongs = listOf(
                        "Can't Hold Us - Macklemore & Ryan Lewis",
                        "Don't Stop Me Now - Queen",
                        "Blinding Lights - The Weeknd",
                        "Dynamite - BTS",
                        "Shake It Off - Taylor Swift"
                    )
                )
            }
            // 默认推荐
            else -> {
                Recommendation(
                    summary = "基于您的心情分析，为您推荐一些平衡、舒缓的音乐，希望能够契合您当前的情绪状态。",
                    suggestedSongs = listOf(
                        "Lose Yourself - Eminem",
                        "The Middle - Zedd",
                        "Shape of You - Ed Sheeran",
                        "Don't Stop Believin' - Journey",
                        "Radioactive - Imagine Dragons"
                    )
                )
            }
        }
    }
} 