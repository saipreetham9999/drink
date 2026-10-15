package com.drink.app.hydration.domain

import com.drink.app.hydration.model.PlanRequest
import com.drink.app.reminders.model.ReminderConfig
import com.drink.app.reminders.model.SleepHours
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class SavePlanTest {
    private val now = ZonedDateTime.of(2026, 3, 7, 12, 0, 0, 0, ZoneId.of("America/Chicago"))
    private val config = ReminderConfig(sleepConfigured = true)

    @Test fun validPlanPassesOneCompleteSnapshotToStorage() {
        val saved = mutableListOf<Triple<PlanRequest, SleepHours, LocalDate>>()
        val action = SavePlan { request, hours, date -> saved.add(Triple(request, hours, date)) }
        val request = PlanRequest(2000, 10, 7)
        action(request, config, now.toLocalDate(), now)
        assertEquals(listOf(Triple(request, SleepHours(config.sleepTime, config.wakeTime), now.toLocalDate())), saved)
    }

    @Test fun invalidOrStaleRequestsNeverReachStorage() {
        var calls = 0
        val action = SavePlan { _, _, _ -> calls++ }
        listOf(PlanRequest(0, 10, 1), PlanRequest(2000, 0, 1), PlanRequest(2000, 10, 2), PlanRequest(5, 10, 7)).forEach {
            assertTrue(runCatching { action(it, config, now.toLocalDate(), now) }.isFailure)
        }
        val request = PlanRequest(2000, 10, 1)
        listOf(config.copy(sleepConfigured = false), config.copy(wakeTime = config.sleepTime),
            config.copy(sleepTime = LocalTime.of(9, 0))).forEach {
            assertTrue(runCatching { action(request, it, now.toLocalDate(), now) }.isFailure)
        }
        assertTrue(runCatching { action(request, config, now.toLocalDate().minusDays(1), now) }.isFailure)
        assertEquals(0, calls)
    }

    @Test fun selectedDurationControlsDstValidation() {
        val shortWindow = config.copy(wakeTime = LocalTime.of(1, 0), sleepTime = LocalTime.of(3, 30))
        assertTrue(PlanRules.valid(PlanRequest(2000, 8, 1), shortWindow, now.toLocalDate(), now.zone))
        assertFalse(PlanRules.valid(PlanRequest(2000, 8, 7), shortWindow, now.toLocalDate(), now.zone))
    }

    @Test fun storageFailureIsReportedToCaller() {
        val failure = IllegalStateException("Storage unavailable")
        val action = SavePlan { _, _, _ -> throw failure }
        val result = runCatching { action(PlanRequest(2000, 10, 1), config, now.toLocalDate(), now) }
        assertSame(failure, result.exceptionOrNull())
    }
}
