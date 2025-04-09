package com.example.moodmelody.repository

import com.example.moodmelody.Song
import com.example.moodmelody.network.SpotifyApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SpotifyRepository(private val apiService: SpotifyApiService) {

    suspend fun searchMusic(query: String): Result<List<Song>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.search(query)
            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!
                val tracksData = responseBody["tracks"] as? Map<String, Any>
                val items = tracksData?.get("items") as? List<Map<String, Any>> ?: emptyList()

                val songs = items.map { trackData ->
                    parseSongFromTrackData(trackData)
                }

                Result.success(songs)
            } else {
                Result.failure(Exception("搜索失败: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecommendations(mood: String, intensity: Int): Result<List<Song>> = withContext(Dispatchers.IO) {
        try {
            // 将情绪映射到Spotify参数
            val (genres, valence, energy) = mapMoodToSpotifyParams(mood, intensity)

            val response = apiService.getRecommendations(
                seedGenres = genres,
                targetValence = valence,
                targetEnergy = energy
            )

            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!
                val tracks = responseBody["tracks"] as? List<Map<String, Any>> ?: emptyList()

                val songs = tracks.map { trackData ->
                    parseSongFromTrackData(trackData)
                }

                Result.success(songs)
            } else {
                Result.failure(Exception("获取推荐失败: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 解析Spotify Track数据为Song对象
    private fun parseSongFromTrackData(trackData: Map<String, Any>): Song {
        val name = trackData["name"] as? String ?: ""
        val uri = trackData["uri"] as? String
        val previewUrl = trackData["preview_url"] as? String

        val artists = trackData["artists"] as? List<Map<String, Any>> ?: emptyList()
        val artistNames = artists.mapNotNull { it["name"] as? String }

        val album = trackData["album"] as? Map<String, Any>
        val images = album?.get("images") as? List<Map<String, Any>> ?: emptyList()
        val coverUrl = if (images.isNotEmpty()) images[0]["url"] as? String else null

        return Song(
            title = name,
            artist = artistNames.joinToString(", "),
            coverUrl = coverUrl,
            uri = uri,
            previewUrl = previewUrl
        )
    }

    // 将情绪映射到Spotify推荐参数 by Claude 但目前跟天气没挂钩
    private fun mapMoodToSpotifyParams(mood: String, intensity: Int): Triple<String, Float?, Float?> {
        return when (mood.lowercase()) {
            "happy" -> {
                val valence = 0.7f + (intensity - 3) * 0.05f // 高正面情绪
                val energy = 0.6f + (intensity - 3) * 0.05f  // 中高能量
                Triple("pop,happy", valence, energy)
            }
            "sad" -> {
                val valence = 0.3f - (intensity - 3) * 0.05f // 低正面情绪
                val energy = 0.4f - (intensity - 3) * 0.05f  // 低能量
                Triple("sad,acoustic", valence, energy)
            }
            "calm" -> {
                val valence = 0.5f                           // 中等正面情绪
                val energy = 0.3f - (intensity - 3) * 0.05f  // 非常低能量
                Triple("ambient,classical", valence, energy)
            }
            "angry" -> {
                val valence = 0.4f - (intensity - 3) * 0.05f // 低中等正面情绪
                val energy = 0.8f + (intensity - 3) * 0.05f  // 高能量
                Triple("rock,metal", valence, energy)
            }
            "anxious" -> {
                val valence = 0.4f                          // 低中等正面情绪
                val energy = 0.5f - (intensity - 3) * 0.05f // 中等能量
                Triple("indie,electronic", valence, energy)
            }
            "tired" -> {
                val valence = 0.5f                          // 中等正面情绪
                val energy = 0.2f - (intensity - 3) * 0.05f // 非常低能量
                Triple("sleep,ambient", valence, energy)
            }
            else -> {
                Triple("pop,rock,electronic", 0.5f, 0.5f)   // 默认参数
            }
        }
    }
}