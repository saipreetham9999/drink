package com.drink.app.hydration.domain

import com.drink.app.hydration.model.DailyPlan
import com.drink.app.hydration.model.HomeState
import java.time.Instant
import java.time.ZoneId

/** Records only the serving the user was shown; storage rechecks the snapshot atomically. */
class RecordAttempt(private val persist: (DailyPlan, Int, Long) -> Unit) {
    operator fun invoke(shown: HomeState, recordedAt: Long, zone: ZoneId = ZoneId.systemDefault()) {
        val plan = checkNotNull(shown.plan) { "Set a daily plan first" }
        check(shown.date == plan.date && shown.date == Instant.ofEpochMilli(recordedAt).atZone(zone).toLocalDate()) {
            "The day changed. Review today's plan."
        }
        check(shown.nextAmountMl != null) { "All planned attempts are recorded" }
        persist(plan, shown.recordedAttempts, recordedAt)
    }
}
