package com.example.moodmelody.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MoodEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MoodEntry)

    @Query("SELECT * FROM mood_entries WHERE date = :date LIMIT 1")
    suspend fun getEntryByDate(date: String): MoodEntry?
    
    @Query("SELECT * FROM mood_entries WHERE date LIKE :monthPattern")
    suspend fun getEntriesForMonth(monthPattern: String): List<MoodEntry>
}