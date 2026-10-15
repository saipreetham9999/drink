package com.drink.app.camera.domain

import org.junit.Assert.*
import org.junit.Test

class CameraSessionLimitTest {
    @Test fun sessionExpiresAtExactlyFourMinutesFromInitialization() {
        val limit = CameraSessionLimit()
        limit.start(50_000)
        assertEquals(240L, limit.remainingSeconds(50_000))
        assertEquals(1L, limit.remainingMillis(289_999))
        assertEquals(0L, limit.remainingMillis(290_000))
        assertEquals(0L, limit.remainingMillis(900_000))
    }
    @Test fun repeatedStartCannotExtendActiveSession() {
        val limit = CameraSessionLimit()
        limit.start(0)
        limit.start(239_000)
        assertEquals(1000L, limit.remainingMillis(239_000))
        assertEquals(0L, limit.remainingMillis(240_000))
    }
    @Test fun stopClearsDeadlineAndAnExplicitNewSessionHasNewBudget() {
        val limit = CameraSessionLimit()
        assertEquals(0L, limit.remainingMillis(0))
        limit.start(1000)
        limit.stop()
        assertEquals(0L, limit.remainingMillis(1100))
        limit.start(500_000)
        assertEquals(240_000L, limit.remainingMillis(500_000))
    }
    @Test fun countdownRoundsUpWithoutExtendingTheDeadline() {
        val limit = CameraSessionLimit()
        limit.start(0)
        assertEquals(240L, limit.remainingSeconds(1))
        assertEquals(1L, limit.remainingSeconds(239_999))
        assertEquals(0L, limit.remainingSeconds(240_000))
    }
}
