package com.drink.app.hydration.domain

import com.drink.app.hydration.model.DailyPlan
import com.drink.app.hydration.model.HomeState
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class RecordAttemptTest {
    private val date = LocalDate.of(2026, 10, 14)
    private val zone = ZoneId.of("America/Chicago")
    private val timestamp = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private val plan = DailyPlan(date, 1001, 3, date)
    private val initial = HomeState(date = date, goalMl = 1001, plan = plan)

    @Test fun actionPassesShownPlanCountAndTimestampOnce() {
        val writes = mutableListOf<Triple<DailyPlan, Int, Long>>()
        RecordAttempt { p, count, time -> writes.add(Triple(p, count, time)) }(initial, timestamp, zone)
        assertEquals(listOf(Triple(plan, 0, timestamp)), writes)
    }
    @Test fun nextServingAndRemainingValuesFollowConfirmedRecords() {
        assertEquals(334, initial.nextAmountMl)
        val second = initial.copy(totalMl = 668, recordedAttempts = 2, hasRecords = true)
        assertEquals(333, second.nextAmountMl)
        assertEquals(333L, second.remainingMl)
        assertEquals(1, second.remainingAttempts)
        val finished = second.copy(totalMl = 1101, recordedAttempts = 3)
        assertNull(finished.nextAmountMl)
        assertEquals(0L, finished.remainingMl)
        assertEquals(0, finished.remainingAttempts)
        assertEquals(109L, HydrationRules.completionPercent(finished.totalMl, finished.goalMl))
    }
    @Test fun invalidAndStaleSnapshotsNeverReachStorage() {
        var writes = 0
        val action = RecordAttempt { _, _, _ -> writes++ }
        listOf(initial.copy(plan = null), initial.copy(recordedAttempts = -1), initial.copy(recordedAttempts = 3),
            initial.copy(date = date.minusDays(1))).forEach {
            assertTrue(runCatching { action(it, timestamp, zone) }.isFailure)
        }
        assertTrue(runCatching { action(initial, timestamp + 86_400_000, zone) }.isFailure)
        assertEquals(0, writes)
    }
    @Test fun failedWriteDoesNotInventARecordedAttempt() {
        val failure = IllegalStateException("Disk full")
        val result = runCatching { RecordAttempt { _, _, _ -> throw failure }(initial, timestamp, zone) }
        assertSame(failure, result.exceptionOrNull())
        assertEquals(0, initial.recordedAttempts)
        assertEquals(0L, initial.totalMl)
    }
}
