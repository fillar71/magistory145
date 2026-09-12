package com.example.ui.screen.editor

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.ProjectDetail
import com.example.domain.model.TimelineClip
import com.example.domain.model.TimelineTrack
import com.example.media.audio.AudioWaveformExtractor
import com.example.media.player.NativeVideoPlayerViewport
import com.example.ui.component.KeyframeStudioDialog
import com.example.ui.component.ResolutionBadge
import com.example.ui.theme.MagistoryAmberGold
import com.example.ui.theme.MagistoryDeepOnyx
import com.example.ui.theme.MagistoryElectricCyan
import com.example.ui.theme.MagistoryEmeraldGreen
import com.example.ui.theme.MagistoryPrimaryPurple
import com.example.ui.theme.MagistorySunsetCoral
import com.example.ui.theme.TextMutedSecondary
import com.example.ui.theme.TextWhitePrimary
import com.example.ui.theme.TrackAudioAccent
import com.example.ui.theme.TrackAudioColor
import com.example.ui.theme.TrackTextAccent
import com.example.ui.theme.TrackTextColor
import com.example.ui.theme.TrackVideoAccent
import com.example.ui.theme.TrackVideoColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineEditorScreen(
    projectDetail: ProjectDetail?,
    onNavigateBack: () -> Unit,
    onNavigateToExport: (projectId: Long) -> Unit,
    onNavigateToAiRevision: (projectId: Long) -> Unit,
    onUpdateClip: (TimelineClip) -> Unit,
    onAddClip: (trackId: Long, TimelineClip) -> Unit,
    onDeleteClip: (clipId: Long) -> Unit,
    onSplitClip: (clipId: Long, splitAtMs: Long) -> Unit
) {
    if (projectDetail == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MagistoryDeepOnyx),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading project...", color = TextMutedSecondary)
        }
        return
    }

    val project = projectDetail.project
    val tracks = projectDetail.tracks
    val totalDuration = projectDetail.totalDurationMs.coerceAtLeast(10000L)

    val context = LocalContext.current

    var isPlaying by remember { mutableStateOf(false) }
    var currentPlayheadMs by remember { mutableLongStateOf(0L) }
    var selectedClipId by remember { mutableStateOf<Long?>(null) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) } // 0.6f to 2.0f
    var showAddClipDialog by remember { mutableStateOf(false) }
    var showVolumeDialog by remember { mutableStateOf(false) }
    var showKeyframeDialog by remember { mutableStateOf(false) }

    // System Photo/Video Picker launcher
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flag)
            } catch (_: Exception) {}

            var fileName = "Imported Media"
            var durationMs = 5000L
            val isVideo = context.contentResolver.getType(uri)?.startsWith("video/") == true ||
                    uri.toString().contains("video")

            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                }
            } catch (_: Exception) {}

            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                if (dur != null && dur > 500L) {
                    durationMs = dur
                }
                retriever.release()
            } catch (_: Exception) {}

            val targetTrack = tracks.find { it.type == "VIDEO" } ?: tracks.firstOrNull()
            if (targetTrack != null) {
                val lastEnd = targetTrack.clips.maxOfOrNull { it.endTimeMs } ?: currentPlayheadMs
                val newClip = TimelineClip(
                    trackId = targetTrack.id,
                    sourceUri = uri.toString(),
                    thumbnailUri = null,
                    startTimeMs = lastEnd,
                    endTimeMs = lastEnd + durationMs,
                    trimStartMs = 0L,
                    trimEndMs = durationMs,
                    label = fileName,
                    transition = "cut"
                )
                onAddClip(targetTrack.id, newClip)
            }
        }
    }

    // Playback ticker loop
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val stepMs = 50L
            while (isPlaying) {
                delay(stepMs)
                currentPlayheadMs += stepMs
                if (currentPlayheadMs >= totalDuration) {
                    currentPlayheadMs = 0L
                    isPlaying = false
                }
            }
        }
    }

    // Identify active video and text clips at playhead
    val videoTrack = tracks.find { it.type == "VIDEO" }
    val audioTrack = tracks.find { it.type == "AUDIO" }
    val textTrack = tracks.find { it.type == "TEXT" }

    val activeVideoClip = videoTrack?.clips?.find { clip ->
        currentPlayheadMs in clip.startTimeMs..clip.endTimeMs
    } ?: videoTrack?.clips?.firstOrNull()

    val activeTextClip = textTrack?.clips?.find { clip ->
        currentPlayheadMs in clip.startTimeMs..clip.endTimeMs
    }

    val selectedClip = tracks.flatMap { it.clips }.find { it.id == selectedClipId }

    // Timecode string formatted MM:SS.S
    fun formatTimecode(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        val tenths = (ms % 1000) / 100
        return String.format(Locale.US, "%02d:%02d.%01d", min, sec, tenths)
    }

    Scaffold(
        containerColor = MagistoryDeepOnyx,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = project.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextWhitePrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ResolutionBadge(resolution = project.resolution)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${project.aspectRatio} • ${formatTimecode(totalDuration)}",
                                fontSize = 11.sp,
                                color = TextMutedSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Save and Back",
                            tint = TextWhitePrimary
                        )
                    }
                },
                actions = {
                    // AI Revision Assistant button (Phase 2 feature)
                    Surface(
                        onClick = { onNavigateToAiRevision(project.id) },
                        shape = RoundedCornerShape(12.dp),
                        color = MagistoryPrimaryPurple.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, MagistoryPrimaryPurple),
                        modifier = Modifier.testTag("ai_revision_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Assistant",
                                tint = MagistoryElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "AI Assist",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhitePrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Export Button
                    Button(
                        onClick = { onNavigateToExport(project.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MagistorySunsetCoral),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MagistoryDeepOnyx)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. HARDWARE VIDEO PLAYER & REAL-TIME KEYFRAME COMPOSITOR VIEWPORT
            NativeVideoPlayerViewport(
                aspectRatioString = project.aspectRatio,
                resolutionString = project.resolution,
                currentPlayheadMs = currentPlayheadMs,
                isPlaying = isPlaying,
                activeVideoClip = activeVideoClip,
                activeTextClip = activeTextClip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            )

            // 2. PLAYHEAD SCRUBBER & TRANSPORT CONTROLS
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                // Timecode
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTimecode(currentPlayheadMs),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MagistoryElectricCyan
                    )
                    Text(
                        text = " / ${formatTimecode(totalDuration)}",
                        fontSize = 12.sp,
                        color = TextMutedSecondary
                    )
                }

                // Playback controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { currentPlayheadMs = (currentPlayheadMs - 3000L).coerceAtLeast(0L) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.FastRewind, contentDescription = "Rewind", tint = TextWhitePrimary)
                    }

                    Surface(
                        onClick = { isPlaying = !isPlaying },
                        shape = CircleShape,
                        color = MagistoryPrimaryPurple,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("play_pause_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { currentPlayheadMs = (currentPlayheadMs + 3000L).coerceAtMost(totalDuration) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = "Fast Forward", tint = TextWhitePrimary)
                    }
                }

                // Zoom controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { zoomScale = (zoomScale - 0.2f).coerceAtLeast(0.6f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = TextMutedSecondary)
                    }
                    IconButton(
                        onClick = { zoomScale = (zoomScale + 0.2f).coerceAtMost(2.5f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = TextMutedSecondary)
                    }
                }
            }

            // 3. EDITING ACTION TOOLBAR (Import Media, Keyframes, Split, Trim, Delete, Add, Volume)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161224))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                // Import User Media from Device / Gallery
                ActionToolChip(
                    icon = Icons.Default.AddPhotoAlternate,
                    label = "Import Media",
                    enabled = true,
                    highlight = true,
                    onClick = {
                        mediaPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    }
                )

                // Keyframe & Transition Studio
                ActionToolChip(
                    icon = Icons.Default.Diamond,
                    label = "Keyframes & FX",
                    enabled = selectedClip != null,
                    highlight = selectedClip?.keyframes?.isNotEmpty() == true,
                    onClick = { showKeyframeDialog = true }
                )

                // Split Action
                ActionToolChip(
                    icon = Icons.Default.CallSplit,
                    label = "Split",
                    enabled = selectedClip != null && currentPlayheadMs > selectedClip.startTimeMs && currentPlayheadMs < selectedClip.endTimeMs,
                    onClick = {
                        selectedClip?.let { clip ->
                            onSplitClip(clip.id, currentPlayheadMs)
                        }
                    }
                )

                // Trim Start (-0.5s)
                ActionToolChip(
                    icon = Icons.Default.FastRewind,
                    label = "Trim +0.5s",
                    enabled = selectedClip != null && selectedClip.durationMs > 1500L,
                    onClick = {
                        selectedClip?.let { clip ->
                            onUpdateClip(clip.copy(startTimeMs = clip.startTimeMs + 500L, trimStartMs = clip.trimStartMs + 500L))
                        }
                    }
                )

                // Trim End (-0.5s)
                ActionToolChip(
                    icon = Icons.Default.FastForward,
                    label = "Trim -0.5s",
                    enabled = selectedClip != null && selectedClip.durationMs > 1500L,
                    onClick = {
                        selectedClip?.let { clip ->
                            onUpdateClip(clip.copy(endTimeMs = clip.endTimeMs - 500L, trimEndMs = clip.trimEndMs - 500L))
                        }
                    }
                )

                // Volume Adjust
                ActionToolChip(
                    icon = Icons.Default.VolumeUp,
                    label = "Volume",
                    enabled = selectedClip != null,
                    onClick = { showVolumeDialog = true }
                )

                // Add Clip Action
                ActionToolChip(
                    icon = Icons.Default.Add,
                    label = "Add Clip",
                    enabled = true,
                    highlight = true,
                    onClick = { showAddClipDialog = true }
                )

                // Delete Clip
                ActionToolChip(
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    enabled = selectedClip != null,
                    isDestructive = true,
                    onClick = {
                        selectedClip?.let { clip ->
                            onDeleteClip(clip.id)
                            selectedClipId = null
                        }
                    }
                )
            }

            // 4. MULTI-TRACK TIMELINE CANVAS
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0F0C1B))
                    .testTag("timeline_canvas")
            ) {
                val timelineWidthDp = (totalDuration / 100 * zoomScale).coerceAtLeast(800f).dp
                val timelineScrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(timelineScrollState)
                        .width(timelineWidthDp)
                        .padding(vertical = 10.dp)
                ) {
                    // Time Ruler Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(Color(0xFF181427))
                    ) {
                        val intervalMs = 2000L
                        val markCount = (totalDuration / intervalMs).toInt() + 1
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (i in 0..markCount) {
                                Text(
                                    text = "${i * 2}s",
                                    fontSize = 9.sp,
                                    color = TextMutedSecondary,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // TRACK 1: VIDEO
                    TimelineTrackRow(
                        trackType = "VIDEO",
                        trackColor = TrackVideoColor,
                        accentColor = TrackVideoAccent,
                        icon = Icons.Default.Videocam,
                        clips = videoTrack?.clips ?: emptyList(),
                        totalDurationMs = totalDuration,
                        totalWidthDp = timelineWidthDp,
                        selectedClipId = selectedClipId,
                        onSelectClip = { selectedClipId = it }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // TRACK 2: AUDIO
                    TimelineTrackRow(
                        trackType = "AUDIO",
                        trackColor = TrackAudioColor,
                        accentColor = TrackAudioAccent,
                        icon = Icons.Default.Audiotrack,
                        clips = audioTrack?.clips ?: emptyList(),
                        totalDurationMs = totalDuration,
                        totalWidthDp = timelineWidthDp,
                        selectedClipId = selectedClipId,
                        onSelectClip = { selectedClipId = it }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // TRACK 3: TEXT
                    TimelineTrackRow(
                        trackType = "TEXT",
                        trackColor = TrackTextColor,
                        accentColor = TrackTextAccent,
                        icon = Icons.Default.TextFields,
                        clips = textTrack?.clips ?: emptyList(),
                        totalDurationMs = totalDuration,
                        totalWidthDp = timelineWidthDp,
                        selectedClipId = selectedClipId,
                        onSelectClip = { selectedClipId = it }
                    )
                }

                // PLAYHEAD LINE OVERLAY
                val playheadFraction = (currentPlayheadMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
                val playheadOffsetDp = timelineWidthDp * playheadFraction

                Box(
                    modifier = Modifier
                        .offset(x = playheadOffsetDp - timelineScrollState.value.dp)
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(MagistoryElectricCyan)
                ) {
                    // Playhead needle head
                    Surface(
                        shape = CircleShape,
                        color = MagistoryElectricCyan,
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.TopCenter)
                    ) {}
                }
            }
        }
    }

    // Add Clip Dialog
    if (showAddClipDialog) {
        var newClipType by remember { mutableStateOf("VIDEO") }
        var newClipLabel by remember { mutableStateOf("") }
        var newClipDurationSec by remember { mutableFloatStateOf(4f) }

        AlertDialog(
            onDismissRequest = { showAddClipDialog = false },
            title = { Text("Add New Clip to Timeline", color = TextWhitePrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Select Track Type:", fontSize = 12.sp, color = TextMutedSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("VIDEO", "AUDIO", "TEXT").forEach { type ->
                            val isSelected = newClipType == type
                            Surface(
                                onClick = { newClipType = type },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MagistoryPrimaryPurple else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = type,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = newClipLabel,
                        onValueChange = { newClipLabel = it },
                        label = { Text("Clip Name or Overlay Text") },
                        placeholder = { Text(if (newClipType == "TEXT") "e.g., Summer Vibes 🌴" else "e.g., Drone Sunset Shot") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MagistoryPrimaryPurple,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = TextWhitePrimary,
                            unfocusedTextColor = TextWhitePrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Duration: ${newClipDurationSec.toInt()} seconds", fontSize = 12.sp, color = TextMutedSecondary)
                    Slider(
                        value = newClipDurationSec,
                        onValueChange = { newClipDurationSec = it },
                        valueRange = 2f..15f,
                        steps = 12,
                        colors = SliderDefaults.colors(thumbColor = MagistoryElectricCyan, activeTrackColor = MagistoryPrimaryPurple)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetTrack = tracks.find { it.type == newClipType }
                        if (targetTrack != null) {
                            val durationMs = (newClipDurationSec * 1000).toLong()
                            val lastEnd = targetTrack.clips.maxOfOrNull { it.endTimeMs } ?: currentPlayheadMs
                            val newClip = TimelineClip(
                                trackId = targetTrack.id,
                                sourceUri = if (newClipType == "TEXT") "text_overlay" else "user_asset:clip",
                                thumbnailUri = if (newClipType == "VIDEO") "img_onboard_timeline" else null,
                                startTimeMs = lastEnd,
                                endTimeMs = lastEnd + durationMs,
                                trimStartMs = 0L,
                                trimEndMs = durationMs,
                                label = if (newClipLabel.isNotBlank()) newClipLabel else "$newClipType Clip",
                                transition = "cut"
                            )
                            onAddClip(targetTrack.id, newClip)
                        }
                        showAddClipDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MagistoryPrimaryPurple)
                ) {
                    Text("Add Clip", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddClipDialog = false }) {
                    Text("Cancel", color = TextMutedSecondary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    // Volume Adjust Dialog
    if (showVolumeDialog && selectedClip != null) {
        var vol by remember { mutableFloatStateOf(selectedClip.volume) }
        AlertDialog(
            onDismissRequest = { showVolumeDialog = false },
            title = { Text("Clip Volume & Audio Level", color = TextWhitePrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(selectedClip.label ?: "Selected Clip", color = TextWhitePrimary, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Gain: ${(vol * 100).toInt()}%", fontSize = 13.sp, color = MagistoryElectricCyan)
                    Slider(
                        value = vol,
                        onValueChange = { vol = it },
                        valueRange = 0.0f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = MagistoryElectricCyan, activeTrackColor = MagistoryPrimaryPurple)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateClip(selectedClip.copy(volume = vol))
                        showVolumeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MagistoryPrimaryPurple)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVolumeDialog = false }) {
                    Text("Cancel", color = TextMutedSecondary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    // Keyframe & Transition Studio Dialog
    if (showKeyframeDialog && selectedClip != null) {
        KeyframeStudioDialog(
            clip = selectedClip,
            currentPlayheadMs = currentPlayheadMs,
            onDismiss = { showKeyframeDialog = false },
            onSave = { updatedClip ->
                onUpdateClip(updatedClip)
                showKeyframeDialog = false
            }
        )
    }
}

@Composable
fun AudioWaveformCanvas(
    sourceUri: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var amplitudes by remember(sourceUri) { mutableStateOf<List<Float>>(emptyList()) }

    LaunchedEffect(sourceUri) {
        amplitudes = AudioWaveformExtractor.extractWaveform(context, sourceUri, bucketCount = 50)
    }

    Canvas(modifier = modifier) {
        if (amplitudes.isEmpty()) return@Canvas
        val barCount = amplitudes.size
        val barWidth = (size.width / (barCount * 1.5f)).coerceIn(2f, 6f)
        val gap = barWidth * 0.4f
        val centerY = size.height / 2f
        val maxBarHeight = size.height * 0.85f

        for (i in amplitudes.indices) {
            val amp = amplitudes[i]
            val x = i * (barWidth + gap)
            if (x > size.width) break
            val barHeight = (amp * maxBarHeight).coerceAtLeast(3f)
            val top = centerY - barHeight / 2f
            val bottom = centerY + barHeight / 2f

            drawLine(
                color = Color(0xAA00E5FF),
                start = Offset(x, top),
                end = Offset(x, bottom),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun ActionToolChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    highlight: Boolean = false,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        color = when {
            !enabled -> Color(0xFF1F1A2F)
            highlight -> MagistoryPrimaryPurple
            isDestructive -> MagistorySunsetCoral.copy(alpha = 0.2f)
            else -> Color(0xFF27213C)
        },
        border = BorderStroke(
            1.dp,
            if (highlight) MagistoryPrimaryPurple else if (isDestructive) MagistorySunsetCoral else Color(0xFF3B3356)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = when {
                    !enabled -> Color(0xFF5A5274)
                    highlight -> Color.White
                    isDestructive -> MagistorySunsetCoral
                    else -> MagistoryElectricCyan
                },
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    !enabled -> Color(0xFF5A5274)
                    highlight -> Color.White
                    isDestructive -> MagistorySunsetCoral
                    else -> TextWhitePrimary
                }
            )
        }
    }
}

@Composable
private fun TimelineTrackRow(
    trackType: String,
    trackColor: Color,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    clips: List<TimelineClip>,
    totalDurationMs: Long,
    totalWidthDp: Dp,
    selectedClipId: Long?,
    onSelectClip: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(Color(0xFF13101E))
            .border(0.5.dp, Color(0xFF251F38))
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            clips.forEach { clip ->
                val startFraction = (clip.startTimeMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                val durationFraction = (clip.durationMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                val clipOffsetDp = totalWidthDp * startFraction
                val clipWidthDp = (totalWidthDp * durationFraction).coerceAtLeast(42.dp)
                val isSelected = clip.id == selectedClipId

                Surface(
                    onClick = { onSelectClip(clip.id) },
                    shape = RoundedCornerShape(6.dp),
                    color = trackColor.copy(alpha = if (isSelected) 0.95f else 0.75f),
                    border = BorderStroke(
                        if (isSelected) 2.dp else 0.5.dp,
                        if (isSelected) MagistoryElectricCyan else accentColor
                    ),
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(clipWidthDp)
                        .offset(x = clipOffsetDp)
                        .testTag("clip_${clip.id}")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Real Audio Waveform Visualization on Audio Track
                        if (trackType == "AUDIO") {
                            AudioWaveformCanvas(
                                sourceUri = clip.sourceUri,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                            )
                        }

                        // Clip Label Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .align(Alignment.TopStart)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = clip.label ?: "${trackType} Clip",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${clip.durationMs / 1000}s",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        // Transition FX Badge
                        if (clip.transition != null && clip.transition != "cut") {
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = Color(0xCC7C4DFF),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(4.dp)
                            ) {
                                Text(
                                    text = "FX: ${clip.transition}",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        // Keyframe Diamonds along the clip
                        if (clip.keyframes.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Diamond,
                                    contentDescription = "Keyframes",
                                    tint = MagistoryAmberGold,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "${clip.keyframes.size} kf",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MagistoryAmberGold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
