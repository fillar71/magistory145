package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ClipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {
    @Query("SELECT * FROM timeline_clips WHERE trackId = :trackId ORDER BY startTimeMs ASC")
    fun getClipsForTrack(trackId: Long): Flow<List<ClipEntity>>

    @Query("SELECT * FROM timeline_clips WHERE trackId = :trackId ORDER BY startTimeMs ASC")
    suspend fun getClipsForTrackDirect(trackId: Long): List<ClipEntity>

    @Query("SELECT c.* FROM timeline_clips c INNER JOIN timeline_tracks t ON c.trackId = t.id WHERE t.projectId = :projectId ORDER BY c.startTimeMs ASC")
    fun getAllClipsForProject(projectId: Long): Flow<List<ClipEntity>>

    @Query("SELECT c.* FROM timeline_clips c INNER JOIN timeline_tracks t ON c.trackId = t.id WHERE t.projectId = :projectId ORDER BY c.startTimeMs ASC")
    suspend fun getAllClipsForProjectDirect(projectId: Long): List<ClipEntity>

    @Query("SELECT * FROM timeline_clips WHERE id = :clipId")
    suspend fun getClipById(clipId: Long): ClipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: ClipEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClips(clips: List<ClipEntity>): List<Long>

    @Update
    suspend fun updateClip(clip: ClipEntity)

    @Delete
    suspend fun deleteClip(clip: ClipEntity)

    @Query("DELETE FROM timeline_clips WHERE id = :clipId")
    suspend fun deleteClipById(clipId: Long)

    @Query("DELETE FROM timeline_clips WHERE trackId = :trackId")
    suspend fun deleteClipsForTrack(trackId: Long)
}
