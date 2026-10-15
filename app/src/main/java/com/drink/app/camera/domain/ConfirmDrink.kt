package com.drink.app.camera.domain

import com.drink.app.hydration.model.HomeState
import java.time.Instant
import java.time.ZoneId

data class PendingDrink(val token: String, val detectedAt: Long, val shown: HomeState)

class ConfirmDrink(private val persist: (PendingDrink, Int, Boolean, Long, ZoneId) -> Boolean) {
    operator fun invoke(candidate: PendingDrink, amount: Int, now: Long, zone: ZoneId): Boolean {
        require(candidate.token.isNotBlank() && candidate.token.length <= 128)
        require(amount in 1..100000)
        check(candidate.shown.plan != null) { "Set a plan first" }
        val date = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        check(candidate.shown.date == date && Instant.ofEpochMilli(candidate.detectedAt).atZone(zone).toLocalDate() == date) {
            "Day changed; review a new suggestion"
        }
        return persist(candidate, amount, candidate.shown.nextAmountMl == amount, now, zone)
    }
}
