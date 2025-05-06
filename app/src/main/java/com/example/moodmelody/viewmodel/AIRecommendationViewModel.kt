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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * AI Music Recommendation ViewModel
 */
class AIRecommendationViewModel(
    private val aiRecommendationRepository: AIRecommendationRepository = AIRecommendationRepository()
) : ViewModel() {
    
    private val TAG = "AIRecommendationViewModel"
    
    // Recommendation results
    private val _recommendation = MutableStateFlow<Recommendation?>(null)
    val recommendation: StateFlow<Recommendation?> = _recommendation.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Error message
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    /**
     * Get AI music recommendations
     * @param moodScore mood score (0-100)
     * @param keywords list of keywords
     * @param lyric favorite lyrics
     * @param weather current weather
     */
    fun getRecommendation(
        moodScore: Float,
        keywords: List<String>,
        lyric: String,
        weather: String
    ) {
        // Create user data object
        val userData = UserData(
            moodScore = moodScore,
            keywords = keywords,
            lyric = lyric,
            weather = weather,
            matchMood = true
        )
        
        // Call the recommendation method
        getRecommendation(userData)
    }
    
    /**
     * Get AI music recommendations
     * @param userData user data
     */
    private fun getRecommendation(userData: UserData) {
        viewModelScope.launch {
            // Reset error state
            _errorMessage.value = null
            // Set loading state
            _isLoading.value = true
            
            try {
                // Call repository method to get recommendations
                val result = aiRecommendationRepository.recommendWithOpenAI(userData)
                
                // Update recommendation results
                _recommendation.value = result
                _isLoading.value = false
            } catch (e: AIRecommendationException) {
                Log.e(TAG, "Failed to get AI recommendation: ${e.message}", e)
                _errorMessage.value = e.message
                _recommendation.value = null
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get AI recommendation: ${e.message}", e)
                _errorMessage.value = e.message
                _recommendation.value = null
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Clear current recommendation results
     */
    fun clearRecommendation() {
        _recommendation.value = null
        _errorMessage.value = null
    }
} 