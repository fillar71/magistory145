package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ApiKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiKeyDao {
    @Query("SELECT * FROM api_keys ORDER BY updatedAt DESC")
    fun getAllKeys(): Flow<List<ApiKeyEntity>>

    @Query("SELECT * FROM api_keys WHERE providerName = :provider LIMIT 1")
    suspend fun getKeyForProvider(provider: String): ApiKeyEntity?

    @Query("SELECT * FROM api_keys WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveKey(): ApiKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(key: ApiKeyEntity): Long

    @Update
    suspend fun updateKey(key: ApiKeyEntity)

    @Delete
    suspend fun deleteKey(key: ApiKeyEntity)

    @Query("DELETE FROM api_keys WHERE providerName = :provider")
    suspend fun deleteKeyForProvider(provider: String)

    @Query("UPDATE api_keys SET isActive = CASE WHEN providerName = :provider THEN 1 ELSE 0 END")
    suspend fun setActiveProvider(provider: String)
}
