package com.drink.app.reminders.model

import java.io.Serializable
import java.time.LocalTime

/** Sleeping hours are collected explicitly before showing plan setup. */
data class ReminderConfig(
    val enabled: Boolean = false,
    val sleepConfigured: Boolean = false,
    val sleepTime: LocalTime = LocalTime.of(22, 0),
    val wakeTime: LocalTime = LocalTime.of(8, 0),
) : Serializable
