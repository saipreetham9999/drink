package com.drink.app.hydration.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.drink.app.hydration.domain.HydrationRules
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryVerificationTest {
    @Test fun historyKeepsRecordedDatesOriginalGoalsAndExtrasAfterReopening() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "history-verification-test.db"
        context.deleteDatabase(name)
        try {
            HydrationStore(context, name).use(HistoryFixture::seed)
            HydrationStore(context, name).use { store ->
                val days = store.readHistory(HistoryFixture.today)
                assertEquals(listOf(HistoryFixture.recent, HistoryFixture.older, HistoryFixture.legacy), days.map { it.date })
                assertEquals(listOf(1101L, 600L, 300L), days.map { it.totalMl })
                assertEquals(listOf(1001, 1500, null), days.map { it.goalMl })
                assertEquals(109L, HydrationRules.completionPercent(days.first().totalMl, days.first().goalMl!!))
                val detail = store.readDay(HistoryFixture.recent)
                assertEquals(days.first(), detail.summary)
                assertEquals(listOf(334, 334, 333, 100), detail.entries.map { it.amountMl })
                assertEquals(detail.entries.map { it.id }.sorted(), detail.entries.map { it.id })
                assertEquals(listOf(100, 500), store.readDay(HistoryFixture.older).entries.map { it.amountMl })
                assertEquals(5000, store.readPlan(HistoryFixture.today.plusDays(2))?.goalMl)
                assertNull(store.readDay(HistoryFixture.legacy).summary.goalMl)
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun openingHistoryAndDetailsNeverWritesOrRelocatesRecords() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "history-readonly-test.db"
        context.deleteDatabase(name)
        try {
            HydrationStore(context, name).use { store ->
                assertTrue(store.readHistory(HistoryFixture.today).isEmpty())
                HistoryFixture.seed(store)
                fun changes(): Long = store.readableDatabase.rawQuery("SELECT total_changes()", null).use { it.moveToFirst(); it.getLong(0) }
                val before = changes()
                val original = store.readDay(HistoryFixture.recent)
                repeat(3) { store.readHistory(HistoryFixture.today).forEach { store.readDay(it.date) } }
                // A different local Today changes visibility, not the stored record date/time.
                store.readHistory(HistoryFixture.recent)
                store.readHistory(HistoryFixture.today.plusDays(10))
                assertEquals(before, changes())
                assertEquals(original, store.readDay(HistoryFixture.recent))
                assertEquals(200L, store.readToday(HistoryFixture.today).totalMl)
            }
        } finally { context.deleteDatabase(name) }
    }
}
