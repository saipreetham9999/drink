package com.drink.app.reminders.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.drink.app.reminders.model.ReminderConfig
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class ReminderStoreTest {
    @Test fun preferencesPersistSeparatelyFromHydration() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "reminder-preferences-test"
        context.deleteSharedPreferences(name)
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
        } finally { context.deleteSharedPreferences(name) }
    }
}

