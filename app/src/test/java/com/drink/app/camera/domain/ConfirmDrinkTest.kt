package com.drink.app.camera.domain

import com.drink.app.hydration.model.DailyPlan
import com.drink.app.hydration.model.HomeState
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ConfirmDrinkTest {
    private val zone = ZoneId.of("America/Chicago")
    private val day = LocalDate.of(2026, 10, 14)
    private val now = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private val shown = HomeState(day, 0, 2000, DailyPlan(day, 2000, 10, day), 0, false)
    private val candidate = PendingDrink("unique-candidate", now, shown)

    @Test fun displayedServingIsPlannedAndCorrectionIsExtra() {
        val modes = mutableListOf<Boolean>()
        val action = ConfirmDrink { _, _, planned, _, _ -> modes.add(planned); true }
        assertTrue(action(candidate, 200, now, zone))
        assertTrue(action(candidate, 150, now, zone))
        assertEquals(listOf(true, false), modes)
    }
    @Test fun persistenceReplayResultIsPreserved() {
        assertFalse(ConfirmDrink { _, _, _, _, _ -> false }(candidate, 200, now, zone))
    }
    @Test fun invalidAmountAndMissingPlanNeverReachPersistence() {
        var calls = 0
        val action = ConfirmDrink { _, _, _, _, _ -> calls++; true }
        for (ml in listOf(0, -1, 100001)) assertTrue(runCatching { action(candidate, ml, now, zone) }.isFailure)
        assertTrue(runCatching { action(candidate.copy(shown = shown.copy(plan = null)), 200, now, zone) }.isFailure)
        assertEquals(0, calls)
    }
    @Test fun dayChangeAndInvalidTokenDoNotWrite() {
        var calls = 0
        val action = ConfirmDrink { _, _, _, _, _ -> calls++; true }
        val tomorrow = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        assertTrue(runCatching { action(candidate, 200, tomorrow, zone) }.isFailure)
        assertTrue(runCatching { action(candidate.copy(token = ""), 200, now, zone) }.isFailure)
        assertEquals(0, calls)
    }
}
