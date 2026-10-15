package com.drink.app.hydration.domain

import com.drink.app.hydration.domain.HydrationRules
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class HydrationRulesTest {
    @Test fun allocationIsExactAcrossLimitsAndRemainders() {
        for (goal in listOf(1, 2, 31, 32, 33, 1001, 2000, 99999, 100000)) {
            for (attempts in 1..minOf(goal, 32)) {
                val amounts = (0 until attempts).map { HydrationRules.attemptAmount(goal, attempts, it) }
                assertEquals(goal, amounts.sum())
                assertTrue(amounts.all { it > 0 })
                assertTrue(amounts.max() - amounts.min() <= 1)
                assertEquals(amounts.min()..amounts.max(), HydrationRules.attemptRange(goal, attempts))
            }
        }
    }
    @Test fun invalidAllocationAndCompletedIndicesAreRejected() {
        listOf(Triple(0, 10, 0), Triple(2000, 0, 0), Triple(2000, 33, 0), Triple(1, 2, 0),
            Triple(100001, 10, 0), Triple(2000, 10, -1), Triple(2000, 10, 10)).forEach { (goal, attempts, index) ->
            assertTrue(runCatching { HydrationRules.attemptAmount(goal, attempts, index) }.isFailure)
        }
    }
    @Test fun remainingIntakeStopsAtZeroWithExtras() {
        assertEquals(2000L, HydrationRules.remainingMl(0, 2000))
        assertEquals(1800L, HydrationRules.remainingMl(200, 2000))
        assertEquals(0L, HydrationRules.remainingMl(2100, 2000))
        assertEquals(0L, HydrationRules.remainingMl(Long.MAX_VALUE, 2000))
    }
    @Test fun attemptsSumExactlyToTarget() {
        assertEquals(List(10) { 200 }, (0..9).map { HydrationRules.attemptAmount(2000, 10, it) })
        assertEquals(listOf(334, 334, 333), (0..2).map { HydrationRules.attemptAmount(1001, 3, it) })
        assertFalse(HydrationRules.validPlan(10, 11))
        assertFalse(HydrationRules.validPlan(2000, 33))
    }
    @Test fun completionIncludesExtraWater() {
        assertEquals(105L, HydrationRules.completionPercent(2100, 2000))
        assertEquals(0L, HydrationRules.completionPercent(0, 2000))
    }
    @Test fun amountsRejectInvalidInput() {
        listOf("", "0", "-1", "2.5", "100001", "999999999999").forEach {
            assertNull(HydrationRules.validAmount(it))
        }
        assertEquals(250, HydrationRules.validAmount("250"))
    }
    @Test fun progressHandlesZeroAndExceededGoal() {
        assertEquals(0f, HydrationRules.progress(0, 2000), 0f)
        assertEquals(0.25f, HydrationRules.progress(500, 2000), 0f)
        assertEquals(1f, HydrationRules.progress(2500, 2000), 0f)
        assertEquals(0f, HydrationRules.progress(500, 0), 0f)
    }
    @Test fun dayBoundsHandleDaylightSavingAndMidnight() {
        val zone = ZoneId.of("America/Chicago")
        val date = LocalDate.of(2026, 3, 8)
        val (start, end) = HydrationRules.dayBounds(date, zone)
        assertEquals(23 * 60 * 60 * 1000L, end - start)
        assertEquals(end, HydrationRules.dayBounds(date.plusDays(1), zone).first)
        val (fallStart, fallEnd) = HydrationRules.dayBounds(LocalDate.of(2026, 11, 1), zone)
        assertEquals(25 * 60 * 60 * 1000L, fallEnd - fallStart)
    }
}

