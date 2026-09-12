package com.example.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ClipKeyframe
import com.example.domain.model.KeyframeSerializer
import com.example.domain.model.TimelineClip
import com.example.ui.theme.MagistoryAmberGold
import com.example.ui.theme.MagistoryElectricCyan
import com.example.ui.theme.MagistoryPrimaryPurple
import com.example.ui.theme.MagistorySunsetCoral
import com.example.ui.theme.TextMutedSecondary
import com.example.ui.theme.TextWhitePrimary
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyframeStudioDialog(
    clip: TimelineClip,
    currentPlayheadMs: Long,
    onDismiss: () -> Unit,
    onSave: (updatedClip: TimelineClip) -> Unit
) {
    val clipRelativeTimeMs = (currentPlayheadMs - clip.startTimeMs).coerceIn(0L, clip.durationMs)

    // Current working keyframes
    var keyframes by remember {
        mutableStateOf(clip.keyframes.toMutableList())
    }

    var selectedTransition by remember {
        mutableStateOf(clip.transition ?: "cut")
    }

    // Editable keyframe parameters at current playhead position
    val existingKeyframe = keyframes.find { kotlin.math.abs(it.timeMs - clipRelativeTimeMs) < 150L }
    var currentScale by remember { mutableFloatStateOf(existingKeyframe?.scale ?: 1.0f) }
    var currentRotation by remember { mutableFloatStateOf(existingKeyframe?.rotationDeg ?: 0f) }
    var currentTransX by remember { mutableFloatStateOf(existingKeyframe?.translationX ?: 0f) }
    var currentTransY by remember { mutableFloatStateOf(existingKeyframe?.translationY ?: 0f) }
    var currentOpacity by remember { mutableFloatStateOf(existingKeyframe?.opacity ?: 1.0f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Diamond,
                    contentDescription = null,
                    tint = MagistoryAmberGold,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Keyframe & Transition Studio",
                        color = TextWhitePrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${clip.label ?: "Selected Clip"} • ${clipRelativeTimeMs / 1000f}s / ${clip.durationMs / 1000f}s",
                        color = TextMutedSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Section 1: Transition Shaders
                item {
                    Text(
                        text = "CUSTOM TRANSITION SHADER",
                        color = MagistoryElectricCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val transitions = listOf(
                            "cut" to "Cut (None)",
                            "crossfade" to "Crossfade",
                            "fade_black" to "Fade to Black",
                            "wipe_left" to "Wipe Left",
                            "zoom_in" to "Zoom In"
                        )
                        transitions.forEach { (id, name) ->
                            val isSelected = selectedTransition == id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTransition = id },
                                label = { Text(name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MagistoryPrimaryPurple,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Section 2: Keyframe Transform Controls
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TRANSFORM CONTROLS",
                            color = MagistoryAmberGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "@ ${String.format(Locale.US, "%.2fs", clipRelativeTimeMs / 1000f)}",
                            color = MagistoryAmberGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Scale
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Scale", fontSize = 11.sp, color = TextWhitePrimary)
                            Text("${String.format(Locale.US, "%.2f", currentScale)}x", fontSize = 11.sp, color = MagistoryAmberGold)
                        }
                        Slider(
                            value = currentScale,
                            onValueChange = { currentScale = it },
                            valueRange = 0.5f..2.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = MagistoryAmberGold,
                                activeTrackColor = MagistoryAmberGold
                            )
                        )
                    }

                    // Rotation
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Rotation", fontSize = 11.sp, color = TextWhitePrimary)
                            Text("${currentRotation.toInt()}°", fontSize = 11.sp, color = MagistoryElectricCyan)
                        }
                        Slider(
                            value = currentRotation,
                            onValueChange = { currentRotation = it },
                            valueRange = -180f..180f,
                            colors = SliderDefaults.colors(
                                thumbColor = MagistoryElectricCyan,
                                activeTrackColor = MagistoryElectricCyan
                            )
                        )
                    }

                    // Position X
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Position X", fontSize = 11.sp, color = TextWhitePrimary)
                            Text("${currentTransX.toInt()}%", fontSize = 11.sp, color = TextWhitePrimary)
                        }
                        Slider(
                            value = currentTransX,
                            onValueChange = { currentTransX = it },
                            valueRange = -60f..60f,
                            colors = SliderDefaults.colors(
                                thumbColor = MagistoryPrimaryPurple,
                                activeTrackColor = MagistoryPrimaryPurple
                            )
                        )
                    }

                    // Position Y
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Position Y", fontSize = 11.sp, color = TextWhitePrimary)
                            Text("${currentTransY.toInt()}%", fontSize = 11.sp, color = TextWhitePrimary)
                        }
                        Slider(
                            value = currentTransY,
                            onValueChange = { currentTransY = it },
                            valueRange = -60f..60f,
                            colors = SliderDefaults.colors(
                                thumbColor = MagistoryPrimaryPurple,
                                activeTrackColor = MagistoryPrimaryPurple
                            )
                        )
                    }

                    // Opacity
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Opacity", fontSize = 11.sp, color = TextWhitePrimary)
                            Text("${(currentOpacity * 100).toInt()}%", fontSize = 11.sp, color = TextWhitePrimary)
                        }
                        Slider(
                            value = currentOpacity,
                            onValueChange = { currentOpacity = it },
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White
                            )
                        )
                    }

                    // Add Keyframe Button
                    Button(
                        onClick = {
                            val newKf = ClipKeyframe(
                                timeMs = clipRelativeTimeMs,
                                scale = currentScale,
                                rotationDeg = currentRotation,
                                translationX = currentTransX,
                                translationY = currentTransY,
                                opacity = currentOpacity
                            )
                            val updated = keyframes.filter { kotlin.math.abs(it.timeMs - clipRelativeTimeMs) >= 150L }.toMutableList()
                            updated.add(newKf)
                            keyframes = updated.sortedBy { it.timeMs }.toMutableList()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MagistoryAmberGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_keyframe_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Set Keyframe at ${String.format(Locale.US, "%.2fs", clipRelativeTimeMs / 1000f)}",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Section 3: List of Active Keyframes
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ACTIVE KEYFRAMES (${keyframes.size})",
                        color = TextMutedSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (keyframes.isEmpty()) {
                    item {
                        Text(
                            text = "No keyframes on this clip yet. Scrub playhead and click 'Set Keyframe' above.",
                            fontSize = 11.sp,
                            color = TextMutedSecondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                } else {
                    items(keyframes) { kf ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E192E),
                            border = BorderStroke(0.5.dp, Color(0xFF383054)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Diamond,
                                        contentDescription = null,
                                        tint = MagistoryAmberGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${String.format(Locale.US, "%.2fs", kf.timeMs / 1000f)}: ${String.format(Locale.US, "%.2f", kf.scale)}x • ${kf.rotationDeg.toInt()}° • ${(kf.opacity * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextWhitePrimary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        keyframes = keyframes.filter { it != kf }.toMutableList()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Keyframe",
                                        tint = MagistorySunsetCoral,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = clip.copy(
                        keyframesJson = KeyframeSerializer.serialize(keyframes),
                        transition = selectedTransition
                    )
                    onSave(updated)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MagistoryPrimaryPurple)
            ) {
                Text("Apply to Clip", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMutedSecondary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}
