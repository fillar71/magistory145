package com.example.domain.model

import org.json.JSONArray
import org.json.JSONObject

data class ClipKeyframe(
    val timeMs: Long,
    val scale: Float = 1.0f,          // 0.2f to 3.0f
    val rotationDeg: Float = 0f,       // -180f to 180f
    val translationX: Float = 0f,      // -100f to +100f (percent)
    val translationY: Float = 0f,      // -100f to +100f (percent)
    val opacity: Float = 1.0f          // 0.0f to 1.0f
)

data class TransformValues(
    val scale: Float = 1.0f,
    val rotationDeg: Float = 0f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val opacity: Float = 1.0f
)

object KeyframeSerializer {
    fun serialize(keyframes: List<ClipKeyframe>): String {
        val array = JSONArray()
        keyframes.sortedBy { it.timeMs }.forEach { kf ->
            val obj = JSONObject().apply {
                put("t", kf.timeMs)
                put("s", kf.scale.toDouble())
                put("r", kf.rotationDeg.toDouble())
                put("x", kf.translationX.toDouble())
                put("y", kf.translationY.toDouble())
                put("o", kf.opacity.toDouble())
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun deserialize(json: String?): List<ClipKeyframe> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<ClipKeyframe>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ClipKeyframe(
                        timeMs = obj.optLong("t", 0L),
                        scale = obj.optDouble("s", 1.0).toFloat(),
                        rotationDeg = obj.optDouble("r", 0.0).toFloat(),
                        translationX = obj.optDouble("x", 0.0).toFloat(),
                        translationY = obj.optDouble("y", 0.0).toFloat(),
                        opacity = obj.optDouble("o", 1.0).toFloat()
                    )
                )
            }
            list.sortedBy { it.timeMs }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Interpolates keyframes and transition effects at a given clip time.
     */
    fun interpolate(
        keyframes: List<ClipKeyframe>,
        clipTimeMs: Long,
        clipDurationMs: Long,
        transition: String?
    ): TransformValues {
        var baseTransform = if (keyframes.isEmpty()) {
            TransformValues()
        } else if (keyframes.size == 1) {
            val single = keyframes.first()
            TransformValues(single.scale, single.rotationDeg, single.translationX, single.translationY, single.opacity)
        } else {
            val sorted = keyframes.sortedBy { it.timeMs }
            when {
                clipTimeMs <= sorted.first().timeMs -> {
                    val first = sorted.first()
                    TransformValues(first.scale, first.rotationDeg, first.translationX, first.translationY, first.opacity)
                }
                clipTimeMs >= sorted.last().timeMs -> {
                    val last = sorted.last()
                    TransformValues(last.scale, last.rotationDeg, last.translationX, last.translationY, last.opacity)
                }
                else -> {
                    // Find adjacent keyframes
                    var prev = sorted.first()
                    var next = sorted.last()
                    for (i in 0 until sorted.size - 1) {
                        if (sorted[i].timeMs <= clipTimeMs && sorted[i + 1].timeMs >= clipTimeMs) {
                            prev = sorted[i]
                            next = sorted[i + 1]
                            break
                        }
                    }
                    val diff = (next.timeMs - prev.timeMs).coerceAtLeast(1L)
                    val factor = ((clipTimeMs - prev.timeMs).toFloat() / diff.toFloat()).coerceIn(0f, 1f)

                    // Smooth ease-in-out curve
                    val easeFactor = (factor * factor * (3f - 2f * factor))

                    TransformValues(
                        scale = prev.scale + (next.scale - prev.scale) * easeFactor,
                        rotationDeg = prev.rotationDeg + (next.rotationDeg - prev.rotationDeg) * easeFactor,
                        translationX = prev.translationX + (next.translationX - prev.translationX) * easeFactor,
                        translationY = prev.translationY + (next.translationY - prev.translationY) * easeFactor,
                        opacity = (prev.opacity + (next.opacity - prev.opacity) * easeFactor).coerceIn(0f, 1f)
                    )
                }
            }
        }

        // Apply transition effects at the boundary
        val transitionDurationMs = 600L.coerceAtMost(clipDurationMs / 2)
        if (transitionDurationMs > 0 && transition != null && transition != "cut") {
            when (transition) {
                "crossfade" -> {
                    if (clipTimeMs < transitionDurationMs) {
                        val fadeFactor = (clipTimeMs.toFloat() / transitionDurationMs.toFloat()).coerceIn(0f, 1f)
                        baseTransform = baseTransform.copy(opacity = baseTransform.opacity * fadeFactor)
                    } else if (clipTimeMs > clipDurationMs - transitionDurationMs) {
                        val fadeFactor = ((clipDurationMs - clipTimeMs).toFloat() / transitionDurationMs.toFloat()).coerceIn(0f, 1f)
                        baseTransform = baseTransform.copy(opacity = baseTransform.opacity * fadeFactor)
                    }
                }
                "fade_black" -> {
                    if (clipTimeMs < transitionDurationMs) {
                        val progress = (clipTimeMs.toFloat() / transitionDurationMs.toFloat()).coerceIn(0f, 1f)
                        baseTransform = baseTransform.copy(opacity = baseTransform.opacity * progress)
                    } else if (clipTimeMs > clipDurationMs - transitionDurationMs) {
                        val progress = ((clipDurationMs - clipTimeMs).toFloat() / transitionDurationMs.toFloat()).coerceIn(0f, 1f)
                        baseTransform = baseTransform.copy(opacity = baseTransform.opacity * progress)
                    }
                }
                "wipe_left" -> {
                    if (clipTimeMs < transitionDurationMs) {
                        val progress = (clipTimeMs.toFloat() / transitionDurationMs.toFloat()).coerceIn(0f, 1f)
                        val wipeOffset = (1f - progress) * 100f
                        baseTransform = baseTransform.copy(translationX = baseTransform.translationX + wipeOffset)
                    }
                }
                "zoom_in" -> {
                    if (clipTimeMs < transitionDurationMs) {
                        val progress = (clipTimeMs.toFloat() / transitionDurationMs.toFloat()).coerceIn(0f, 1f)
                        val zoomScale = 0.7f + 0.3f * progress
                        baseTransform = baseTransform.copy(scale = baseTransform.scale * zoomScale)
                    }
                }
            }
        }

        return baseTransform
    }
}
