package com.drink.app.reminders.domain

import com.drink.app.hydration.domain.HydrationRules
import com.drink.app.hydration.model.DailyPlan
import com.drink.app.reminders.model.ReminderConfig
import java.time.LocalDate
import java.time.ZonedDateTime

interface ReminderState {
    fun read(): ReminderConfig
    var nextAt: Long
    var lastAt: Long
    val deliveredSlots: Set<String>
    fun recordDelivery(key: String, deliveredAt: Long)
}

interface ReminderPlans {
    fun plan(date: LocalDate): DailyPlan?
    fun completed(date: LocalDate): Int
}

interface ReminderPlatform {
    fun allowed(): Boolean
    fun schedule(trigger: Long)
    fun cancelAlarm()
    fun show(amountMl: Int, date: LocalDate, completed: Int): Boolean
    fun clearNotificationUnless(date: LocalDate?, completed: Int, amountMl: Int?)
}

/** Contains no Android calls. The caller serializes scheduling and alarm delivery. */
class ReminderEngine(private val state: ReminderState, private val plans: ReminderPlans,
    private val platform: ReminderPlatform, private val currentTime: () -> ZonedDateTime = { ZonedDateTime.now() }) {
    companion object {
        const val COOLDOWN_MS = 15 * 60_000L
        fun slotKey(slot: ZonedDateTime): String = "${slot.toLocalDate()}|${slot.zone.id}|${slot.toLocalTime()}|${slot.offset}"
    }

    private fun usable(plan: DailyPlan?, date: LocalDate, count: Int): Boolean = plan != null &&
        plan.date == date && plan.endsOn >= date && HydrationRules.validPlan(plan.goalMl, plan.attempts) && count in 0 until plan.attempts

    fun reconcile() {
        val now = currentTime()
        if (state.lastAt > now.toInstant().toEpochMilli()) state.lastAt = 0
        val config = state.read()
        if (!config.enabled || !config.sleepConfigured || !ReminderRules.valid(config) || !platform.allowed()) {
            state.nextAt = 0
            platform.cancelAlarm()
            platform.clearNotificationUnless(null, 0, null)
            return
        }
        val today = now.toLocalDate()
        val todayPlan = plans.plan(today)
        val todayCount = plans.completed(today)
        platform.clearNotificationUnless(
            if (usable(todayPlan, today, todayCount) && !ReminderRules.isSleeping(now.toLocalTime(), config)) today else null,
            todayCount, if (usable(todayPlan, today, todayCount)) HydrationRules.attemptAmount(todayPlan!!.goalMl, todayPlan.attempts, todayCount) else null)
        val delivered = state.deliveredSlots
        val next = (0..7).asSequence().flatMap { offset ->
            val date = today.plusDays(offset.toLong())
            val plan = plans.plan(date)
            val count = plans.completed(date)
            if (!usable(plan, date, count)) emptySequence()
            else ReminderRules.slots(date, plan!!.attempts, config, now.zone).drop(count).asSequence()
        }.filter {
            val millis = it.toInstant().toEpochMilli()
            it.toInstant() > now.toInstant() && slotKey(it) !in delivered &&
                (state.lastAt == 0L || millis - state.lastAt >= COOLDOWN_MS)
        }.minByOrNull { it.toInstant() }?.toInstant()?.toEpochMilli() ?: 0L
        state.nextAt = next
        if (next == 0L) platform.cancelAlarm() else platform.schedule(next)
    }

    fun clockChanged() {
        state.lastAt = 0
        reconcile()
    }

    fun deliver(expectedAt: Long) {
        val now = currentTime()
        val millis = now.toInstant().toEpochMilli()
        if (expectedAt <= 0 || expectedAt != state.nextAt) return
        if (millis < expectedAt) { platform.schedule(expectedAt); return }
        state.nextAt = 0
        val config = state.read()
        val date = now.toLocalDate()
        val plan = plans.plan(date)
        val count = plans.completed(date)
        if (config.enabled && config.sleepConfigured && ReminderRules.valid(config) && platform.allowed() &&
            !ReminderRules.isSleeping(now.toLocalTime(), config) && usable(plan, date, count) &&
            (state.lastAt == 0L || millis - state.lastAt >= COOLDOWN_MS)) {
            val slot = ReminderRules.slots(date, plan!!.attempts, config, now.zone).drop(count)
                .firstOrNull { it.toInstant().toEpochMilli() == expectedAt }
            if (slot != null && slotKey(slot) !in state.deliveredSlots &&
                platform.show(HydrationRules.attemptAmount(plan.goalMl, plan.attempts, count), date, count)) {
                state.recordDelivery(slotKey(slot), millis)
            }
        }
        reconcile()
    }
}
