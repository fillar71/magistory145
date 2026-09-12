package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Home : Screen("home")
    object NewProject : Screen("new_project")
    object AIProcessing : Screen("ai_processing/{prompt}/{aspectRatio}/{resolution}") {
        fun createRoute(prompt: String, aspectRatio: String, resolution: String): String {
            val encodedPrompt = android.net.Uri.encode(prompt)
            return "ai_processing/$encodedPrompt/$aspectRatio/$resolution"
        }
    }
    object TimelineEditor : Screen("timeline_editor/{projectId}") {
        fun createRoute(projectId: Long): String = "timeline_editor/$projectId"
    }
    object Export : Screen("export/{projectId}") {
        fun createRoute(projectId: Long): String = "export/$projectId"
    }
    object Settings : Screen("settings")
    object Subscription : Screen("subscription")
    object AIRevision : Screen("ai_revision/{projectId}") {
        fun createRoute(projectId: Long): String = "ai_revision/$projectId"
    }
}
