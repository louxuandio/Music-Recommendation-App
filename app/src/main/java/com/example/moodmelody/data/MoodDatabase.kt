package com.example.moodmelody.data
import android.content.Context
import androidx.room.*

@Database(entities = [MoodEntry::class], version = 1)
@TypeConverters(Converters::class)
abstract class MoodDatabase : RoomDatabase() {
    abstract fun moodEntryDao(): MoodEntryDao

    companion object {
        @Volatile private var INSTANCE: MoodDatabase? = null

        fun getDatabase(context: Context): MoodDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MoodDatabase::class.java,
                    "mood_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
