package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_keys")
data class ApiKeyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val providerName: String, // "gemini", "openai", "claude", "nemotron"
    val encryptedKey: ByteArray,
    val iv: ByteArray,
    val isActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ApiKeyEntity

        if (id != other.id) return false
        if (providerName != other.providerName) return false
        if (!encryptedKey.contentEquals(other.encryptedKey)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (isActive != other.isActive) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + providerName.hashCode()
        result = 31 * result + encryptedKey.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + isActive.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}
