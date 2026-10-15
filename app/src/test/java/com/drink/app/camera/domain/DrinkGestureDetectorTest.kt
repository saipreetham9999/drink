package com.drink.app.camera.domain

import org.junit.Assert.*
import org.junit.Test

class DrinkGestureDetectorTest {
    private fun frame(time: Long, near: Boolean, confidence: Float = .95f, hand: Int = 15): PoseFrame {
        val points = MutableList(33) { PosePoint(.5f, .8f, 0f) }
        points[9] = PosePoint(.48f, .25f, confidence); points[10] = PosePoint(.52f, .25f, confidence)
        points[11] = PosePoint(.3f, .45f, confidence); points[12] = PosePoint(.7f, .45f, confidence)
        points[hand] = PosePoint(.5f, if (near) .28f else .8f, confidence)
        return PoseFrame(time, points, 480, 640)
    }
    private fun gesture(detector: DrinkGestureDetector, offset: Long = 0, hand: Int = 15): List<DrinkGesture> =
        (0L..1800L step 200).mapNotNull { detector.accept(frame(offset + it, it in 600..1400, hand = hand)) }

    @Test fun approachHoldAndReturnCreatesExactlyOneSuggestionForEitherHand() {
        for (hand in 15..16) assertEquals(1, gesture(DrinkGestureDetector(), hand = hand).size)
    }
    @Test fun cooldownAndHeldHandDoNotCreateDuplicateSuggestions() {
        val detector = DrinkGestureDetector()
        assertEquals(1, gesture(detector).size)
        assertTrue(gesture(detector, 2000).isEmpty())
        assertEquals(1, gesture(detector, 18000).size)
    }
    @Test fun lowConfidenceAndMissingLandmarksNeverProduceSuggestions() {
        val detector = DrinkGestureDetector()
        for (t in 0L..4000L step 200) assertNull(detector.accept(frame(t, t > 400, .2f)))
        assertNull(detector.accept(PoseFrame(5000, emptyList(), 480, 640)))
    }
    @Test fun staticHandNearMouthAndQuickTouchesAreNotEnough() {
        val detector = DrinkGestureDetector()
        for (t in 0L..3000L step 200) assertNull(detector.accept(frame(t, true)))
        val quick = DrinkGestureDetector()
        for (t in 0L..1800L step 200) assertNull(quick.accept(frame(t, t in 600..800)))
    }
    @Test fun missingFramesAndOutOfOrderFramesCannotCompleteGesture() {
        val detector = DrinkGestureDetector()
        detector.accept(frame(0, false)); detector.accept(frame(400, false))
        for (t in 1400L..2400L step 200) assertNull(detector.accept(frame(t, true)))
        assertNull(detector.accept(frame(1800, false)))
        assertNull(detector.accept(frame(2600, false)))
    }
    @Test fun restartDoesNotForgetRecentCandidateCooldown() {
        val detector = DrinkGestureDetector()
        assertEquals(1, gesture(detector).size)
        detector.reset()
        assertTrue(gesture(detector, 4000).isEmpty())
    }
}
