package com.drink.app.hydration.model

import java.time.LocalDate

data class HomeState(
    val date: LocalDate = LocalDate.now(), val totalMl: Long = 0, val goalMl: Int = 0,
    val plan: DailyPlan? = null, val recordedAttempts: Int = 0, val hasRecords: Boolean = false,
)
