package com.example.moodmelody.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mood_entries")
data class MoodEntry(
    @PrimaryKey val date: String, // e.g., "2025-04-15"
    val moodIndex: Float,
    val keywords: List<String>,
    val activity: String?,
    val note: String
)