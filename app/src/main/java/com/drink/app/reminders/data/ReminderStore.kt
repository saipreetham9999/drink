package com.drink.app.reminders.data

import android.content.Context
import com.drink.app.hydration.data.HydrationStore
import com.drink.app.reminders.domain.ReminderRules
import com.drink.app.reminders.domain.ReminderState
import com.drink.app.reminders.model.ReminderConfig
import com.drink.app.reminders.model.SleepHours
import java.time.LocalTime

class ReminderStore(private val context: Context, name: String = "reminders") : ReminderState {
    private val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val databaseName = if (name == "reminders") "hydration.db" else "$name.db"
    override fun read(): ReminderConfig {
        val hours = HydrationStore(context, databaseName).use { it.readSleepHours() }
        val configured = hours != null || preferences.getBoolean("sleep_set", false)
        return ReminderConfig(
            enabled = configured && preferences.getBoolean("enabled", false),
            sleepConfigured = configured,
            sleepTime = hours?.sleepTime ?: ReminderRules.parseTime(preferences.getString("sleep", "22:00") ?: "22:00") ?: LocalTime.of(22, 0),
            wakeTime = hours?.wakeTime ?: ReminderRules.parseTime(preferences.getString("wake", "08:00") ?: "08:00") ?: LocalTime.of(8, 0),
        )
    }
    fun save(config: ReminderConfig) {
        require(ReminderRules.valid(config))
        if (config.sleepConfigured) HydrationStore(context, databaseName).use {
            it.saveSleepHours(SleepHours(config.sleepTime, config.wakeTime))
        }
        // Old sleep keys remain readable for upgrades; new writes use only the database.
        check(preferences.edit().putBoolean("enabled", config.enabled).commit())
    }
    override val deliveredSlots: Set<String>
        get() = preferences.getStringSet("delivered_slots", emptySet())?.toSet() ?: emptySet()

    override fun recordDelivery(key: String, deliveredAt: Long) {
        val date = java.time.LocalDate.parse(key.substringBefore('|'))
        val recent = (deliveredSlots + key).filter {
            val savedDate = runCatching { java.time.LocalDate.parse(it.substringBefore('|')) }.getOrNull()
            savedDate != null && savedDate >= date.minusDays(7) && savedDate <= date.plusDays(7)
        }.sorted().takeLast(256).toSet()
        check(preferences.edit().putLong("last", deliveredAt).putStringSet("delivered_slots", recent).commit())
    }

    override var nextAt: Long
        get() = preferences.getLong("next", 0)
        set(value) { check(preferences.edit().putLong("next", value).commit()) }
    override var lastAt: Long
        get() = preferences.getLong("last", 0)
        set(value) { check(preferences.edit().putLong("last", value).commit()) }
}
