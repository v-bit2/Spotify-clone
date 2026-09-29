package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteTrackEntity::class, DownloadedTrackEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SpotificDatabase : RoomDatabase() {
    abstract fun spotificDao(): SpotificDao

    companion object {
        @Volatile
        private var INSTANCE: SpotificDatabase? = null

        fun getInstance(context: Context): SpotificDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SpotificDatabase::class.java,
                    "spotific.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
