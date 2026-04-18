package dev.aaa1115910.bv.player.danmaku

import kotlin.math.abs

internal class DanmakuTimer {
    private var lastFrameNanos: Long = 0L
    private var smoothPositionMs: Double = 0.0
    private var lastSeekSerial: Int = 0
    private var lastPlaying: Boolean = false
    private var lastPlaybackSpeed: Double = 1.0

    fun reset(positionMs: Long, nowNanos: Long, seekSerial: Int, isPlaying: Boolean, playbackSpeed: Float) {
        lastFrameNanos = nowNanos
        smoothPositionMs = positionMs.coerceAtLeast(0L).toDouble()
        lastSeekSerial = seekSerial
        lastPlaying = isPlaying
        lastPlaybackSpeed = normalizeSpeed(playbackSpeed)
    }

    fun step(nowNanos: Long, rawPositionMs: Long, isPlaying: Boolean, playbackSpeed: Float, seekSerial: Int): Double {
        val raw = rawPositionMs.coerceAtLeast(0L).toDouble()
        val speed = normalizeSpeed(playbackSpeed)

        if (lastFrameNanos == 0L || seekSerial != lastSeekSerial) {
            reset(rawPositionMs, nowNanos, seekSerial, isPlaying, playbackSpeed)
            return smoothPositionMs
        }

        val dtNanos = (nowNanos - lastFrameNanos).coerceAtLeast(0L)
        lastFrameNanos = nowNanos
        lastSeekSerial = seekSerial

        if (!isPlaying) {
            if (lastPlaying || abs(raw - smoothPositionMs) >= IDLE_REANCHOR_THRESHOLD_MS) {
                smoothPositionMs = raw
            }
            lastPlaying = false
            lastPlaybackSpeed = speed
            return smoothPositionMs
        }

        if (!lastPlaying || abs(speed - lastPlaybackSpeed) >= SPEED_CHANGE_EPSILON) {
            smoothPositionMs = raw
            lastPlaying = true
            lastPlaybackSpeed = speed
            return smoothPositionMs
        }

        if (dtNanos > 0L) {
            smoothPositionMs += dtNanos.toDouble() / 1_000_000.0 * speed
        }

        if (!smoothPositionMs.isFinite() || abs(smoothPositionMs) > 1e15) smoothPositionMs = raw
        if (smoothPositionMs < 0.0) smoothPositionMs = 0.0
        if (abs(raw - smoothPositionMs) >= EXTREME_DRIFT_REANCHOR_THRESHOLD_MS) smoothPositionMs = raw

        lastPlaying = true
        lastPlaybackSpeed = speed
        return smoothPositionMs
    }

    private fun normalizeSpeed(playbackSpeed: Float): Double =
        if (playbackSpeed.isFinite() && playbackSpeed > 0f) playbackSpeed.toDouble() else 1.0

    private companion object {
        const val IDLE_REANCHOR_THRESHOLD_MS = 120.0
        const val EXTREME_DRIFT_REANCHOR_THRESHOLD_MS = 1_000.0
        const val SPEED_CHANGE_EPSILON = 0.0001
    }
}
