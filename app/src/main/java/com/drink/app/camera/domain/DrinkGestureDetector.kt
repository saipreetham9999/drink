package com.drink.app.camera.domain

import kotlin.math.hypot

data class PosePoint(val x: Float, val y: Float, val confidence: Float)
data class PoseFrame(val timeMs: Long, val points: List<PosePoint>, val width: Int, val height: Int,
    val mirrored: Boolean = false, val inferenceMs: Long = 0)
data class DrinkGesture(val elapsedMs: Long)

/** A conservative movement baseline, not proof of drinking or a volume measurement. */
class DrinkGestureDetector {
    private data class Arm(var awayAt: Long? = null, var nearAt: Long? = null,
        var returnAt: Long? = null, var held: Boolean = false, var samples: Int = 0)
    private val arms = arrayOf(Arm(), Arm())
    private var previous: Long? = null
    private var lastCandidate: Long? = null

    fun reset() { arms.indices.forEach { arms[it] = Arm() }; previous = null }

    fun accept(frame: PoseFrame): DrinkGesture? {
        val old = previous
        if (old != null && frame.timeMs <= old) return null
        if (old != null && frame.timeMs - old > 750) arms.indices.forEach { arms[it] = Arm() }
        previous = frame.timeMs
        fun point(index: Int) = frame.points.getOrNull(index)?.takeIf {
            it.confidence >= .7f && it.x.isFinite() && it.y.isFinite() && it.x in 0f..1f && it.y in 0f..1f
        }
        val mouthA = point(9); val mouthB = point(10)
        val shoulderA = point(11); val shoulderB = point(12)
        if (mouthA == null || mouthB == null || shoulderA == null || shoulderB == null) {
            arms.indices.forEach { arms[it] = Arm() }; return null
        }
        // Use pixel distances so portrait/landscape normalization does not distort geometry.
        fun distance(ax: Float, ay: Float, bx: Float, by: Float) =
            hypot((ax - bx) * frame.width, (ay - by) * frame.height)
        val scale = distance(shoulderA.x, shoulderA.y, shoulderB.x, shoulderB.y)
        if (scale < frame.width * .08f || scale <= 0) { arms.indices.forEach { arms[it] = Arm() }; return null }
        if (lastCandidate?.let { frame.timeMs - it < 15_000 } == true) return null
        val mx = (mouthA.x + mouthB.x) / 2; val my = (mouthA.y + mouthB.y) / 2
        for (side in 0..1) {
            val wrist = point(15 + side)
            if (wrist == null) { arms[side] = Arm(); continue }
            var arm = arms[side]
            if (arm.awayAt?.let { frame.timeMs - it > 12_000 } == true) { arms[side] = Arm(); arm = arms[side] }
            val ratio = distance(wrist.x, wrist.y, mx, my) / scale
            when {
                arm.held && ratio >= .9f -> {
                    if (arm.returnAt == null) arm.returnAt = frame.timeMs
                    if (frame.timeMs - arm.returnAt!! >= 200) {
                        lastCandidate = frame.timeMs
                        arms.indices.forEach { arms[it] = Arm() }
                        return DrinkGesture(frame.timeMs)
                    }
                }
                arm.held -> arm.returnAt = null
                ratio >= .9f -> {
                    if (arm.nearAt != null) { arms[side] = Arm(); arm = arms[side] }
                    if (arm.awayAt == null) arm.awayAt = frame.timeMs
                }
                ratio <= .45f && arm.awayAt?.let { frame.timeMs - it >= 400 } == true -> {
                    if (arm.nearAt == null) arm.nearAt = frame.timeMs
                    arm.samples++
                    if (frame.timeMs - arm.nearAt!! >= 800 && arm.samples >= 4) arm.held = true
                }
                else -> { arm.nearAt = null; arm.samples = 0 }
            }
        }
        return null
    }
}
