package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.domain.model.Project
import com.example.domain.model.TimelineClip
import com.example.ui.navigation.Screen
import com.example.ui.screen.aiprocessing.AIProcessingScreen
import com.example.ui.screen.editor.TimelineEditorScreen
import com.example.ui.screen.export.ExportScreen
import com.example.ui.screen.home.HomeScreen
import com.example.ui.screen.login.LoginScreen
import com.example.ui.screen.newproject.NewProjectScreen
import com.example.ui.screen.onboarding.OnboardingScreen
import com.example.ui.screen.revision.AIRevisionScreen
import com.example.ui.screen.settings.SettingsScreen
import com.example.ui.screen.splash.SplashScreen
import com.example.ui.screen.subscription.SubscriptionScreen
import com.example.ui.theme.MagistoryTheme
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MagistoryTheme {
                MagistoryNavApp()
            }
        }
    }
}

@Composable
fun MagistoryNavApp() {
    val context = LocalContext.current
    val app = context.applicationContext as MagistoryApp

    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val projects: List<Project> by app.projectRepository.allProjects.collectAsState(initial = emptyList())
    val userProfile by app.userPreferencesRepository.userProfile.collectAsState(
        initial = com.example.data.repository.UserProfile()
    )
    val isOnboardingDone by app.userPreferencesRepository.isOnboardingCompleted.collectAsState(initial = false)
    val isLoggedIn by app.userPreferencesRepository.isLoggedIn.collectAsState(initial = true)

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateNext = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                isOnboardingDone = isOnboardingDone,
                isLoggedIn = isLoggedIn
            )
        }

        // Onboarding Screen
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinishOnboarding = {
                    scope.launch {
                        app.userPreferencesRepository.setOnboardingCompleted(true)
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // Login Screen
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { email, name ->
                    scope.launch {
                        app.userPreferencesRepository.setLoggedIn(true, email, name)
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // Home Screen
        composable(Screen.Home.route) {
            HomeScreen(
                projects = projects,
                userProfile = userProfile,
                onNavigateToNewProject = {
                    navController.navigate(Screen.NewProject.route)
                },
                onNavigateToEditor = { projectId ->
                    navController.navigate(Screen.TimelineEditor.createRoute(projectId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToSubscription = {
                    navController.navigate(Screen.Subscription.route)
                },
                onDeleteProject = { projectId ->
                    scope.launch {
                        app.projectRepository.deleteProject(projectId)
                    }
                }
            )
        }

        // New Project Screen
        composable(Screen.NewProject.route) {
            NewProjectScreen(
                onNavigateBack = { navController.popBackStack() },
                onStartAiGeneration = { prompt, aspectRatio, resolution ->
                    navController.navigate(Screen.AIProcessing.createRoute(prompt, aspectRatio, resolution)) {
                        popUpTo(Screen.NewProject.route) { inclusive = true }
                    }
                },
                onCreateBlankProject = { title, aspectRatio, resolution ->
                    scope.launch {
                        val newId = app.projectRepository.createProject(
                            title = title,
                            aspectRatio = aspectRatio,
                            resolution = resolution,
                            thumbnailPath = "img_onboard_timeline"
                        )
                        navController.navigate(Screen.TimelineEditor.createRoute(newId)) {
                            popUpTo(Screen.NewProject.route) { inclusive = true }
                        }
                    }
                },
                isPremiumUser = userProfile.isPremium
            )
        }

        // AI Processing Screen
        composable(
            route = Screen.AIProcessing.route,
            arguments = listOf(
                navArgument("prompt") { type = NavType.StringType },
                navArgument("aspectRatio") { type = NavType.StringType },
                navArgument("resolution") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val rawPrompt = backStackEntry.arguments?.getString("prompt") ?: ""
            val prompt = android.net.Uri.decode(rawPrompt)
            val aspectRatio = backStackEntry.arguments?.getString("aspectRatio") ?: "16:9"
            val resolution = backStackEntry.arguments?.getString("resolution") ?: "1080p"

            AIProcessingScreen(
                prompt = prompt,
                aspectRatio = aspectRatio,
                resolution = resolution,
                aiWorkflowRepository = app.aiWorkflowRepository,
                onGenerationComplete = { newProjectId ->
                    navController.navigate(Screen.TimelineEditor.createRoute(newProjectId)) {
                        popUpTo(Screen.AIProcessing.route) { inclusive = true }
                    }
                }
            )
        }

        // Timeline Editor Screen
        composable(
            route = Screen.TimelineEditor.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            val projectDetail by app.projectRepository.getProjectDetail(projectId).collectAsState(initial = null)

            TimelineEditorScreen(
                projectDetail = projectDetail,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToExport = { pId ->
                    navController.navigate(Screen.Export.createRoute(pId))
                },
                onNavigateToAiRevision = { pId ->
                    navController.navigate(Screen.AIRevision.createRoute(pId))
                },
                onUpdateClip = { clip ->
                    scope.launch {
                        app.projectRepository.updateClip(clip)
                    }
                },
                onAddClip = { trackId, clip ->
                    scope.launch {
                        app.projectRepository.addClip(trackId, clip)
                    }
                },
                onDeleteClip = { clipId ->
                    scope.launch {
                        app.projectRepository.deleteClip(clipId)
                    }
                },
                onSplitClip = { clipId, splitAtMs ->
                    scope.launch {
                        app.projectRepository.splitClipAtPlayhead(clipId, splitAtMs)
                    }
                }
            )
        }

        // Export Screen
        composable(
            route = Screen.Export.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            val projectDetail by app.projectRepository.getProjectDetail(projectId).collectAsState(initial = null)

            ExportScreen(
                project = projectDetail?.project,
                tracks = projectDetail?.tracks ?: emptyList(),
                isPremiumUser = userProfile.isPremium,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSubscription = {
                    navController.navigate(Screen.Subscription.route)
                }
            )
        }

        // Settings Screen
        composable(Screen.Settings.route) {
            SettingsScreen(
                userProfile = userProfile,
                apiKeyManager = app.apiKeyManager,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSubscription = {
                    navController.navigate(Screen.Subscription.route)
                },
                onLogout = {
                    scope.launch {
                        app.userPreferencesRepository.setLoggedIn(false)
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // Subscription Screen
        composable(Screen.Subscription.route) {
            SubscriptionScreen(
                isCurrentlyPremium = userProfile.isPremium,
                onNavigateBack = { navController.popBackStack() },
                onUpgradeSuccess = {
                    scope.launch {
                        app.userPreferencesRepository.setTier("premium")
                    }
                }
            )
        }

        // AI Revision Assistant Screen (Phase 2)
        composable(
            route = Screen.AIRevision.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L

            AIRevisionScreen(
                projectId = projectId,
                aiWorkflowRepository = app.aiWorkflowRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

// Preserve Greeting for backwards-compatibility with screenshot/unit tests
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
