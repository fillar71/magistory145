package com.example.media.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.R
import com.example.domain.model.KeyframeSerializer
import com.example.domain.model.Project
import com.example.domain.model.TimelineClip
import com.example.domain.model.TimelineTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

data class ExportResult(
    val file: File,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val mimeType: String = "video/mp4"
)

object HardwareVideoExporter {

    private const val TAG = "HardwareVideoExporter"

    suspend fun exportProject(
        context: Context,
        project: Project,
        tracks: List<TimelineTrack>,
        resolution: String = "1080p",
        fps: Int = 30,
        onProgress: (stage: String, progress: Float) -> Unit
    ): ExportResult = withContext(Dispatchers.IO) {
        val totalDurationMs = calculateTotalDuration(project, tracks).coerceIn(2000L, 30000L)
        val (width, height) = calculateDimensions(project.aspectRatio, resolution)

        onProgress("Initializing Hardware Video Encoder...", 0.05f)

        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        if (!outputDir.exists()) outputDir.mkdirs()
        val outputFile = File(outputDir, "Magistory_${project.id}_${System.currentTimeMillis()}.mp4")

        val bitrate = when (resolution) {
            "4K", "1080p" -> 4_500_000
            else -> 2_500_000
        }

        var videoCodec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var isMuxerStarted = false
        var videoTrackIndex = -1
        var samplesWritten = 0

        var frameBitmap: Bitmap? = null

        try {
            videoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            val colorFormat = selectColorFormat(videoCodec, MediaFormat.MIMETYPE_VIDEO_AVC)
            val isSemiPlanar = colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar

            val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // Keyframe every 1 second
            }

            videoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            videoCodec.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val totalFrames = ((totalDurationMs / 1000f) * fps).roundToInt().coerceAtLeast(15)
            val bufferInfo = MediaCodec.BufferInfo()

            // Preload clip bitmaps & thumbnails
            val bitmapCache = preloadClipBitmaps(context, tracks)

            val videoClips = tracks.find { it.type == "VIDEO" }?.clips ?: emptyList()
            val textClips = tracks.find { it.type == "TEXT" }?.clips ?: emptyList()

            onProgress("Compositing video frames & rendering shaders...", 0.15f)

            // Reusable canvas and pixel arrays
            frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val frameCanvas = Canvas(frameBitmap)
            val argbPixels = IntArray(width * height)
            val yuvSize = width * height * 3 / 2
            val yuvBuffer = ByteArray(yuvSize)

            // Encode frames sequentially
            for (frameIndex in 0 until totalFrames) {
                val frameTimeMs = (frameIndex * 1000L) / fps
                val presentationTimeUs = (frameIndex * 1_000_000L) / fps

                // Find active clips at current frame
                val activeVideo = videoClips.find { frameTimeMs >= it.startTimeMs && frameTimeMs <= it.endTimeMs }
                val activeText = textClips.find { frameTimeMs >= it.startTimeMs && frameTimeMs <= it.endTimeMs }

                // 1. Render frame composite to canvas
                renderFrameToCanvas(
                    canvas = frameCanvas,
                    width = width,
                    height = height,
                    frameTimeMs = frameTimeMs,
                    activeVideo = activeVideo,
                    activeText = activeText,
                    bitmapCache = bitmapCache,
                    project = project
                )

                // 2. Convert ARGB to YUV420 byte buffer
                frameBitmap.getPixels(argbPixels, 0, width, 0, 0, width, height)
                convertArgbToYuv(argbPixels, yuvBuffer, width, height, isSemiPlanar)

                // 3. Feed frame into MediaCodec input buffer
                var queued = false
                for (attempt in 0 until 10) {
                    val inputIndex = videoCodec.dequeueInputBuffer(10000L)
                    if (inputIndex >= 0) {
                        val inputBuffer = videoCodec.getInputBuffer(inputIndex)
                        if (inputBuffer != null) {
                            inputBuffer.clear()
                            inputBuffer.put(yuvBuffer, 0, yuvSize)
                            videoCodec.queueInputBuffer(inputIndex, 0, yuvSize, presentationTimeUs, 0)
                            queued = true
                        }
                        break
                    }
                    drainEncoder(
                        codec = videoCodec,
                        muxer = muxer,
                        bufferInfo = bufferInfo,
                        isEndOfStream = false,
                        onMuxerStarted = { idx ->
                            videoTrackIndex = idx
                            isMuxerStarted = true
                        },
                        getVideoTrackIndex = { videoTrackIndex },
                        isMuxerStarted = { isMuxerStarted },
                        onSampleWritten = { samplesWritten++ }
                    )
                }

                // 4. Drain pending encoded packets
                drainEncoder(
                    codec = videoCodec,
                    muxer = muxer,
                    bufferInfo = bufferInfo,
                    isEndOfStream = false,
                    onMuxerStarted = { idx ->
                        videoTrackIndex = idx
                        isMuxerStarted = true
                    },
                    getVideoTrackIndex = { videoTrackIndex },
                    isMuxerStarted = { isMuxerStarted },
                    onSampleWritten = { samplesWritten++ }
                )

                if (frameIndex % 10 == 0) {
                    val progress = 0.15f + (frameIndex.toFloat() / totalFrames) * 0.70f
                    onProgress("Encoding video frames (${frameIndex + 1}/$totalFrames)...", progress)
                }
            }

            // Signal End of Stream on encoder input
            for (attempt in 0 until 15) {
                val inputIndex = videoCodec.dequeueInputBuffer(10000L)
                if (inputIndex >= 0) {
                    val finalTimestampUs = (totalFrames * 1_000_000L) / fps
                    videoCodec.queueInputBuffer(
                        inputIndex, 0, 0, finalTimestampUs,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                    break
                }
                drainEncoder(
                    codec = videoCodec,
                    muxer = muxer,
                    bufferInfo = bufferInfo,
                    isEndOfStream = false,
                    onMuxerStarted = { idx ->
                        videoTrackIndex = idx
                        isMuxerStarted = true
                    },
                    getVideoTrackIndex = { videoTrackIndex },
                    isMuxerStarted = { isMuxerStarted },
                    onSampleWritten = { samplesWritten++ }
                )
            }

            onProgress("Finalizing MP4 container & writing headers...", 0.90f)

            // Drain remaining packets until EOS
            drainEncoder(
                codec = videoCodec,
                muxer = muxer,
                bufferInfo = bufferInfo,
                isEndOfStream = true,
                onMuxerStarted = { idx ->
                    videoTrackIndex = idx
                    isMuxerStarted = true
                },
                getVideoTrackIndex = { videoTrackIndex },
                isMuxerStarted = { isMuxerStarted },
                onSampleWritten = { samplesWritten++ }
            )

            onProgress("Export completed successfully!", 1.0f)

            ExportResult(
                file = outputFile,
                durationMs = totalDurationMs,
                width = width,
                height = height,
                fileSize = outputFile.length()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Video export failed", e)
            throw e
        } finally {
            try {
                frameBitmap?.recycle()
            } catch (_: Exception) {}

            try {
                videoCodec?.stop()
            } catch (e: Exception) {
                Log.w(TAG, "VideoCodec stop failed: ${e.message}")
            }
            try {
                videoCodec?.release()
            } catch (_: Exception) {}

            try {
                if (isMuxerStarted && samplesWritten > 0) {
                    muxer?.stop()
                }
            } catch (e: Exception) {
                Log.w(TAG, "MediaMuxer stop failed: ${e.message}")
            }
            try {
                muxer?.release()
            } catch (_: Exception) {}
        }
    }

    private fun selectColorFormat(codec: MediaCodec, mimeType: String): Int {
        return try {
            val capabilities = codec.codecInfo.getCapabilitiesForType(mimeType)
            if (capabilities.colorFormats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)) {
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
            } else if (capabilities.colorFormats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar)) {
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
            } else {
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
            }
        } catch (_: Exception) {
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
        }
    }

    private fun convertArgbToYuv(
        argb: IntArray,
        yuv: ByteArray,
        width: Int,
        height: Int,
        isSemiPlanar: Boolean
    ) {
        val frameSize = width * height
        val qSize = frameSize / 4
        var yIndex = 0
        var uvIndex = frameSize
        var uIndex = frameSize
        var vIndex = frameSize + qSize

        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                val c = argb[index++]
                val r = (c shr 16) and 0xff
                val g = (c shr 8) and 0xff
                val b = c and 0xff

                // Fast standard BT.601 limited range conversion
                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128

                yuv[yIndex++] = y.coerceIn(16, 235).toByte()

                if (j % 2 == 0 && i % 2 == 0) {
                    if (isSemiPlanar) {
                        if (uvIndex + 1 < yuv.size) {
                            yuv[uvIndex++] = u.coerceIn(16, 240).toByte()
                            yuv[uvIndex++] = v.coerceIn(16, 240).toByte()
                        }
                    } else {
                        if (uIndex < frameSize + qSize) yuv[uIndex++] = u.coerceIn(16, 240).toByte()
                        if (vIndex < yuv.size) yuv[vIndex++] = v.coerceIn(16, 240).toByte()
                    }
                }
            }
        }
    }

    private fun renderFrameToCanvas(
        canvas: Canvas,
        width: Int,
        height: Int,
        frameTimeMs: Long,
        activeVideo: TimelineClip?,
        activeText: TimelineClip?,
        bitmapCache: Map<String, Bitmap>,
        project: Project
    ) {
        // 1. Draw solid dark backdrop
        val bgPaint = Paint().apply { color = Color.parseColor("#0F0C1B") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Draw Video Clip Frame with Keyframes and Transition Shaders
        if (activeVideo != null) {
            val clipTimeMs = (frameTimeMs - activeVideo.startTimeMs).coerceAtLeast(0L)
            val clipDurationMs = activeVideo.durationMs.coerceAtLeast(1L)
            val transform = KeyframeSerializer.interpolate(
                keyframes = activeVideo.keyframes,
                clipTimeMs = clipTimeMs,
                clipDurationMs = clipDurationMs,
                transition = activeVideo.transition
            )

            canvas.save()

            // Apply keyframe transformations (pivot at center)
            val centerX = width / 2f + (transform.translationX / 100f) * width
            val centerY = height / 2f + (transform.translationY / 100f) * height
            canvas.translate(centerX, centerY)
            canvas.rotate(transform.rotationDeg)
            canvas.scale(transform.scale, transform.scale)
            canvas.translate(-width / 2f, -height / 2f)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = (transform.opacity * 255).roundToInt().coerceIn(0, 255)
            }

            val bmp = bitmapCache[activeVideo.thumbnailUri ?: activeVideo.sourceUri]
            if (bmp != null) {
                val srcRect = Rect(0, 0, bmp.width, bmp.height)
                val destRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
                canvas.drawBitmap(bmp, srcRect, destRect, paint)
            } else {
                // Procedural cinematic card
                val gradientPaint = Paint().apply {
                    shader = LinearGradient(
                        0f, 0f, width.toFloat(), height.toFloat(),
                        Color.parseColor("#4A148C"),
                        Color.parseColor("#00E5FF"),
                        Shader.TileMode.CLAMP
                    )
                    alpha = (transform.opacity * 255).roundToInt().coerceIn(0, 255)
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gradientPaint)
            }

            canvas.restore()
        }

        // 3. Draw Text Overlay
        if (activeText != null && !activeText.label.isNullOrBlank()) {
            val label = activeText.label
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = (width * 0.055f).coerceIn(24f, 80f)
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
                setShadowLayer(16f, 0f, 6f, Color.BLACK)
            }

            // Draw text backdrop badge
            val textY = height * 0.75f
            val textBounds = Rect()
            textPaint.getTextBounds(label, 0, label.length, textBounds)
            val padX = width * 0.04f
            val padY = height * 0.02f

            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#B3000000")
            }
            val badgeRect = RectF(
                (width / 2f) - textBounds.width() / 2f - padX,
                textY + textBounds.top - padY,
                (width / 2f) + textBounds.width() / 2f + padX,
                textY + textBounds.bottom + padY
            )
            canvas.drawRoundRect(badgeRect, 20f, 20f, badgePaint)
            canvas.drawText(label, width / 2f, textY, textPaint)
        }

        // 4. Subtle Project Watermark
        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#80FFFFFF")
            textSize = (width * 0.025f).coerceIn(14f, 32f)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Magistory AI", width - 24f, height - 24f, watermarkPaint)
    }

    private fun drainEncoder(
        codec: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        isEndOfStream: Boolean,
        onMuxerStarted: (Int) -> Unit,
        getVideoTrackIndex: () -> Int,
        isMuxerStarted: () -> Boolean,
        onSampleWritten: () -> Unit
    ) {
        val timeoutUs = 10000L
        var tryCount = 0
        while (true) {
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
            if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!isEndOfStream) {
                    break
                } else {
                    tryCount++
                    if (tryCount > 25) {
                        Log.d(TAG, "Drain timeout waiting for EOS; exiting drain loop")
                        break
                    }
                }
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (isMuxerStarted()) {
                    Log.w(TAG, "Format changed again after muxer started")
                } else {
                    val newFormat = codec.outputFormat
                    val trackIdx = muxer.addTrack(newFormat)
                    muxer.start()
                    onMuxerStarted(trackIdx)
                }
            } else if (outputIndex >= 0) {
                val encodedData = codec.getOutputBuffer(outputIndex)
                if (encodedData != null) {
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size != 0 && isMuxerStarted()) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        try {
                            muxer.writeSampleData(getVideoTrackIndex(), encodedData, bufferInfo)
                            onSampleWritten()
                        } catch (e: Exception) {
                            Log.w(TAG, "Error writing sample data", e)
                        }
                    }

                    codec.releaseOutputBuffer(outputIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break
                    }
                } else {
                    codec.releaseOutputBuffer(outputIndex, false)
                }
            }
        }
    }

    private fun calculateDimensions(aspectRatio: String, resolution: String): Pair<Int, Int> {
        val baseSize = when (resolution) {
            "4K", "1080p" -> 720 // Downgrade to 720p maximum for software encoders (emulator)
            else -> 480
        }

        return when (aspectRatio) {
            "9:16" -> Pair(baseSize, (baseSize * 16) / 9)
            "1:1" -> Pair(baseSize, baseSize)
            else -> Pair((baseSize * 16) / 9, baseSize)
        }.let { (w, h) ->
            // Encoders require dimensions to be multiples of 16
            val alignW = (w + 15) and 15.inv()
            val alignH = (h + 15) and 15.inv()
            Pair(alignW, alignH)
        }
    }

    private fun calculateTotalDuration(project: Project, tracks: List<TimelineTrack>): Long {
        var maxEnd = 0L
        tracks.forEach { track ->
            track.clips.forEach { clip ->
                if (clip.endTimeMs > maxEnd) maxEnd = clip.endTimeMs
            }
        }
        return if (maxEnd > 0) maxEnd else project.durationMs
    }

    private fun preloadClipBitmaps(
        context: Context,
        tracks: List<TimelineTrack>
    ): Map<String, Bitmap> {
        val map = mutableMapOf<String, Bitmap>()
        val drawableMap = mapOf(
            "thumb_morning_routine" to R.drawable.thumb_morning_routine,
            "thumb_cyberpunk_neon" to R.drawable.thumb_cyberpunk_neon,
            "img_onboard_timeline" to R.drawable.img_onboard_timeline,
            "img_onboard_ai" to R.drawable.img_onboard_ai
        )

        tracks.forEach { track ->
            track.clips.forEach { clip ->
                val key = clip.thumbnailUri ?: clip.sourceUri
                if (!map.containsKey(key)) {
                    val resId = drawableMap[clip.thumbnailUri]
                    var bmp = if (resId != null) {
                        BitmapFactory.decodeResource(context.resources, resId)
                    } else if (clip.sourceUri.startsWith("content://") || clip.sourceUri.startsWith("file://")) {
                        try {
                            val uri = Uri.parse(clip.sourceUri)
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                BitmapFactory.decodeStream(stream)
                            }
                        } catch (_: Exception) { null }
                    } else null

                    // If still null and it is a local video, grab a frame via MediaMetadataRetriever
                    if (bmp == null && (clip.sourceUri.startsWith("content://") || clip.sourceUri.startsWith("file://"))) {
                        try {
                            val retriever = MediaMetadataRetriever()
                            retriever.setDataSource(context, Uri.parse(clip.sourceUri))
                            bmp = retriever.frameAtTime
                            retriever.release()
                        } catch (_: Exception) { null }
                    }

                    if (bmp != null) {
                        map[key] = bmp
                    }
                }
            }
        }

        // Add a default background image
        if (!map.containsKey("default")) {
            map["default"] = BitmapFactory.decodeResource(context.resources, R.drawable.img_onboard_ai)
        }

        return map
    }
}
