package com.example.ui.screen.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.MagistoryDeepOnyx
import com.example.ui.theme.MagistoryElectricCyan
import com.example.ui.theme.MagistoryPrimaryPurple
import com.example.ui.theme.MagistorySunsetCoral
import com.example.ui.theme.TextMutedSecondary
import com.example.ui.theme.TextWhitePrimary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateNext: (targetRoute: String) -> Unit,
    isOnboardingDone: Boolean,
    isLoggedIn: Boolean
) {
    val scale = remember { Animatable(0.7f) }
    val glowAlpha = remember { Animatable(0.3f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.05f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        glowAlpha.animateTo(
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    LaunchedEffect(Unit) {
        delay(1600)
        val route = when {
            !isOnboardingDone -> "onboarding"
            !isLoggedIn -> "login"
            else -> "home"
        }
        onNavigateNext(route)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF140D24),
                        MagistoryDeepOnyx,
                        Color(0xFF07050A)
                    )
                )
            )
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Glowing Emblem Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(130.dp)
                    .scale(scale.value)
            ) {
                // Background Glow
                Box(
                    modifier = Modifier
                        .size(126.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MagistoryPrimaryPurple.copy(alpha = glowAlpha.value * 0.6f),
                                    MagistoryElectricCyan.copy(alpha = glowAlpha.value * 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // App Icon Card
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                listOf(MagistoryPrimaryPurple, MagistoryElectricCyan)
                            ),
                            shape = RoundedCornerShape(26.dp)
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_magistory_logo),
                        contentDescription = "Magistory Emblem",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "MAGISTORY",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = TextWhitePrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Autonomous AI Video Studio",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextMutedSecondary
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MagistoryElectricCyan,
                strokeWidth = 2.5.dp
            )
        }
    }
}
