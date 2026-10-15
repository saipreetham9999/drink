package com.drink.app.reminders.domain

import com.drink.app.hydration.model.DailyPlan
import com.drink.app.reminders.model.ReminderConfig
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ReminderEngineTest {
    private class Fixture(days: Int = 1, attempts: Int = 10, goal: Int = 2000) : ReminderState, ReminderPlans, ReminderPlatform {
        var now = ZonedDateTime.parse("2026-10-08T08:00:00-05:00[America/Chicago]")
        var config = ReminderConfig(enabled = true, sleepConfigured = true)
        val plans = (0 until days).associate { offset ->
            val date = now.toLocalDate().plusDays(offset.toLong())
            date to DailyPlan(date, goal, attempts, now.toLocalDate().plusDays(days - 1L))
        }.toMutableMap()
        val counts = mutableMapOf<LocalDate, Int>()
        override var nextAt = 0L
        override var lastAt = 0L
        override val deliveredSlots = mutableSetOf<String>()
        var permission = true
        var postSucceeds = true
        var alarm = 0L
        var posts = 0
        var active: Triple<LocalDate, Int, Int>? = null
        override fun read() = config
        override fun plan(date: LocalDate) = plans[date]
        override fun completed(date: LocalDate) = counts[date] ?: 0
        override fun allowed() = permission
        override fun schedule(trigger: Long) { alarm = trigger }
        override fun cancelAlarm() { alarm = 0 }
        override fun show(amountMl: Int, date: LocalDate, completed: Int): Boolean {
            if (!postSucceeds) return false
            posts++; active = Triple(date, completed, amountMl); return true
        }
        override fun clearNotificationUnless(date: LocalDate?, completed: Int, amountMl: Int?) {
            if (date == null || active != Triple(date, completed, amountMl)) active = null
        }
        override fun recordDelivery(key: String, deliveredAt: Long) { deliveredSlots.add(key); lastAt = deliveredAt }
        fun engine() = ReminderEngine(this, this, this) { now }
        fun slots() = ReminderRules.slots(now.toLocalDate(), plans.getValue(now.toLocalDate()).attempts, config, now.zone)
        fun atTrigger() { now = Instant.ofEpochMilli(nextAt).atZone(now.zone) }
    }

    @Test fun nextAlarmMatchesPreviewMinuteAndSkipsPastTimes() {
        val f = Fixture()
        val slots = f.slots()
        assertTrue(slots.all { it.second == 0 && it.nano == 0 })
        f.now = slots[3].plusSeconds(10)
        f.engine().reconcile()
        assertEquals(slots[4].toInstant().toEpochMilli(), f.nextAt)
        assertEquals(f.nextAt, f.alarm)
    }

    @Test fun disabledDeniedAndUnconfiguredStatesCancelWithoutPosting() {
        for (mode in 0..3) {
            val f = Fixture()
            f.engine().reconcile()
            f.active = Triple(f.now.toLocalDate(), 0, 200)
            when (mode) {
                0 -> f.config = f.config.copy(enabled = false)
                1 -> f.permission = false
                2 -> f.config = f.config.copy(sleepConfigured = false)
                else -> f.config = f.config.copy(wakeTime = f.config.sleepTime)
            }
            f.engine().reconcile()
            assertEquals(0L, f.nextAt); assertEquals(0L, f.alarm)
            assertNull(f.active); assertEquals(0, f.posts)
        }
    }

    @Test fun dueSlotPostsNextAmountAndNeverRecordsIntake() {
        val f = Fixture(attempts = 3, goal = 1001)
        f.counts[f.now.toLocalDate()] = 2
        val engine = f.engine()
        engine.reconcile(); f.atTrigger()
        val due = f.nextAt
        engine.deliver(due)
        assertEquals(1, f.posts)
        assertEquals(333, f.active?.third)
        assertEquals(2, f.completed(f.now.toLocalDate()))
        assertEquals(f.now.toInstant().toEpochMilli(), f.lastAt)
        engine.deliver(due)
        assertEquals(1, f.posts)
    }

    @Test fun earlyAndUnrelatedBroadcastsDoNotConsumeCurrentAlarm() {
        val f = Fixture()
        val engine = f.engine()
        engine.reconcile()
        val due = f.nextAt
        engine.deliver(due - 1)
        engine.deliver(due)
        assertEquals(due, f.nextAt); assertEquals(due, f.alarm)
        assertEquals(0, f.posts)
    }

    @Test fun delayedSleepingOrPreviousDayAlarmDoesNotPost() {
        for (nextDay in listOf(false, true)) {
            val f = Fixture(days = 7)
            val engine = f.engine()
            engine.reconcile()
            val due = f.nextAt
            f.now = if (nextDay) f.now.plusDays(1).withHour(12) else f.now.withHour(22)
            engine.deliver(due)
            assertEquals(0, f.posts)
            assertTrue(f.nextAt > f.now.toInstant().toEpochMilli())
        }
    }

    @Test fun changedSleepOrAdvancedCountRejectsPreviouslyScheduledSlot() {
        for (changedSleep in listOf(false, true)) {
            val f = Fixture()
            val engine = f.engine()
            engine.reconcile(); f.atTrigger()
            val due = f.nextAt
            if (changedSleep) f.config = f.config.copy(wakeTime = LocalTime.of(9, 0))
            else f.counts[f.now.toLocalDate()] = 1
            engine.deliver(due)
            assertEquals(0, f.posts)
            assertTrue(f.nextAt > due)
        }
    }

    @Test fun completionClearsNotificationAndKeepsNextWeeklyDay() {
        val f = Fixture(days = 7)
        val engine = f.engine()
        engine.reconcile(); f.atTrigger(); engine.deliver(f.nextAt)
        assertNotNull(f.active)
        f.counts[f.now.toLocalDate()] = 10
        engine.reconcile()
        assertNull(f.active)
        assertEquals(f.now.toLocalDate().plusDays(1), Instant.ofEpochMilli(f.nextAt).atZone(f.now.zone).toLocalDate())
    }

    @Test fun recordingOrGoalChangeClearsObsoleteNotification() {
        for (goalChange in listOf(false, true)) {
            val f = Fixture()
            val engine = f.engine()
            engine.reconcile(); f.atTrigger(); engine.deliver(f.nextAt)
            if (goalChange) f.plans[f.now.toLocalDate()] = f.plans.getValue(f.now.toLocalDate()).copy(goalMl = 1500)
            else f.counts[f.now.toLocalDate()] = 1
            engine.reconcile()
            assertNull(f.active)
        }
    }

    @Test fun reopenAndBackwardsClockDoNotRepeatDeliveredSlot() {
        val f = Fixture()
        var engine = f.engine()
        engine.reconcile(); f.atTrigger()
        val fired = f.now
        engine.deliver(f.nextAt)
        engine = f.engine()
        f.now = fired.minusMinutes(10)
        engine.clockChanged()
        assertTrue(f.nextAt > fired.toInstant().toEpochMilli())
        engine.deliver(fired.toInstant().toEpochMilli())
        assertEquals(1, f.posts)
    }

    @Test fun delayedAlarmSkipsSlotsInsideCooldownWithoutCatchupBurst() {
        val f = Fixture()
        val slots = f.slots()
        val engine = f.engine()
        engine.reconcile()
        val due = f.nextAt
        f.now = slots[1].minusMinutes(5)
        engine.deliver(due)
        assertEquals(1, f.posts)
        assertEquals(slots[2].toInstant().toEpochMilli(), f.nextAt)
    }

    @Test fun expiredPlansCancelAndFailedPostsDoNotMarkDelivered() {
        val f = Fixture()
        val engine = f.engine()
        engine.reconcile(); f.atTrigger()
        f.postSucceeds = false
        engine.deliver(f.nextAt)
        assertEquals(0L, f.lastAt); assertTrue(f.deliveredSlots.isEmpty())
        f.now = f.now.plusDays(1)
        engine.reconcile()
        assertEquals(0L, f.nextAt); assertEquals(0L, f.alarm)
    }

    @Test fun timezoneRecoveryUsesNewZoneAndOnlyFutureSlots() {
        val f = Fixture(days = 7)
        val engine = f.engine()
        engine.reconcile()
        f.now = f.now.withZoneSameInstant(ZoneId.of("Asia/Tokyo"))
        engine.reconcile()
        assertTrue(f.nextAt > f.now.toInstant().toEpochMilli())
        val next = Instant.ofEpochMilli(f.nextAt).atZone(f.now.zone)
        assertFalse(ReminderRules.isSleeping(next.toLocalTime(), f.config))
        assertTrue(ReminderRules.slots(next.toLocalDate(), 10, f.config, f.now.zone).contains(next))
    }
}
