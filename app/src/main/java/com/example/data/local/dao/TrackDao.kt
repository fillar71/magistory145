package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM timeline_tracks WHERE projectId = :projectId ORDER BY orderIndex ASC")
    fun getTracksForProject(projectId: Long): Flow<List<TrackEntity>>

    @Query("SELECT * FROM timeline_tracks WHERE projectId = :projectId ORDER BY orderIndex ASC")
    suspend fun getTracksForProjectDirect(projectId: Long): List<TrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>): List<Long>

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Delete
    suspend fun deleteTrack(track: TrackEntity)

    @Query("DELETE FROM timeline_tracks WHERE projectId = :projectId")
    suspend fun deleteTracksForProject(projectId: Long)
}
