package com.example.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LlmTimelineDto(
    @Json(name = "title") val title: String = "Untitled AI Project",
    @Json(name = "description") val description: String? = null,
    @Json(name = "tracks") val tracks: List<LlmTrackDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class LlmTrackDto(
    @Json(name = "type") val type: String = "VIDEO", // VIDEO, AUDIO, TEXT
    @Json(name = "orderIndex") val orderIndex: Int = 0,
    @Json(name = "clips") val clips: List<LlmClipDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class LlmClipDto(
    @Json(name = "searchQuery") val searchQuery: String? = null,
    @Json(name = "source") val source: String? = "pexels", // pexels, pixabay, freesound
    @Json(name = "durationMs") val durationMs: Long = 4000L,
    @Json(name = "startTimeMs") val startTimeMs: Long = 0L,
    @Json(name = "transition") val transition: String? = "cut",
    @Json(name = "volume") val volume: Float? = 1.0f,
    @Json(name = "label") val label: String? = null,
    @Json(name = "style") val style: String? = null
)
