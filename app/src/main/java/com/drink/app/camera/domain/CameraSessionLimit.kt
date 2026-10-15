package com.drink.app.camera.domain

/** Uses monotonic elapsed time, never the adjustable wall clock. */
class CameraSessionLimit {
    companion object { const val MAX_MILLIS = 4 * 60 * 1000L }
    private var startedAt: Long? = null

    fun start(now: Long) { if (startedAt == null) startedAt = now }
    fun stop() { startedAt = null }
    fun remainingMillis(now: Long): Long = startedAt?.let {
        (MAX_MILLIS - (now - it).coerceAtLeast(0)).coerceAtLeast(0)
    } ?: 0
    fun remainingSeconds(now: Long): Long = (remainingMillis(now) + 999) / 1000
}
