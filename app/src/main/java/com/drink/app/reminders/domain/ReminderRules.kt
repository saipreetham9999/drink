package com.drink.app.reminders.domain

import com.drink.app.hydration.model.DailyPlan
import com.drink.app.reminders.model.ReminderConfig
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object ReminderRules {
    fun valid(config: ReminderConfig): Boolean = config.sleepTime != config.wakeTime
    fun parseTime(value: String): LocalTime? = if (value.matches(Regex("\\d{2}:\\d{2}")))
        runCatching { LocalTime.parse(value) }.getOrNull() else null

    fun isSleeping(time: LocalTime, config: ReminderConfig): Boolean =
        if (config.sleepTime < config.wakeTime) time >= config.sleepTime && time < config.wakeTime
        else time >= config.sleepTime || time < config.wakeTime

    /** Waking periods within one calendar day; also supports daytime sleeping. */
    private fun wakingPeriods(date: LocalDate, config: ReminderConfig, zone: ZoneId): List<Pair<ZonedDateTime, ZonedDateTime>> {
        require(valid(config))
        val start = date.atStartOfDay(zone)
        val end = date.plusDays(1).atStartOfDay(zone)
        val sleep = date.atTime(config.sleepTime).atZone(zone)
        val wake = date.atTime(config.wakeTime).atZone(zone)
        return (if (config.wakeTime < config.sleepTime) listOf(wake to sleep)
            else listOf(start to sleep, wake to end)).filter { it.second.toInstant() > it.first.toInstant() }
    }

    fun slots(date: LocalDate, attempts: Int, config: ReminderConfig, zone: ZoneId): List<ZonedDateTime> {
        if (!valid(config) || attempts !in 1..32) return emptyList()
        val periods = wakingPeriods(date, config, zone)
        val durations = periods.map { Duration.between(it.first, it.second).seconds }
        val total = durations.sum()
        if (total / (attempts + 1) < 15 * 60) return emptyList()
        return (1..attempts).map { index ->
            var offset = total * index / (attempts + 1)
            var result: ZonedDateTime? = null
            for ((periodIndex, period) in periods.withIndex()) {
                if (offset < durations[periodIndex]) { result = period.first.plusSeconds(offset); break }
                offset -= durations[periodIndex]
            }
            checkNotNull(result)
        }.filter { !isSleeping(it.toLocalTime(), config) }.distinctBy { it.toInstant() }
    }

    fun supports(date: LocalDate, attempts: Int, config: ReminderConfig): Boolean =
        (0..6).all { slots(date.plusDays(it.toLong()), attempts, config, ZoneId.systemDefault()).size == attempts }

    fun nextSlot(now: ZonedDateTime, plan: DailyPlan, completed: Int, config: ReminderConfig): ZonedDateTime? =
        slots(plan.date, plan.attempts, config, now.zone).drop(completed.coerceAtLeast(0))
            .firstOrNull { it.toInstant() > now.toInstant() }
}
