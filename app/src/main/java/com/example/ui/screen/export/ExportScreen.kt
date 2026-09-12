package com.example.ui.screen.export

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.example.domain.model.Project
import com.example.domain.model.TimelineTrack
import com.example.media.export.ExportResult
import com.example.media.export.HardwareVideoExporter
import com.example.ui.theme.MagistoryAmberGold
import com.example.ui.theme.MagistoryDeepOnyx
import com.example.ui.theme.MagistoryElectricCyan
import com.example.ui.theme.MagistoryEmeraldGreen
import com.example.ui.theme.MagistoryPrimaryPurple
import com.example.ui.theme.MagistorySunsetCoral
import com.example.ui.theme.TextMutedSecondary
import com.example.ui.theme.TextWhitePrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    project: Project?,
    tracks: List<TimelineTrack> = emptyList(),
    isPremiumUser: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateToSubscription: () -> Unit
) {
    val context = LocalContext.current

    if (project == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(MagistoryDeepOnyx),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading project...", color = TextMutedSecondary)
        }
        return
    }

    var selectedResolution by remember {
        mutableStateOf(if (isPremiumUser && project.resolution == "4K") "4K" else "1080p")
    }
    var selectedFps by remember { mutableStateOf("30 FPS") }
    var selectedCodec by remember { mutableStateOf("H.264 / AVC") }

    var isRendering by remember { mutableStateOf(false) }
    var renderProgress by remember { mutableFloatStateOf(0f) }
    var currentRenderStage by remember { mutableStateOf("") }
    var isExportComplete by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<ExportResult?>(null) }
    var exportErrorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    val estimatedFileSizeMb = remember(selectedResolution, selectedFps, project.durationMs) {
        val durationSec = (project.durationMs / 1000).coerceAtLeast(10)
        when (selectedResolution) {
            "4K" -> (durationSec * 6.5f).toInt()
            "1080p" -> (durationSec * 2.2f).toInt()
            else -> (durationSec * 1.1f).toInt()
        }
    }

    fun startRenderingProcess() {
        isRendering = true
        isExportComplete = false
        exportErrorMessage = null
        renderProgress = 0f

        val fpsNum = if (selectedFps.startsWith("60")) 60 else 30

        scope.launch {
            try {
                val result = HardwareVideoExporter.exportProject(
                    context = context,
                    project = project,
                    tracks = tracks,
                    resolution = selectedResolution,
                    fps = fpsNum
                ) { stage, progress ->
                    currentRenderStage = stage
                    renderProgress = progress
                }
                exportResult = result
                isExportComplete = true
            } catch (e: Exception) {
                exportErrorMessage = "Export failed: ${e.message}"
            } finally {
                isRendering = false
            }
        }
    }

    Scaffold(
        containerColor = MagistoryDeepOnyx,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Export Video",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhitePrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhitePrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MagistoryDeepOnyx)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            if (!isRendering && !isExportComplete) {
                // Project Summary Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = MagistoryPrimaryPurple,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = project.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextWhitePrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${project.aspectRatio} • ${project.durationMs / 1000}s duration • Approx. $estimatedFileSizeMb MB",
                                fontSize = 12.sp,
                                color = TextMutedSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Resolution Selector
                Text("Export Resolution", fontWeight = FontWeight.Bold, color = TextWhitePrimary)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("720p", "1080p", "4K").forEach { res ->
                        val isLocked = res == "4K" && !isPremiumUser
                        val isSelected = selectedResolution == res

                        Surface(
                            onClick = {
                                if (isLocked) {
                                    onNavigateToSubscription()
                                } else {
                                    selectedResolution = res
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MagistoryPrimaryPurple.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) MagistoryPrimaryPurple else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = res,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isSelected) MagistoryElectricCyan else TextWhitePrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                if (isLocked) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Diamond, contentDescription = null, tint = MagistorySunsetCoral, modifier = Modifier.size(11.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("PRO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MagistorySunsetCoral)
                                    }
                                } else {
                                    Text(
                                        text = if (res == "4K") "Ultra HD" else if (res == "1080p") "Full HD" else "Standard",
                                        fontSize = 10.sp,
                                        color = TextMutedSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Frame Rate Selector
                Text("Frame Rate", fontWeight = FontWeight.Bold, color = TextWhitePrimary)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("24 FPS", "30 FPS", "60 FPS").forEach { fps ->
                        val isSelected = selectedFps == fps
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFps = fps },
                            label = { Text(fps, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MagistoryPrimaryPurple.copy(alpha = 0.3f),
                                selectedLabelColor = MagistoryPrimaryPurple,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = TextMutedSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outline,
                                selectedBorderColor = MagistoryPrimaryPurple
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Video Codec Selector
                Text("Encoding Codec", fontWeight = FontWeight.Bold, color = TextWhitePrimary)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("H.264 / AVC", "H.265 / HEVC").forEach { codec ->
                        val isSelected = selectedCodec == codec
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCodec = codec },
                            label = { Text(codec, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MagistoryPrimaryPurple.copy(alpha = 0.3f),
                                selectedLabelColor = MagistoryPrimaryPurple,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = TextMutedSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outline,
                                selectedBorderColor = MagistoryPrimaryPurple
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                Button(
                    onClick = { startRenderingProcess() },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MagistorySunsetCoral),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("start_render_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Start Client-Side Render", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }
            } else if (isRendering) {
                // Rendering In Progress View
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                        CircularProgressIndicator(
                            progress = { renderProgress },
                            modifier = Modifier.size(96.dp),
                            color = MagistorySunsetCoral,
                            trackColor = MaterialTheme.colorScheme.outline,
                            strokeWidth = 6.dp
                        )
                        Text(
                            text = "${(renderProgress * 100).toInt()}%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = TextWhitePrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "Rendering Video Offline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhitePrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = currentRenderStage,
                        fontSize = 13.sp,
                        color = MagistoryElectricCyan,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "$selectedResolution @ $selectedFps • $selectedCodec",
                        fontSize = 11.sp,
                        color = TextMutedSecondary
                    )
                }
            } else if (isExportComplete) {
                // Export Complete View
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MagistoryEmeraldGreen.copy(alpha = 0.2f),
                        border = BorderStroke(2.dp, MagistoryEmeraldGreen),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Export Complete",
                                tint = MagistoryEmeraldGreen,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Render Completed Successfully!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = TextWhitePrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Your video has been rendered and saved locally to device storage.",
                        fontSize = 13.sp,
                        color = TextMutedSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MagistoryEmeraldGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Movie, contentDescription = null, tint = MagistoryEmeraldGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PHYSICAL VIDEO EXPORT (MediaCodec + MediaMuxer)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MagistoryEmeraldGreen)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            val file = exportResult?.file
                            val sizeKb = (exportResult?.fileSize ?: 0L) / 1024L
                            val sizeMb = sizeKb / 1024f
                            val sizeDisplay = if (sizeMb >= 1f) String.format(java.util.Locale.US, "%.1f MB", sizeMb) else "$sizeKb KB"

                            Text("File: ${file?.name ?: "Magistory_Video.mp4"}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextWhitePrimary)
                            Text("Size: $sizeDisplay • Dimensions: ${exportResult?.width ?: 1280}x${exportResult?.height ?: 720}", fontSize = 12.sp, color = MagistoryElectricCyan)
                            Text("Path: ${file?.absolutePath ?: "Movies/Magistory"}", fontSize = 11.sp, color = TextMutedSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Play Exported Video
                        Button(
                            onClick = {
                                val file = exportResult?.file
                                if (file != null && file.exists()) {
                                    try {
                                        val contentUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val playIntent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(contentUri, "video/mp4")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(playIntent, "Play Exported Video"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open video player: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Exported file not found", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MagistoryEmeraldGreen),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("play_exported_video_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play Video", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        // Share Exported Video
                        Button(
                            onClick = {
                                val file = exportResult?.file
                                if (file != null && file.exists()) {
                                    try {
                                        val contentUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "video/mp4"
                                            putExtra(Intent.EXTRA_STREAM, contentUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Exported Video"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Exported file not found", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MagistoryPrimaryPurple),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("share_exported_video_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onNavigateBack,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Back to Editor", color = TextWhitePrimary)
                    }
                }
            } else if (exportErrorMessage != null) {
                // Error view
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                ) {
                    Text("Export Failed", color = MagistorySunsetCoral, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(exportErrorMessage ?: "", color = TextMutedSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { startRenderingProcess() },
                        colors = ButtonDefaults.buttonColors(containerColor = MagistoryPrimaryPurple)
                    ) {
                        Text("Retry Export", color = Color.White)
                    }
                }
            }
        }
    }
}
