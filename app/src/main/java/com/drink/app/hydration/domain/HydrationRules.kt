package com.drink.app.hydration.domain

import java.time.LocalDate
import java.time.ZoneId


object HydrationRules {
    fun validPlan(goalMl: Int, attempts: Int): Boolean = goalMl in 1..100000 && attempts in 1..32 && goalMl >= attempts
    fun attemptAmount(goalMl: Int, attempts: Int, index: Int): Int {
        require(validPlan(goalMl, attempts) && index in 0 until attempts)
        return goalMl / attempts + if (index < goalMl % attempts) 1 else 0
    }
    fun completionPercent(totalMl: Long, goalMl: Int): Long =
        if (goalMl <= 0) 0 else (totalMl.toDouble() * 100 / goalMl).toLong()
    fun validAmount(value: String): Int? = value.toIntOrNull()?.takeIf { it in 1..100000 }
    fun progress(totalMl: Long, goalMl: Int): Float =
        if (goalMl <= 0) 0f else (totalMl.toDouble() / goalMl).coerceIn(0.0, 1.0).toFloat()
    fun dayBounds(date: LocalDate, zone: ZoneId): Pair<Long, Long> =
        date.atStartOfDay(zone).toInstant().toEpochMilli() to
            date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
}
