package com.example.media.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.domain.model.KeyframeSerializer
import com.example.domain.model.TimelineClip
import com.example.ui.theme.MagistoryAmberGold
import com.example.ui.theme.MagistoryElectricCyan
import com.example.ui.theme.MagistoryEmeraldGreen
import com.example.ui.theme.MagistoryPrimaryPurple

@Composable
fun NativeVideoPlayerViewport(
    aspectRatioString: String,
    resolutionString: String,
    currentPlayheadMs: Long,
    isPlaying: Boolean,
    activeVideoClip: TimelineClip?,
    activeTextClip: TimelineClip?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val previewAspectRatio = when (aspectRatioString) {
        "9:16" -> 9f / 16f
        "1:1" -> 1f
        else -> 16f / 9f
    }

    // Determine if active clip is a real video file (Uri or content://)
    val isRealVideoFile = activeVideoClip != null && (
        activeVideoClip.sourceUri.startsWith("content://") ||
        activeVideoClip.sourceUri.startsWith("file://") ||
        activeVideoClip.sourceUri.endsWith(".mp4") ||
        activeVideoClip.sourceUri.endsWith(".mkv")
    )

    // Compute active keyframe transform
    val currentClipTimeMs = if (activeVideoClip != null) {
        (currentPlayheadMs - activeVideoClip.startTimeMs).coerceAtLeast(0L)
    } else 0L

    val transform = if (activeVideoClip != null) {
        KeyframeSerializer.interpolate(
            keyframes = activeVideoClip.keyframes,
            clipTimeMs = currentClipTimeMs,
            clipDurationMs = activeVideoClip.durationMs.coerceAtLeast(1L),
            transition = activeVideoClip.transition
        )
    } else KeyframeSerializer.interpolate(emptyList(), 0L, 1L, null)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Video aspect container
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(previewAspectRatio)
                .background(Color(0xFF13101E))
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .clip(RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isRealVideoFile && activeVideoClip != null) {
                // REAL HARDWARE VIDEO PLAYBACK ENGINE (ExoPlayer Media3)
                val exoPlayer = remember {
                    ExoPlayer.Builder(context).build().apply {
                        repeatMode = Player.REPEAT_MODE_OFF
                        playWhenReady = isPlaying
                    }
                }

                LaunchedEffect(activeVideoClip.sourceUri) {
                    val mediaItem = MediaItem.fromUri(Uri.parse(activeVideoClip.sourceUri))
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                }

                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            useController = false
                            player = exoPlayer
                        }
                    },
                    update = { view ->
                        val seekTarget = currentClipTimeMs + activeVideoClip.trimStartMs
                        
                        // Only seek if we are too far off to avoid stuttering
                        if (kotlin.math.abs(exoPlayer.currentPosition - seekTarget) > 100) {
                            exoPlayer.seekTo(seekTarget)
                        }
                        
                        if (isPlaying && !exoPlayer.isPlaying) {
                            exoPlayer.play()
                        } else if (!isPlaying && exoPlayer.isPlaying) {
                            exoPlayer.pause()
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = transform.scale
                            scaleY = transform.scale
                            rotationZ = transform.rotationDeg
                            translationX = (transform.translationX / 100f) * size.width
                            translationY = (transform.translationY / 100f) * size.height
                            alpha = transform.opacity
                        }
                        .testTag("native_video_view")
                )

                DisposableEffect(Unit) {
                    onDispose {
                        exoPlayer.release()
                    }
                }
            } else {
                // HIGH-RES FRAME / KEYFRAME COMPOSITOR VIEWPORT
                val thumbRes = when (activeVideoClip?.thumbnailUri) {
                    "thumb_morning_routine" -> R.drawable.thumb_morning_routine
                    "thumb_cyberpunk_neon" -> R.drawable.thumb_cyberpunk_neon
                    "img_onboard_timeline" -> R.drawable.img_onboard_timeline
                    else -> R.drawable.img_onboard_ai
                }

                val hasContentUri = activeVideoClip?.sourceUri?.startsWith("content://") == true ||
                        activeVideoClip?.sourceUri?.startsWith("file://") == true

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = transform.scale
                            scaleY = transform.scale
                            rotationZ = transform.rotationDeg
                            translationX = (transform.translationX / 100f) * size.width
                            translationY = (transform.translationY / 100f) * size.height
                            alpha = transform.opacity
                        }
                ) {
                    if (hasContentUri && activeVideoClip != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(activeVideoClip.sourceUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Imported Media Frame",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = thumbRes),
                            contentDescription = "Preview Frame",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Text Overlay Layer
            if (activeTextClip != null && !activeTextClip.label.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.Center)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xCC000000),
                        border = BorderStroke(1.dp, MagistoryAmberGold.copy(alpha = 0.6f)),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text(
                            text = activeTextClip.label,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Engine & FX status badges
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Engine Mode Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xCC13101E),
                    border = BorderStroke(0.5.dp, if (isRealVideoFile) MagistoryEmeraldGreen else MagistoryPrimaryPurple)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SlowMotionVideo,
                            contentDescription = null,
                            tint = if (isRealVideoFile) MagistoryEmeraldGreen else MagistoryElectricCyan,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isRealVideoFile) "HW VIDEO ENGINE" else "COMPOSITOR",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRealVideoFile) MagistoryEmeraldGreen else MagistoryElectricCyan
                        )
                    }
                }

                if (activeVideoClip?.keyframes?.isNotEmpty() == true) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xCC13101E),
                        border = BorderStroke(0.5.dp, MagistoryAmberGold)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Diamond,
                                contentDescription = null,
                                tint = MagistoryAmberGold,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "KEYFRAMES: ${activeVideoClip.keyframes.size}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MagistoryAmberGold
                            )
                        }
                    }
                }

                if (activeVideoClip?.transition != null && activeVideoClip.transition != "cut") {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xCC13101E),
                        border = BorderStroke(0.5.dp, MagistoryElectricCyan)
                    ) {
                        Text(
                            text = "FX: ${activeVideoClip.transition}",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MagistoryElectricCyan,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Aspect Ratio & Resolution watermark
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0x77000000),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            Text(
                text = "$aspectRatioString • $resolutionString",
                fontSize = 10.sp,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
