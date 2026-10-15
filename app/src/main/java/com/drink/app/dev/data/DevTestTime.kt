package com.drink.app.dev.data

import android.content.Context
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Uses the sandbox calendar without changing the device clock. */
class DevTestTime(context: Context, databaseName: String) {
    private val preferences = context.getSharedPreferences(databaseName, Context.MODE_PRIVATE)
    fun now(): Long {
        val date = runCatching { LocalDate.parse(preferences.getString("date", null)) }.getOrDefault(LocalDate.now())
        return date.atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
