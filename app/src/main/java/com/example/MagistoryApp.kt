package com.example

import android.app.Application
import com.example.data.local.database.MagistoryDatabase
import com.example.data.repository.AiWorkflowRepository
import com.example.data.repository.ProjectRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.security.ApiKeyManager
import com.example.security.EncryptionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MagistoryApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { MagistoryDatabase.getInstance(this) }
    val encryptionHelper by lazy { EncryptionHelper() }
    val apiKeyManager by lazy { ApiKeyManager(database.apiKeyDao(), encryptionHelper) }
    val projectRepository by lazy {
        ProjectRepository(
            database.projectDao(),
            database.trackDao(),
            database.clipDao()
        )
    }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val aiWorkflowRepository by lazy {
        AiWorkflowRepository(
            this,
            database.projectDao(),
            database.trackDao(),
            database.clipDao(),
            apiKeyManager
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Pre-seed default projects if database is fresh
        applicationScope.launch {
            projectRepository.ensureInitialSeedData()
        }
    }
}
