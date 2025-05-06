package com.example.moodmelody.network

import com.example.moodmelody.model.OpenAIRequest
import com.example.moodmelody.model.OpenAIResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * OpenAI API接口定义
 */
interface OpenAIApiService {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Body openAIRequest: OpenAIRequest
    ): Response<OpenAIResponse>
} 