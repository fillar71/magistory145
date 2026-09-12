package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ApiKeyDao
import com.example.data.local.dao.ClipDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.TrackDao
import com.example.data.local.entity.ApiKeyEntity
import com.example.data.local.entity.ClipEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TrackEntity

@Database(
    entities = [
        ProjectEntity::class,
        TrackEntity::class,
        ClipEntity::class,
        ApiKeyEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MagistoryDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun trackDao(): TrackDao
    abstract fun clipDao(): ClipDao
    abstract fun apiKeyDao(): ApiKeyDao

    companion object {
        @Volatile
        private var INSTANCE: MagistoryDatabase? = null

        fun getInstance(context: Context): MagistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MagistoryDatabase::class.java,
                    "magistory_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
