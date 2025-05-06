package com.example.moodmelody.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mood_entries")
data class MoodEntry(
    @PrimaryKey val date: String, // e.g., "2025-04-15"
    val calm: Float,
    val excited: Float,
    val happy: Float,
    val sad: Float,
    val result: String,
    val keywords: List<String>,
    val activity: String?,
    val note: String
)