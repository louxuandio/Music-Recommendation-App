package com.example.moodmelody.model

/**
 * AI推荐结果
 */
data class Recommendation(
    val summary: String,               // GPT-4.1 返回的摘要
    val suggestedSongs: List<String>   // 3~5 首歌曲，如 "Someone Like You - Adele"
)

/**
 * 自定义异常：AI推荐失败
 */
class AIRecommendationException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * OpenAI请求消息
 */
data class OpenAIMessage(
    val role: String,
    val content: String
)

/**
 * OpenAI请求体
 */
data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 500,
    val top_p: Double = 1.0,
    val frequency_penalty: Double = 0.0,
    val presence_penalty: Double = 0.0
)

/**
 * OpenAI响应体中的选择
 */
data class OpenAIChoice(
    val index: Int,
    val message: OpenAIMessage,
    val finish_reason: String
)

/**
 * OpenAI响应体
 */
data class OpenAIResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<OpenAIChoice>,
    val usage: OpenAIUsage
)

/**
 * OpenAI响应体中的使用统计
 */
data class OpenAIUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
) 