package com.drink.app.hydration.domain

import com.drink.app.hydration.model.PlanRequest
import com.drink.app.reminders.model.ReminderConfig
import com.drink.app.reminders.model.SleepHours
import java.time.LocalDate
import java.time.ZonedDateTime

/** One explicit action; persistence commits plan and sleeping hours together. */
class SavePlan(private val persist: (PlanRequest, SleepHours, LocalDate) -> Unit) {
    operator fun invoke(request: PlanRequest, config: ReminderConfig, expectedDate: LocalDate,
        now: ZonedDateTime = ZonedDateTime.now()) {
        check(expectedDate == now.toLocalDate()) { "The day changed. Review today's plan." }
        require(PlanRules.valid(request, config, expectedDate, now.zone)) { "Invalid water plan or sleeping hours" }
        persist(request, SleepHours(config.sleepTime, config.wakeTime), expectedDate)
    }
}
