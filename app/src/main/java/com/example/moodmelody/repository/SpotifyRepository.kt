package com.example.moodmelody.repository

import com.example.moodmelody.Song
import com.example.moodmelody.network.SpotifyApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

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
                Result.failure(Exception("Search failed: ${response.errorBody()?.string()}"))
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
                Result.failure(Exception("Failed to get recommendations: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取新发行歌曲
     * 使用Spotify新发行榜单API获取最新发布的歌曲
     */
    suspend fun getTopTrendingSongs(limit: Int = 20): Result<List<Song>> = withContext(Dispatchers.IO) {
        try {
            // 使用新发行榜单API代替播放列表API
            val response = apiService.getNewReleases(limit = limit)
            
            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!
                val albumsData = responseBody["albums"] as? Map<String, Any> ?: mapOf()
                val items = albumsData["items"] as? List<Map<String, Any>> ?: emptyList()
                
                // 从每个专辑中提取信息创建Song对象
                val songs = items.mapNotNull { album ->
                    try {
                        val name = album["name"] as? String ?: return@mapNotNull null
                        
                        // 获取艺术家信息
                        val artists = album["artists"] as? List<Map<String, Any>> ?: emptyList()
                        val artistNames = artists.mapNotNull { it["name"] as? String }
                        val artistName = artistNames.joinToString(", ")
                        
                        // 获取封面图片
                        val images = album["images"] as? List<Map<String, Any>> ?: emptyList()
                        val coverUrl = if (images.isNotEmpty()) images[0]["url"] as? String else null
                        
                        // 获取专辑URI
                        val uri = album["uri"] as? String
                        
                        Song(
                            title = name,
                            artist = artistName,
                            coverUrl = coverUrl,
                            uri = uri,
                            previewUrl = null
                        )
                    } catch (e: Exception) {
                        Log.e("SpotifyRepository", "Error parsing album: ${e.message}")
                        null
                    }
                }
                
                Result.success(songs)
            } else {
                Result.failure(Exception("Failed to get new releases: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取推荐精选歌单
     */
    suspend fun getFeaturedPlaylists(limit: Int = 10): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getFeaturedPlaylists(limit = limit)
            
            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!
                val playlistsData = responseBody["playlists"] as? Map<String, Any>
                val items = playlistsData?.get("items") as? List<Map<String, Any>> ?: emptyList()
                
                Result.success(items)
            } else {
                Result.failure(Exception("Failed to get featured playlists: ${response.errorBody()?.string()}"))
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

    // 将情绪映射到Spotify参数
    private fun mapMoodToSpotifyParams(mood: String, intensity: Int): Triple<String, Float, Float> {
        val normalizedIntensity = intensity.coerceIn(1, 5) / 5f

        return when (mood.lowercase()) {
            "happy" -> Triple(
                "pop,happy",
                0.7f + (normalizedIntensity * 0.3f),  // 高正能量
                0.6f + (normalizedIntensity * 0.4f)   // 中高能量
            )
            "sad" -> Triple(
                "sad,acoustic",
                0.1f + (normalizedIntensity * 0.2f),  // 低正能量
                0.1f + (normalizedIntensity * 0.3f)   // 低能量
            )
            "relaxed", "calm" -> Triple(
                "chill,ambient",
                0.4f + (normalizedIntensity * 0.3f),  // 中等正能量
                0.2f + (normalizedIntensity * 0.2f)   // 低能量
            )
            "excited" -> Triple(
                "dance,electronic",
                0.6f + (normalizedIntensity * 0.4f),  // 高正能量
                0.7f + (normalizedIntensity * 0.3f)   // 高能量
            )
            "angry" -> Triple(
                "rock,metal",
                0.2f + (normalizedIntensity * 0.2f),  // 低正能量
                0.8f + (normalizedIntensity * 0.2f)   // 高能量
            )
            else -> Triple(
                "pop,rock",
                0.5f,                                 // 中等正能量
                0.5f                                  // 中等能量
            )
        }
    }
}