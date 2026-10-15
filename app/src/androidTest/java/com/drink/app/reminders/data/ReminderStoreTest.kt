package com.drink.app.reminders.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.drink.app.reminders.model.ReminderConfig
import com.drink.app.hydration.data.HydrationStore
import com.drink.app.reminders.model.SleepHours
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class ReminderStoreTest {
    @Test fun deliveryLedgerPersistsAndIsBounded() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "reminder-ledger-test"
        context.deleteSharedPreferences(name)
        try {
            val store = ReminderStore(context, name)
            repeat(270) { store.recordDelivery("2026-10-14|test|$it", it + 1L) }
            val reopened = ReminderStore(context, name)
            assertEquals(256, reopened.deliveredSlots.size)
            assertEquals(270L, reopened.lastAt)
            reopened.recordDelivery("2026-11-14|test|later", 1000)
            assertEquals(setOf("2026-11-14|test|later"), reopened.deliveredSlots)
        } finally { context.deleteSharedPreferences(name) }
    }
    @Test fun preferencesAndCanonicalSleepingHoursPersist() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "reminder-preferences-test"
        context.deleteSharedPreferences(name)
        context.deleteDatabase("$name.db")
        try {
            assertFalse(ReminderStore(context, name).read().enabled)
            val config = ReminderConfig(true, true, LocalTime.of(23, 0), LocalTime.of(7, 0))
            val store = ReminderStore(context, name)
            store.save(config)
            store.nextAt = 1234
            store.lastAt = 1000
            val reopened = ReminderStore(context, name)
            assertEquals(config, reopened.read())
            assertEquals(1234L, reopened.nextAt)
            assertEquals(1000L, reopened.lastAt)
            reopened.save(config.copy(enabled = false))
            assertFalse(ReminderStore(context, name).read().enabled)
        } finally { context.deleteSharedPreferences(name); context.deleteDatabase("$name.db") }
    }

    @Test fun legacyHoursRemainReadableUntilSetupIsSaved() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "legacy-sleep-test"
        context.deleteSharedPreferences(name)
        context.deleteDatabase("$name.db")
        try {
            assertTrue(context.getSharedPreferences(name, 0).edit().putBoolean("sleep_set", true)
                .putBoolean("enabled", true).putString("sleep", "23:00").putString("wake", "07:00").commit())
            val store = ReminderStore(context, name)
            assertEquals(LocalTime.of(23, 0), store.read().sleepTime)
            assertTrue(store.read().enabled)
            HydrationStore(context, "$name.db").use { it.saveSleepHours(SleepHours(LocalTime.of(22, 0), LocalTime.of(8, 0))) }
            assertEquals(LocalTime.of(22, 0), store.read().sleepTime)
            assertEquals(LocalTime.of(8, 0), store.read().wakeTime)
            assertTrue(store.read().sleepConfigured)
        } finally { context.deleteSharedPreferences(name); context.deleteDatabase("$name.db") }
    }
}

