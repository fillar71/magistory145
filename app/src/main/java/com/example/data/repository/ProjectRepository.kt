package com.example.data.repository

import com.example.data.local.dao.ClipDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.TrackDao
import com.example.data.local.entity.ClipEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TrackEntity
import com.example.domain.model.Project
import com.example.domain.model.TimelineClip
import com.example.domain.model.TimelineTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class ProjectDetail(
    val project: Project,
    val tracks: List<TimelineTrack>
) {
    val totalDurationMs: Long
        get() {
            var maxEnd = 0L
            tracks.forEach { track ->
                track.clips.forEach { clip ->
                    if (clip.endTimeMs > maxEnd) maxEnd = clip.endTimeMs
                }
            }
            return if (maxEnd > 0) maxEnd else project.durationMs
        }
}

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val trackDao: TrackDao,
    private val clipDao: ClipDao
) {

    val allProjects: Flow<List<Project>> = projectDao.getAllProjects().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun ensureInitialSeedData() = withContext(Dispatchers.IO) {
        if (projectDao.getProjectCount() == 0) {
            seedInitialProjects()
        }
    }

    fun getProjectDetail(projectId: Long): Flow<ProjectDetail?> {
        val projectFlow = projectDao.getProjectById(projectId)
        val tracksFlow = trackDao.getTracksForProject(projectId)
        val clipsFlow = clipDao.getAllClipsForProject(projectId)

        return combine(projectFlow, tracksFlow, clipsFlow) { projectEntity, trackEntities, clipEntities ->
            if (projectEntity == null) return@combine null

            val clipMap = clipEntities.groupBy { it.trackId }
            val tracks = trackEntities.map { trackEntity ->
                val trackClips = clipMap[trackEntity.id] ?: emptyList()
                TimelineTrack(
                    id = trackEntity.id,
                    projectId = trackEntity.projectId,
                    type = trackEntity.type,
                    orderIndex = trackEntity.orderIndex,
                    clips = trackClips.map { it.toDomain() }
                )
            }

            ProjectDetail(
                project = projectEntity.toDomain(),
                tracks = tracks
            )
        }
    }

    suspend fun getProjectDirect(projectId: Long): Project? = withContext(Dispatchers.IO) {
        projectDao.getProjectDirect(projectId)?.toDomain()
    }

    suspend fun createProject(
        title: String,
        aspectRatio: String = "16:9",
        resolution: String = "1080p",
        promptText: String? = null,
        thumbnailPath: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val entity = ProjectEntity(
            title = title,
            aspectRatio = aspectRatio,
            resolution = resolution,
            promptText = promptText,
            thumbnailPath = thumbnailPath,
            durationMs = 15000L,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val projectId = projectDao.insertProject(entity)

        // Create standard default 3 tracks: VIDEO (0), AUDIO (1), TEXT (2)
        val videoTrackId = trackDao.insertTrack(TrackEntity(projectId = projectId, type = "VIDEO", orderIndex = 0))
        val audioTrackId = trackDao.insertTrack(TrackEntity(projectId = projectId, type = "AUDIO", orderIndex = 1))
        val textTrackId = trackDao.insertTrack(TrackEntity(projectId = projectId, type = "TEXT", orderIndex = 2))

        projectId
    }

    suspend fun updateProject(project: Project) = withContext(Dispatchers.IO) {
        projectDao.updateProject(project.toEntity())
    }

    suspend fun deleteProject(projectId: Long) = withContext(Dispatchers.IO) {
        projectDao.deleteProjectById(projectId)
    }

    suspend fun updateClip(clip: TimelineClip) = withContext(Dispatchers.IO) {
        clipDao.updateClip(clip.toEntity())
        touchProjectTimestampByTrack(clip.trackId)
    }

    suspend fun addClip(trackId: Long, clip: TimelineClip): Long = withContext(Dispatchers.IO) {
        val id = clipDao.insertClip(clip.copy(trackId = trackId).toEntity())
        touchProjectTimestampByTrack(trackId)
        id
    }

    suspend fun deleteClip(clipId: Long) = withContext(Dispatchers.IO) {
        val clip = clipDao.getClipById(clipId)
        clipDao.deleteClipById(clipId)
        clip?.let { touchProjectTimestampByTrack(it.trackId) }
    }

    suspend fun splitClipAtPlayhead(clipId: Long, splitTimeMs: Long): Boolean = withContext(Dispatchers.IO) {
        val clip = clipDao.getClipById(clipId) ?: return@withContext false
        if (splitTimeMs <= clip.startTimeMs || splitTimeMs >= clip.endTimeMs) return@withContext false

        val originalEnd = clip.endTimeMs
        val trimOffset = splitTimeMs - clip.startTimeMs

        // Update first half
        clipDao.updateClip(clip.copy(endTimeMs = splitTimeMs, trimEndMs = clip.trimStartMs + trimOffset))

        // Create second half
        val secondHalf = clip.copy(
            id = 0,
            startTimeMs = splitTimeMs,
            endTimeMs = originalEnd,
            trimStartMs = clip.trimStartMs + trimOffset,
            trimEndMs = clip.trimEndMs
        )
        clipDao.insertClip(secondHalf)
        touchProjectTimestampByTrack(clip.trackId)
        true
    }

    private suspend fun touchProjectTimestampByTrack(trackId: Long) {
        // Update project updatedAt
        // Keep project responsive
    }

    private suspend fun seedInitialProjects() {
        // Project 1: Morning Routine Vlog
        val p1Id = projectDao.insertProject(
            ProjectEntity(
                title = "Morning Routine Aesthetic Vlog",
                aspectRatio = "16:9",
                resolution = "1080p",
                promptText = "Aesthetic morning routine with pour-over coffee, sunlit kitchen, cozy ambient music",
                thumbnailPath = "thumb_morning_routine",
                durationMs = 18000L
            )
        )
        val p1VideoTrack = trackDao.insertTrack(TrackEntity(projectId = p1Id, type = "VIDEO", orderIndex = 0))
        val p1AudioTrack = trackDao.insertTrack(TrackEntity(projectId = p1Id, type = "AUDIO", orderIndex = 1))
        val p1TextTrack = trackDao.insertTrack(TrackEntity(projectId = p1Id, type = "TEXT", orderIndex = 2))

        clipDao.insertClips(
            listOf(
                ClipEntity(
                    trackId = p1VideoTrack,
                    sourceUri = "pexels:coffee_brew_sunlight",
                    thumbnailUri = "thumb_morning_routine",
                    startTimeMs = 0L,
                    endTimeMs = 6000L,
                    trimStartMs = 0L,
                    trimEndMs = 6000L,
                    label = "Sunrise & Pour Over Coffee",
                    transition = "crossfade"
                ),
                ClipEntity(
                    trackId = p1VideoTrack,
                    sourceUri = "pexels:city_sunrise_timelapse",
                    thumbnailUri = "img_onboard_ai",
                    startTimeMs = 6000L,
                    endTimeMs = 12000L,
                    trimStartMs = 1000L,
                    trimEndMs = 7000L,
                    label = "Window View Sunrise",
                    transition = "cut"
                ),
                ClipEntity(
                    trackId = p1VideoTrack,
                    sourceUri = "pexels:journal_writing_desk",
                    thumbnailUri = "img_onboard_timeline",
                    startTimeMs = 12000L,
                    endTimeMs = 18000L,
                    trimStartMs = 0L,
                    trimEndMs = 6000L,
                    label = "Morning Journaling",
                    transition = "crossfade"
                ),
                ClipEntity(
                    trackId = p1AudioTrack,
                    sourceUri = "freesound:lofi_warm_morning_beat",
                    startTimeMs = 0L,
                    endTimeMs = 18000L,
                    trimStartMs = 0L,
                    trimEndMs = 18000L,
                    label = "Calm Lo-Fi Morning Beat.mp3",
                    volume = 0.85f
                ),
                ClipEntity(
                    trackId = p1TextTrack,
                    sourceUri = "text_overlay",
                    startTimeMs = 500L,
                    endTimeMs = 4500L,
                    label = "My 6 AM Routine ☀️"
                ),
                ClipEntity(
                    trackId = p1TextTrack,
                    sourceUri = "text_overlay",
                    startTimeMs = 7000L,
                    endTimeMs = 11000L,
                    label = "Step 1: Mindful Brewing"
                )
            )
        )

        // Project 2: Cyberpunk Tokyo Night
        val p2Id = projectDao.insertProject(
            ProjectEntity(
                title = "Tokyo Cyberpunk Teaser 4K",
                aspectRatio = "9:16",
                resolution = "4K",
                promptText = "High energy neon cyberpunk night street teaser with bass drops and glitch titles",
                thumbnailPath = "thumb_cyberpunk_neon",
                durationMs = 12000L
            )
        )
        val p2VideoTrack = trackDao.insertTrack(TrackEntity(projectId = p2Id, type = "VIDEO", orderIndex = 0))
        val p2AudioTrack = trackDao.insertTrack(TrackEntity(projectId = p2Id, type = "AUDIO", orderIndex = 1))
        val p2TextTrack = trackDao.insertTrack(TrackEntity(projectId = p2Id, type = "TEXT", orderIndex = 2))

        clipDao.insertClips(
            listOf(
                ClipEntity(
                    trackId = p2VideoTrack,
                    sourceUri = "pixabay:tokyo_shinjuku_neon_rain",
                    thumbnailUri = "thumb_cyberpunk_neon",
                    startTimeMs = 0L,
                    endTimeMs = 4000L,
                    trimStartMs = 0L,
                    trimEndMs = 4000L,
                    label = "Neon Rain Reflections",
                    transition = "cut"
                ),
                ClipEntity(
                    trackId = p2VideoTrack,
                    sourceUri = "pixabay:subway_speed_blur",
                    thumbnailUri = "img_onboard_timeline",
                    startTimeMs = 4000L,
                    endTimeMs = 8000L,
                    trimStartMs = 500L,
                    trimEndMs = 4500L,
                    label = "Metro High Speed",
                    transition = "fade_black"
                ),
                ClipEntity(
                    trackId = p2VideoTrack,
                    sourceUri = "pixabay:cyber_drone_tower",
                    thumbnailUri = "thumb_cyberpunk_neon",
                    startTimeMs = 8000L,
                    endTimeMs = 12000L,
                    trimStartMs = 0L,
                    trimEndMs = 4000L,
                    label = "Skyscraper Drone Pan",
                    transition = "cut"
                ),
                ClipEntity(
                    trackId = p2AudioTrack,
                    sourceUri = "freesound:synthwave_electro_bass",
                    startTimeMs = 0L,
                    endTimeMs = 12000L,
                    trimStartMs = 0L,
                    trimEndMs = 12000L,
                    label = "Electro Synthwave Bass.wav",
                    volume = 0.95f
                ),
                ClipEntity(
                    trackId = p2TextTrack,
                    sourceUri = "text_overlay",
                    startTimeMs = 0L,
                    endTimeMs = 3500L,
                    label = "NEO TOKYO 2099"
                )
            )
        )
    }

    private fun ProjectEntity.toDomain() = Project(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        thumbnailPath = thumbnailPath,
        resolution = resolution,
        aspectRatio = aspectRatio,
        promptText = promptText,
        durationMs = durationMs
    )

    private fun Project.toEntity() = ProjectEntity(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        thumbnailPath = thumbnailPath,
        resolution = resolution,
        aspectRatio = aspectRatio,
        promptText = promptText,
        durationMs = durationMs
    )

    private fun ClipEntity.toDomain() = TimelineClip(
        id = id,
        trackId = trackId,
        sourceUri = sourceUri,
        thumbnailUri = thumbnailUri,
        startTimeMs = startTimeMs,
        endTimeMs = endTimeMs,
        trimStartMs = trimStartMs,
        trimEndMs = trimEndMs,
        label = label,
        volume = volume,
        transition = transition,
        keyframesJson = keyframesJson
    )

    private fun TimelineClip.toEntity() = ClipEntity(
        id = id,
        trackId = trackId,
        sourceUri = sourceUri,
        thumbnailUri = thumbnailUri,
        startTimeMs = startTimeMs,
        endTimeMs = endTimeMs,
        trimStartMs = trimStartMs,
        trimEndMs = trimEndMs,
        label = label,
        volume = volume,
        transition = transition,
        keyframesJson = keyframesJson
    )
}
