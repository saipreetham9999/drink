package com.drink.app.reminders.scheduling

import android.content.Context
import com.drink.app.hydration.data.HydrationStore
import com.drink.app.reminders.data.ReminderStore
import com.drink.app.reminders.domain.ReminderEngine
import com.drink.app.reminders.domain.ReminderPlans
import com.drink.app.reminders.domain.ReminderRules
import com.drink.app.reminders.model.ReminderConfig
import com.drink.app.reminders.notification.ReminderNotifier
import java.time.LocalDate
import java.time.ZoneId

/** Serializes configuration, recovery and alarm delivery across Android callbacks. */
object ReminderScheduler {
    const val ACTION = "com.drink.app.REMIND_WATER"
    const val TRIGGER = "trigger_at"

    @Synchronized fun configure(context: Context, config: ReminderConfig) {
        require(ReminderRules.valid(config))
        if (config.enabled) {
            require(config.sleepConfigured)
            HydrationStore(context).use { db ->
                val today = LocalDate.now()
                (0..6).forEach { offset ->
                    db.readPlan(today.plusDays(offset.toLong()))?.let { plan ->
                        require(ReminderRules.slots(plan.date, plan.attempts, config, ZoneId.systemDefault()).size == plan.attempts)
                    }
                }
            }
            ReminderNotifier.createChannel(context)
        }
        ReminderStore(context).save(config)
        reconcile(context)
    }

    private fun withEngine(context: Context, action: (ReminderEngine) -> Unit) {
        HydrationStore(context).use { db ->
            val plans = object : ReminderPlans {
                override fun plan(date: LocalDate) = db.readPlan(date)
                override fun completed(date: LocalDate) = db.recordedAttempts(date)
            }
            action(ReminderEngine(ReminderStore(context), plans, AndroidReminderPlatform(context)))
        }
    }

    @Synchronized fun reconcile(context: Context) = withEngine(context) { it.reconcile() }
    @Synchronized fun clockChanged(context: Context) = withEngine(context) { it.clockChanged() }
    @Synchronized fun deliver(context: Context, expectedAt: Long) = withEngine(context) { it.deliver(expectedAt) }
}
