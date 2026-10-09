package com.drink.app.reminders.data

import android.content.Context
import com.drink.app.reminders.domain.ReminderRules
import com.drink.app.reminders.model.ReminderConfig
import java.time.LocalTime

class ReminderStore(context: Context, name: String = "reminders") {
    private val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    fun read(): ReminderConfig {
        val configured = preferences.getBoolean("sleep_set", false)
        return ReminderConfig(
            enabled = configured && preferences.getBoolean("enabled", false),
            sleepConfigured = configured,
            sleepTime = ReminderRules.parseTime(preferences.getString("sleep", "22:00") ?: "22:00") ?: LocalTime.of(22, 0),
            wakeTime = ReminderRules.parseTime(preferences.getString("wake", "08:00") ?: "08:00") ?: LocalTime.of(8, 0),
        )
    }
    fun save(config: ReminderConfig) {
        require(ReminderRules.valid(config))
        check(preferences.edit().putBoolean("enabled", config.enabled).putBoolean("sleep_set", config.sleepConfigured)
            .putString("sleep", config.sleepTime.toString()).putString("wake", config.wakeTime.toString()).commit())
    }
    var nextAt: Long
        get() = preferences.getLong("next", 0)
        set(value) { check(preferences.edit().putLong("next", value).commit()) }
    var lastAt: Long
        get() = preferences.getLong("last", 0)
        set(value) { check(preferences.edit().putLong("last", value).commit()) }
}
