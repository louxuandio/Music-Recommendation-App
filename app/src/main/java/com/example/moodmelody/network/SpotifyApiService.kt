package com.example.moodmelody.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SpotifyApiService {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String = "track",
        @Query("limit") limit: Int = 20
    ): Response<Map<String, Any>>

    @GET("recommendations")
    suspend fun getRecommendations(
        @Query("seed_genres") seedGenres: String,
        @Query("target_valence") targetValence: Float? = null,
        @Query("target_energy") targetEnergy: Float? = null,
        @Query("limit") limit: Int = 20
    ): Response<Map<String, Any>>
}