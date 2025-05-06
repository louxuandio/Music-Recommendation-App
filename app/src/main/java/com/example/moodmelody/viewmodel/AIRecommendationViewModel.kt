package com.example.moodmelody.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodmelody.model.AIRecommendationException
import com.example.moodmelody.model.Recommendation
import com.example.moodmelody.model.UserData
import com.example.moodmelody.repository.AIRecommendationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * AI音乐推荐ViewModel
 */
class AIRecommendationViewModel(
    private val aiRecommendationRepository: AIRecommendationRepository = AIRecommendationRepository()
) : ViewModel() {
    
    private val TAG = "AIRecommendViewModel"
    
    // 推荐结果
    private val _recommendation = MutableStateFlow<Recommendation?>(null)
    val recommendation: StateFlow<Recommendation?> = _recommendation
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    /**
     * 获取AI音乐推荐
     * @param moodScore 心情得分 (0-100)
     * @param keywords 关键词列表
     * @param lyric 喜欢的歌词
     * @param weather 当前天气
     */
    fun getRecommendation(
        moodScore: Float,
        keywords: List<String>,
        lyric: String,
        weather: String
    ) {
        // 创建用户数据对象
        val userData = UserData(
            moodScore = moodScore,
            keywords = keywords,
            lyric = lyric,
            weather = weather
        )
        
        // 调用获取推荐方法
        getRecommendation(userData)
    }
    
    /**
     * 获取AI音乐推荐
     * @param userData 用户数据
     */
    fun getRecommendation(userData: UserData) {
        viewModelScope.launch {
            try {
                // 重置错误状态
                _error.value = null
                // 设置加载状态
                _isLoading.value = true
                
                // 调用仓库方法获取推荐
                val result = aiRecommendationRepository.recommendWithOpenAI(userData)
                
                // 更新推荐结果
                _recommendation.value = result
                
            } catch (e: AIRecommendationException) {
                Log.e(TAG, "获取AI推荐失败: ${e.message}", e)
                _error.value = e.message
                _recommendation.value = null
            } catch (e: Exception) {
                Log.e(TAG, "获取AI推荐发生未知错误: ${e.message}", e)
                _error.value = "获取推荐失败: ${e.message}"
                _recommendation.value = null
            } finally {
                // 结束加载状态
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 清除当前推荐结果
     */
    fun clearRecommendation() {
        _recommendation.value = null
        _error.value = null
    }
} 