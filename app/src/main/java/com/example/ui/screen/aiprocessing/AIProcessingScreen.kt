package com.example.ui.screen.aiprocessing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AiGenerationStep
import com.example.data.repository.AiWorkflowRepository
import com.example.ui.theme.MagistoryDeepOnyx
import com.example.ui.theme.MagistoryElectricCyan
import com.example.ui.theme.MagistoryEmeraldGreen
import com.example.ui.theme.MagistoryPrimaryPurple
import com.example.ui.theme.MagistorySunsetCoral
import com.example.ui.theme.TextMutedSecondary
import com.example.ui.theme.TextWhitePrimary

@Composable
fun AIProcessingScreen(
    prompt: String,
    aspectRatio: String,
    resolution: String,
    aiWorkflowRepository: AiWorkflowRepository,
    onGenerationComplete: (projectId: Long) -> Unit
) {
    var currentStep by remember {
        mutableStateOf(
            AiGenerationStep(
                stepIndex = 1,
                title = "Initializing Autonomous Agent...",
                description = "Preparing prompt and timeline canvas..."
            )
        )
    }
    var progressFraction by remember { mutableFloatStateOf(0.15f) }

    val rotation = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        rotation.animateTo(
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    LaunchedEffect(Unit) {
        val newProjectId = aiWorkflowRepository.generateAutonomousTimeline(
            prompt = prompt,
            aspectRatio = aspectRatio,
            resolution = resolution,
            onStepProgress = { step ->
                currentStep = step
                progressFraction = (step.stepIndex / 5.0f).coerceIn(0.1f, 1.0f)
            }
        )
        progressFraction = 1.0f
        onGenerationComplete(newProjectId)
    }

    val stepTitles = listOf(
        "Synthesizing screenplay & scene concept",
        "Structuring multi-track JSON timeline",
        "Querying Pexels & Pixabay footage",
        "Synchronizing Freesound audio stems",
        "Compiling tracks & building Room DB"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MagistoryDeepOnyx)
            .testTag("ai_processing_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp)
        ) {
            // Rotating AI Aura Orb
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(130.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(126.dp)
                        .clip(CircleShape)
                        .rotate(rotation.value)
                        .border(
                            3.dp,
                            Brush.sweepGradient(
                                listOf(
                                    MagistoryPrimaryPurple,
                                    MagistoryElectricCyan,
                                    MagistorySunsetCoral,
                                    MagistoryPrimaryPurple
                                )
                            ),
                            CircleShape
                        )
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Processing",
                            tint = MagistoryElectricCyan,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Autonomous Agent Running",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = TextWhitePrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentStep.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MagistoryElectricCyan,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = currentStep.description,
                fontSize = 13.sp,
                color = TextMutedSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MagistoryPrimaryPurple,
                    trackColor = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Step ${currentStep.stepIndex} of 5",
                        fontSize = 11.sp,
                        color = TextMutedSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${(progressFraction * 100).toInt()}%",
                        fontSize = 11.sp,
                        color = MagistoryPrimaryPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Pipeline Step Items List
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    stepTitles.forEachIndexed { index, title ->
                        val stepNum = index + 1
                        val isDone = currentStep.stepIndex > stepNum
                        val isCurrent = currentStep.stepIndex == stepNum

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MagistoryEmeraldGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else if (isCurrent) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MagistoryElectricCyan,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Circle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDone || isCurrent) TextWhitePrimary else TextMutedSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
