package com.drink.app.hydration.model

import com.drink.app.hydration.domain.HydrationRules
import java.time.LocalDate

data class HomeState(
    val date: LocalDate = LocalDate.now(), val totalMl: Long = 0, val goalMl: Int = 0,
    val plan: DailyPlan? = null, val recordedAttempts: Int = 0, val hasRecords: Boolean = false,
) {
    val remainingMl: Long get() = HydrationRules.remainingMl(totalMl, goalMl)
    val remainingAttempts: Int get() = ((plan?.attempts ?: 0) - recordedAttempts).coerceAtLeast(0)
    val nextAmountMl: Int? get() = plan?.let {
        if (recordedAttempts in 0 until it.attempts)
            HydrationRules.attemptAmount(it.goalMl, it.attempts, recordedAttempts) else null
    }
}
