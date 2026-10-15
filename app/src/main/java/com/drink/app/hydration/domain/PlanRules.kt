package com.drink.app.hydration.domain

import com.drink.app.hydration.model.PlanRequest
import com.drink.app.reminders.domain.ReminderRules
import com.drink.app.reminders.model.ReminderConfig
import java.time.LocalDate
import java.time.ZoneId

/** Used by both setup preview and the save action. */
object PlanRules {
    fun valid(request: PlanRequest, config: ReminderConfig, date: LocalDate,
        zone: ZoneId = ZoneId.systemDefault()): Boolean =
        HydrationRules.validPlan(request.goalMl, request.attempts) && request.days in listOf(1, 7) &&
            config.sleepConfigured && ReminderRules.valid(config) &&
            (0 until request.days).all {
                ReminderRules.slots(date.plusDays(it.toLong()), request.attempts, config, zone).size == request.attempts
            }
}
