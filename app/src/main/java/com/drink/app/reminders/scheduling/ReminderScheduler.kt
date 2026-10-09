package com.drink.app.reminders.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.drink.app.hydration.data.HydrationStore
import com.drink.app.hydration.domain.HydrationRules
import com.drink.app.reminders.data.ReminderStore
import com.drink.app.reminders.domain.ReminderRules
import com.drink.app.reminders.model.ReminderConfig
import com.drink.app.reminders.notification.ReminderNotifier
import java.time.Instant
import java.time.ZonedDateTime

object ReminderScheduler {
    const val ACTION = "com.drink.app.REMIND_WATER"
    const val TRIGGER = "trigger_at"
    private fun pending(context: Context, trigger: Long = 0) = PendingIntent.getBroadcast(
        context, 100, Intent(context, ReminderReceiver::class.java).setAction(ACTION).putExtra(TRIGGER, trigger),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    @Synchronized fun configure(context: Context, config: ReminderConfig) {
        require(ReminderRules.valid(config))
        HydrationStore(context).use { db ->
            val today = java.time.LocalDate.now()
            (0..6).forEach { offset ->
                db.readPlan(today.plusDays(offset.toLong()))?.let { plan ->
                    require(ReminderRules.slots(plan.date, plan.attempts, config, java.time.ZoneId.systemDefault()).size == plan.attempts)
                }
            }
        }
        if (config.enabled) ReminderNotifier.createChannel(context)
        ReminderStore(context).save(config)
        reconcile(context)
    }

    @Synchronized fun reconcile(context: Context, reset: Boolean = false) {
        val store = ReminderStore(context)
        val config = store.read()
        val now = ZonedDateTime.now()
        val next = if (config.enabled && config.sleepConfigured && ReminderRules.valid(config) && ReminderNotifier.allowed(context)) {
            HydrationStore(context).use { db ->
                (0..7).mapNotNull { offset ->
                    db.readPlan(now.toLocalDate().plusDays(offset.toLong()))?.let {
                        ReminderRules.nextSlot(now, it, db.recordedAttempts(it.date), config)
                    }
                }.minByOrNull { it.toInstant() }?.toInstant()?.toEpochMilli() ?: 0L
            }
        } else 0L
        val alarms = context.getSystemService(AlarmManager::class.java)
        store.nextAt = next
        if (next == 0L) {
            alarms.cancel(pending(context))
            ReminderNotifier.cancel(context)
        } else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending(context, next))
    }

    @Synchronized fun clockChanged(context: Context) {
        ReminderStore(context).lastAt = 0
        reconcile(context)
    }

    @Synchronized fun deliver(context: Context, expectedAt: Long) {
        val store = ReminderStore(context)
        val now = ZonedDateTime.now()
        val millis = now.toInstant().toEpochMilli()
        if (expectedAt <= 0 || expectedAt != store.nextAt || millis < expectedAt) return
        store.nextAt = 0
        val config = store.read()
        if (config.enabled && config.sleepConfigured && ReminderNotifier.allowed(context) &&
            !ReminderRules.isSleeping(now.toLocalTime(), config) &&
            Instant.ofEpochMilli(expectedAt).atZone(now.zone).toLocalDate() == now.toLocalDate() &&
            (store.lastAt == 0L || millis - store.lastAt >= 15 * 60_000L)) {
            HydrationStore(context).use { db ->
                db.readPlan(now.toLocalDate())?.let { plan ->
                    val count = db.recordedAttempts(plan.date)
                    if (count < plan.attempts) {
                        store.lastAt = millis
                        ReminderNotifier.show(context, HydrationRules.attemptAmount(plan.goalMl, plan.attempts, count))
                    }
                }
            }
        }
        reconcile(context)
    }
}
