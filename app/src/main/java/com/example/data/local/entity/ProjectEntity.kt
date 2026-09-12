package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null,
    val resolution: String = "1080p", // "1080p" or "4K"
    val aspectRatio: String = "16:9", // "16:9", "9:16", "1:1"
    val promptText: String? = null,
    val durationMs: Long = 0L
)
