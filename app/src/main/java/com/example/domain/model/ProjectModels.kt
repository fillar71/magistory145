package com.example.domain.model

data class Project(
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

enum class TrackType {
    VIDEO,
    AUDIO,
    TEXT
}

data class TimelineTrack(
    val id: Long = 0,
    val projectId: Long,
    val type: String, // "VIDEO", "AUDIO", "TEXT"
    val orderIndex: Int,
    val clips: List<TimelineClip> = emptyList()
)

data class TimelineClip(
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
    val transition: String? = "cut",
    val keyframesJson: String? = null
) {
    val durationMs: Long get() = (endTimeMs - startTimeMs).coerceAtLeast(0L)
    val keyframes: List<ClipKeyframe> get() = KeyframeSerializer.deserialize(keyframesJson)
}
