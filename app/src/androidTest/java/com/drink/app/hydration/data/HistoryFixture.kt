package com.drink.app.hydration.data

import com.drink.app.hydration.model.PlanRequest
import java.time.*

internal object HistoryFixture {
    val today: LocalDate = LocalDate.of(2026, 10, 14)
    val recent: LocalDate = today.minusDays(1)
    val older: LocalDate = today.minusDays(2)
    val legacy: LocalDate = today.minusDays(5)
    fun timestamp(date: LocalDate, hour: Int = 12): Long = date.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    fun seed(store: HydrationStore) {
        store.writableDatabase.execSQL("INSERT INTO intake(recorded_at, amount_ml, local_date) VALUES (?, 300, ?)",
            arrayOf<Any>(timestamp(legacy), legacy.toString()))
        store.configurePlan(PlanRequest(2000, 10, 1), today.minusDays(3)) // No intake: omit from History.
        store.configurePlan(PlanRequest(1500, 3, 1), older)
        store.recordAttempt(older, timestamp(older))
        store.addExtra(100, older, timestamp(older, 8)) // Earlier time, later insertion.
        store.configurePlan(PlanRequest(1001, 3, 1), recent)
        repeat(3) { store.recordAttempt(recent, timestamp(recent)) } // Tie timestamps sort by ID.
        store.addExtra(100, recent, timestamp(recent) + 1)
        store.configurePlan(PlanRequest(2000, 10, 1), today)
        store.recordAttempt(today, timestamp(today))
        store.configurePlan(PlanRequest(3000, 10, 1), today.plusDays(1))
        store.recordAttempt(today.plusDays(1), timestamp(today.plusDays(1)))
        store.configurePlan(PlanRequest(5000, 10, 7), today.plusDays(2))
    }
}
