package com.example

import com.example.data.local.entity.ClipEntity
import com.example.data.local.entity.ProjectEntity
import com.example.domain.model.LlmClipDto
import com.example.domain.model.LlmTimelineDto
import com.example.domain.model.LlmTrackDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MagistoryUnitTest {

    @Test
    fun testClipEntityDurationCalculation() {
        val clip = ClipEntity(
            id = 1L,
            trackId = 10L,
            sourceUri = "pexels:cinematic_sunset",
            startTimeMs = 2000L,
            endTimeMs = 7500L,
            label = "Sunset B-roll"
        )
        assertEquals(5500L, clip.durationMs)
    }

    @Test
    fun testProjectEntityDefaults() {
        val project = ProjectEntity(
            title = "Tokyo Neon Night",
            aspectRatio = "9:16",
            resolution = "4K",
            durationMs = 15000L,
            createdAt = 1000L,
            updatedAt = 2000L
        )
        assertEquals("Tokyo Neon Night", project.title)
        assertEquals("9:16", project.aspectRatio)
        assertEquals("4K", project.resolution)
    }

    @Test
    fun testLlmTimelineDtoStructure() {
        val dto = LlmTimelineDto(
            title = "Cyberpunk Reel",
            description = "High energy sci-fi reel",
            tracks = listOf(
                LlmTrackDto(
                    type = "VIDEO",
                    orderIndex = 0,
                    clips = listOf(
                        LlmClipDto("tokyo rain", "pexels", 4000L)
                    )
                ),
                LlmTrackDto(
                    type = "AUDIO",
                    orderIndex = 1,
                    clips = listOf(
                        LlmClipDto("synthwave beat", "freesound", 12000L, volume = 0.9f)
                    )
                )
            )
        )
        assertEquals("Cyberpunk Reel", dto.title)
        assertEquals(2, dto.tracks.size)
        assertEquals("VIDEO", dto.tracks[0].type)
        assertEquals("AUDIO", dto.tracks[1].type)
    }
}
