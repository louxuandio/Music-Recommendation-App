package com.example.moodmelody.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SpotifyApiService {
    @GET("v1/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String = "track",
        @Query("limit") limit: Int = 10
    ): Response<Map<String, Any>>

    @GET("v1/recommendations")
    suspend fun getRecommendations(
        @Query("seed_genres") seedGenres: String,
        @Query("target_valence") targetValence: Float,
        @Query("target_energy") targetEnergy: Float,
        @Query("limit") limit: Int = 20
    ): Response<Map<String, Any>>
    
    @GET("v1/browse/featured-playlists")
    suspend fun getFeaturedPlaylists(
        @Query("limit") limit: Int = 10
    ): Response<Map<String, Any>>
    
    @GET("v1/browse/new-releases")
    suspend fun getNewReleases(
        @Query("limit") limit: Int = 20
    ): Response<Map<String, Any>>
    
    @GET("playlists/{playlist_id}/tracks")
    suspend fun getPlaylistTracks(
        @Path("playlist_id") playlistId: String,
        @Query("limit") limit: Int = 20
    ): Response<Map<String, Any>>
    
    // 获取全球热门50首歌曲的固定播放列表ID
    companion object {
        const val GLOBAL_TOP_50_PLAYLIST_ID = "37i9dQZEVXbMDoHDwVN2tF"
    }
}