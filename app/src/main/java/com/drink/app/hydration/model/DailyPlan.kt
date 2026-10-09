package com.drink.app.hydration.model

import java.time.LocalDate

data class DailyPlan(val date: LocalDate, val goalMl: Int, val attempts: Int, val endsOn: LocalDate)
data class PlanRequest(val goalMl: Int, val attempts: Int, val days: Int)
