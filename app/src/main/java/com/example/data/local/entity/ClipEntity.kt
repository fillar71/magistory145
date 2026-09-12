package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timeline_clips",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["trackId"])]
)
data class ClipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackId: Long,
    val sourceUri: String,
    val thumbnailUri: String? = null,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 5000L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 5000L,
    val label: String? = null,
    val volume: Float = 1.0f,
    val transition: String? = "cut", // "cut", "crossfade", "fade_black", "wipe_left", "zoom_in"
    val keyframesJson: String? = null
) {
    val durationMs: Long get() = (endTimeMs - startTimeMs).coerceAtLeast(0L)
}
