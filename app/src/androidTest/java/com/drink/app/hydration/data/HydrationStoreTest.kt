package com.drink.app.hydration.data

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.drink.app.hydration.model.PlanRequest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*

@RunWith(AndroidJUnit4::class)
class HydrationStoreTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun stamp(date: LocalDate) = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private fun rejects(action: () -> Unit) {
        assertTrue(runCatching(action).isFailure)
    }
    @Test fun recordingLocksPlanAndExtrasPreserveAttemptsAndHistory() {
        val name = "plan-lock-test.db"
        context.deleteDatabase(name)
        val day = LocalDate.now().minusDays(1)
        try {
            HydrationStore(context, name).use { store ->
                store.configurePlan(PlanRequest(2000, 10, 1), day)
                store.configurePlan(PlanRequest(1001, 3, 1), day)
                repeat(3) { store.recordAttempt(day, stamp(day) + it) }
                rejects { store.recordAttempt(day, stamp(day) + 4) }
                rejects { store.configurePlan(PlanRequest(1500, 5, 1), day) }
                store.addExtra(100, day, stamp(day) + 5)
                assertEquals(3, store.readToday(day).recordedAttempts)
                assertEquals(1101L, store.readToday(day).totalMl)
                assertEquals(listOf(334, 334, 333, 100), store.readDay(day).entries.map { it.amountMl })
                assertEquals(1001, store.readHistory().single().goalMl)
            }
            HydrationStore(context, name).use { assertEquals(1101L, it.readDay(day).summary.totalMl) }
        } finally { context.deleteDatabase(name) }
    }
    @Test fun weeklyRolloverExpiryAndExtraFirstLocksPlan() {
        val name = "plan-week-test.db"
        context.deleteDatabase(name)
        val day = LocalDate.now()
        try {
            HydrationStore(context, name).use { store ->
                store.configurePlan(PlanRequest(2000, 10, 7), day)
                store.addExtra(100, day, stamp(day))
                rejects { store.configurePlan(PlanRequest(1000, 5, 1), day) }
                assertEquals(0, store.readToday(day).recordedAttempts)
                assertEquals(0L, store.readToday(day.plusDays(1)).totalMl)
                assertEquals(2000, store.readPlan(day.plusDays(6))?.goalMl)
                assertNull(store.readPlan(day.plusDays(7)))
                store.configurePlan(PlanRequest(1500, 5, 1), day.plusDays(1))
                assertEquals(2000, store.readDay(day).summary.goalMl)
                assertNull(store.readPlan(day.plusDays(2)))
                rejects { store.recordAttempt(day, stamp(day.plusDays(1))) }
            }
        } finally { context.deleteDatabase(name) }
    }
    @Test fun versionTwoUpgradeKeepsCurrentAndPastGoals() {
        val name = "plan-v2-migration-test.db"
        context.deleteDatabase(name)
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        try {
            context.openOrCreateDatabase(name, 0, null).use { db ->
                db.execSQL("CREATE TABLE intake (id INTEGER PRIMARY KEY, recorded_at INTEGER NOT NULL, amount_ml INTEGER NOT NULL, local_date TEXT NOT NULL)")
                db.execSQL("CREATE TABLE preferences (id INTEGER PRIMARY KEY, goal_ml INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE daily_goals (local_date TEXT PRIMARY KEY, goal_ml INTEGER NOT NULL)")
                db.execSQL("INSERT INTO preferences VALUES (1, 2000)")
                db.execSQL("INSERT INTO daily_goals VALUES (?, 1800)", arrayOf(today.toString()))
                db.execSQL("INSERT INTO daily_goals VALUES (?, 1500)", arrayOf(yesterday.toString()))
                db.execSQL("INSERT INTO intake VALUES (1, ?, 250, ?)", arrayOf(stamp(today), today.toString()))
                db.execSQL("INSERT INTO intake VALUES (2, ?, 100, ?)", arrayOf(stamp(yesterday), yesterday.toString()))
                db.version = 2
            }
            HydrationStore(context, name).use { store ->
                val home = store.readToday()
                assertEquals(1800, home.goalMl)
                assertEquals(250L, home.totalMl)
                assertEquals(0, home.recordedAttempts)
                assertTrue(home.hasRecords)
                assertEquals(today, home.plan?.endsOn)
                assertEquals(1500, store.readHistory().single().goalMl)
                rejects { store.configurePlan(PlanRequest(2000, 10, 1)) }
            }
        } finally { context.deleteDatabase(name) }
    }
    @Test fun migrationPreservesOldRecordsAndUnknownPastGoals() {
        val name = "plan-migration-test.db"
        context.deleteDatabase(name)
        val day = LocalDate.now().minusDays(1)
        try {
            context.openOrCreateDatabase(name, 0, null).use { db ->
                db.execSQL("CREATE TABLE intake (id INTEGER PRIMARY KEY, recorded_at INTEGER NOT NULL, amount_ml INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE preferences (id INTEGER PRIMARY KEY, goal_ml INTEGER NOT NULL)")
                db.execSQL("INSERT INTO preferences VALUES (1, 1750)")
                db.execSQL("INSERT INTO intake VALUES (1, ?, 300)", arrayOf(stamp(day)))
                db.version = 1
            }
            HydrationStore(context, name).use { store ->
                assertNull(store.readToday().plan)
                assertNull(store.readHistory().single().goalMl)
                assertEquals(300L, store.readDay(day).summary.totalMl)
                assertEquals(stamp(day), store.readDay(day).entries.single().recordedAt)
            }
        } finally { context.deleteDatabase(name) }
    }
}

