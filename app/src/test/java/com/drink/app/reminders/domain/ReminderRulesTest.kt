package com.drink.app.reminders.domain

import com.drink.app.hydration.model.DailyPlan
import com.drink.app.reminders.model.ReminderConfig
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ReminderRulesTest {
    private val zone = ZoneId.of("America/Chicago")
    private val date = LocalDate.of(2026, 10, 8)
    @Test fun sleepBoundaries() {
        val config = ReminderConfig()
        assertTrue(ReminderRules.isSleeping(LocalTime.of(22, 0), config))
        assertTrue(ReminderRules.isSleeping(LocalTime.of(2, 0), config))
        assertFalse(ReminderRules.isSleeping(LocalTime.of(8, 0), config))
    }
    @Test fun tenAttemptsAreEvenlySpacedAndFutureOnly() {
        val config = ReminderConfig()
        val slots = ReminderRules.slots(date, 10, config, zone)
        assertEquals(10, slots.size)
        assertTrue(slots.all { it.hour in 8..21 })
        assertTrue(slots.zipWithNext().all { Duration.between(it.first, it.second).toMinutes() >= 15 })
        val plan = DailyPlan(date, 2000, 10, date)
        assertEquals(slots[4], ReminderRules.nextSlot(slots[3], plan, 0, config))
        assertEquals(slots[6], ReminderRules.nextSlot(date.atTime(8, 0).atZone(zone), plan, 6, config))
        assertNull(ReminderRules.nextSlot(slots.last(), plan, 0, config))
        assertNull(ReminderRules.nextSlot(slots.first(), plan, 10, config))
    }
    @Test fun daytimeSleepAndDstAreExcluded() {
        val config = ReminderConfig(sleepTime = LocalTime.of(9, 0), wakeTime = LocalTime.of(17, 0))
        listOf(date, LocalDate.of(2026, 3, 8), LocalDate.of(2026, 11, 1)).forEach { day ->
            val slots = ReminderRules.slots(day, 10, config, zone)
            assertEquals(10, slots.size)
            assertTrue(slots.none { ReminderRules.isSleeping(it.toLocalTime(), config) })
            assertTrue(slots.zipWithNext().all { it.second.toInstant() > it.first.toInstant() })
        }
    }
    @Test fun invalidAndTooCrowdedSchedulesAreRejected() {
        assertFalse(ReminderRules.valid(ReminderConfig(wakeTime = LocalTime.of(22, 0))))
        assertTrue(ReminderRules.slots(date, 10, ReminderConfig(sleepTime = LocalTime.of(9, 0)), zone).isEmpty())
        listOf("25:00", "08:60", "8:00", "", "08:00:00").forEach { assertNull(ReminderRules.parseTime(it)) }
        assertEquals(LocalTime.of(8, 0), ReminderRules.parseTime("08:00"))
    }
}
