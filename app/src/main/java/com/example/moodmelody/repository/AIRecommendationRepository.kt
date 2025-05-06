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
            // 添加详细日志信息，帮助排查问题
            val apiKeyStart = if (openAIApiKey.length > 10) openAIApiKey.substring(0, 10) + "..." else "无效密钥"
            Log.d(TAG, "开始API调用，API密钥前缀: $apiKeyStart, 密钥长度: ${openAIApiKey.length}")
            Log.d(TAG, "用户数据: 心情分数=${userData.moodScore}, 天气=${userData.weather}")
            
            // 检查API密钥是否无效 (占位符或空值)
            if (openAIApiKey.isBlank() || openAIApiKey.startsWith("sk-your-") || openAIApiKey == "null") {
                Log.e(TAG, "OpenAI API密钥无效，长度: ${openAIApiKey.length}, 前缀: $apiKeyStart")
                // 返回实际API错误而不是模拟数据，帮助排查问题
                throw AIRecommendationException("API密钥无效或格式错误: $apiKeyStart")
            }
            
            // 构建system提示
            val systemPrompt = """
                You are a professional music recommendation assistant specializing in Western pop music. Based on the user's emotional data, generate targeted music recommendations.
                
                IMPORTANT: Be very consistent with the user's dominant mood. If they say they are "happy", describe them as having a positive, upbeat emotional state with high mood score, NOT a low score. Similarly, if their dominant mood is "sad", describe them as having a negative emotional state.
                
                Mood mapping guidelines:
                - For "happy" users: Describe them as having a high mood score and positive emotional state
                - For "sad" users: Describe them as having a low mood score and negative emotional state
                - For "relaxed" users: Describe them as having a moderate-high mood score and calm emotional state
                - For "excited" users: Describe them as having a high mood score and energetic emotional state
                
                Always recommend songs that truly reflect the user's current emotional state:
                - For sad moods: Recommend melancholic, emotional songs that resonate with sadness (like Adele, Lewis Capaldi, Sam Smith)
                - For relaxed moods: Recommend calm, soothing music that enhances the relaxed state (like Coldplay's slower songs, Jack Johnson)
                - For happy moods: Recommend upbeat, energetic songs that celebrate happiness
                - For excited moods: Recommend high-energy, dynamic music
                
                Return a JSON format reply with the following fields:
                1. summary: A brief analysis of the user's current emotional state and the reason for music recommendations
                2. suggestedSongs: An array of 5 songs, in the format "Song Name - Artist"
                
                Only return JSON format, do not include any other text. Make sure the returned JSON is valid.
            """.trimIndent()
            
            // 构建user提示
            val userPrompt = """
                Based on the following data, recommend English pop music:
                - Mood score: ${userData.moodScore}/100 (higher indicates more positive emotion)
                - Dominant mood: ${userData.dominantMood.ifEmpty { "Not specified" }}
                - Keywords: ${userData.keywords.joinToString(", ")}
                - User's favorite lyrics: "${userData.lyric}"
                - Current weather: ${userData.weather}
                
                IMPORTANT: The dominant mood "${userData.dominantMood}" takes priority over the raw mood score. My emotional state is "${userData.dominantMood}".
                
                My current mood is ${userData.dominantMood.ifEmpty { "reflected by my mood score" }}, and I want music that truly matches this emotional state.
                Please recommend popular Western music that resonates with my current emotional state.
                
                If my dominant mood is "happy", recommend upbeat, joyful songs.
                If my dominant mood is "sad", recommend emotionally resonant, melancholic songs.
                If my dominant mood is "relaxed", recommend calm, soothing music.
                If my dominant mood is "excited", recommend high-energy, dynamic music.
                
                Please provide songs that actually match my mood of ${userData.dominantMood.ifEmpty { "based on my mood score" }}.
                Do not try to artificially cheer me up if I'm feeling sad or down.
                
                Please consider these factors and recommend exclusively popular Western music (mainly American and British pop/rock) that authentically reflects my current mood. Return only JSON format.
            """.trimIndent()
            
            // 构建OpenAI请求
            val openAIRequest = OpenAIRequest(
                model = "gpt-4o",  // 更换为GPT-4.1模型
                messages = listOf(
                    OpenAIMessage(role = "system", content = systemPrompt),
                    OpenAIMessage(role = "user", content = userPrompt)
                ),
                temperature = 0.7,
                max_tokens = 500
            )
            
            try {
                // 调用OpenAI API
                Log.d(TAG, "开始发送API请求...")
                val response = openAIApiService.createChatCompletion(
                    openAIRequest = openAIRequest
                )
                
                Log.d(TAG, "API响应状态码: ${response.code()}")
                if (response.isSuccessful) {
                    val openAIResponse = response.body()
                    if (openAIResponse != null && openAIResponse.choices.isNotEmpty()) {
                        // 提取AI回复的内容
                        val aiContent = openAIResponse.choices[0].message.content.trim()
                        Log.d(TAG, "API调用成功! AI回复长度: ${aiContent.length}")
                        Log.d(TAG, "AI原始回复前50字符: ${aiContent.take(50)}...")
                        
                        // 尝试从AI回复中解析JSON
                        try {
                            return@withContext extractRecommendationFromJson(aiContent)
                        } catch (e: JsonSyntaxException) {
                            Log.e(TAG, "解析AI回复JSON时出错: ${e.message}")
                            throw AIRecommendationException("无法解析AI回复: ${e.message}")
                        }
                    } else {
                        Log.e(TAG, "OpenAI API返回了空响应")
                        throw AIRecommendationException("OpenAI API返回了空响应")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "未知错误"
                    Log.e(TAG, "OpenAI API错误: 状态码=${response.code()}, 错误信息=$errorBody")
                    throw AIRecommendationException("API调用失败: 状态码=${response.code()}, 错误=$errorBody")
                }
            } catch (e: Exception) {
                Log.e(TAG, "OpenAI API调用异常: ${e.message}", e)
                throw AIRecommendationException("API通信异常: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "推荐过程中发生错误: ${e.message}", e)
            throw e
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
     * Get mock recommendations based on user data
     */
    private fun getMockRecommendation(userData: UserData): Recommendation {
        val moodScore = userData.moodScore
        val weather = userData.weather
        val matchMood = userData.matchMood
        
        // Determine emotional state based on mood score
        val mood = when {
            moodScore >= 80 -> "happy"
            moodScore >= 60 -> "calm"
            moodScore >= 40 -> "neutral"
            moodScore >= 20 -> "melancholic"
            else -> "sad"
        }
        
        // Match mood strategy - provide songs matching the current mood
        if (matchMood) {
            return when {
                // 悲伤心情 - 提供悲伤的歌曲
                moodScore < 40 -> {
                    Recommendation(
                        summary = "Your emotional state appears to be melancholic or sad. I've selected songs that resonate with these feelings, helping you process and reflect on your emotions through music that acknowledges sadness.",
                        suggestedSongs = listOf(
                            "Someone Like You - Adele",
                            "All I Want - Kodaline",
                            "Fix You - Coldplay",
                            "Skinny Love - Bon Iver",
                            "When The Party's Over - Billie Eilish"
                        )
                    )
                }
                // 平静心情 - 提供平静的歌曲
                mood == "calm" -> {
                    Recommendation(
                        summary = "Your mood reflects a calm and relaxed state. I've selected songs with soothing melodies and gentle rhythms that complement and enhance your current peaceful emotional state.",
                        suggestedSongs = listOf(
                            "Photograph - Ed Sheeran",
                            "Yellow - Coldplay",
                            "Vienna - Billy Joel",
                            "The Night We Met - Lord Huron",
                            "Saturn - Sleeping At Last"
                        )
                    )
                }
                // 高兴心情 - 提供欢快的歌曲
                moodScore > 70 -> {
                    Recommendation(
                        summary = "Your current emotional state appears quite positive and upbeat! I've selected energetic and joyful songs that match and amplify your happy mood.",
                        suggestedSongs = listOf(
                            "Happy - Pharrell Williams",
                            "Can't Stop the Feeling! - Justin Timberlake",
                            "Good As Hell - Lizzo",
                            "Walking on Sunshine - Katrina & The Waves",
                            "Uptown Funk - Mark Ronson ft. Bruno Mars"
                        )
                    )
                }
                // 默认推荐
                else -> {
                    Recommendation(
                        summary = "Based on your mood, I've selected a balanced mix of songs that should complement your current emotional state without pushing too far in any direction.",
                        suggestedSongs = listOf(
                            "Viva La Vida - Coldplay",
                            "Circles - Post Malone",
                            "Thinking Out Loud - Ed Sheeran",
                            "Dreams - Fleetwood Mac",
                            "Watermelon Sugar - Harry Styles"
                        )
                    )
                }
            }
        }
        // Uplift mood strategy - always provide positive/uplifting songs
        else {
            return when {
                // 悲伤心情 - 提供振奋的歌曲
                moodScore < 40 -> {
                    Recommendation(
                        summary = "I notice your emotional state appears to be on the lower side. I've selected uplifting and energetic songs specifically designed to boost your mood and bring some positive energy.",
                        suggestedSongs = listOf(
                            "Good as Hell - Lizzo",
                            "Can't Stop the Feeling! - Justin Timberlake",
                            "Happy - Pharrell Williams",
                            "Walking on Sunshine - Katrina & The Waves",
                            "Don't Stop Me Now - Queen"
                        )
                    )
                }
                // 雨天 - 提供温暖的歌曲
                weather.contains("rain", ignoreCase = true) -> {
                    Recommendation(
                        summary = "Given the rainy weather and your current mood, I've selected songs with warm, uplifting melodies to brighten your day and counterbalance the gloomy atmosphere outside.",
                        suggestedSongs = listOf(
                            "Here Comes the Sun - The Beatles",
                            "Three Little Birds - Bob Marley",
                            "Mr. Blue Sky - Electric Light Orchestra",
                            "Somewhere Over the Rainbow - Israel Kamakawiwo'ole",
                            "I Can See Clearly Now - Johnny Nash"
                        )
                    )
                }
                // 默认提升心情的推荐
                else -> {
                    Recommendation(
                        summary = "I've selected a mix of uplifting and positive songs to enhance your current mood and provide an energetic soundtrack for your day.",
                        suggestedSongs = listOf(
                            "Blinding Lights - The Weeknd",
                            "Levitating - Dua Lipa",
                            "Dynamite - BTS",
                            "Good Feeling - Flo Rida",
                            "Shake It Off - Taylor Swift"
                        )
                    )
                }
            }
        }
    }
} 