package com.drink.app.hydration.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.drink.app.hydration.domain.HydrationRules
import com.drink.app.hydration.model.PlanRequest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*

@RunWith(AndroidJUnit4::class)
class DayRolloverTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val zone = ZoneId.of("America/Chicago")
    private fun isolated(name: String, action: (HydrationStore) -> Unit) {
        context.deleteDatabase(name)
        try { HydrationStore(context, name).use(action) } finally { context.deleteDatabase(name) }
    }

    @Test fun weeklyPlanCoversSevenDatesWithoutExtendingWhenReopened() = isolated("rollover-week-test.db") { store ->
        val start = LocalDate.of(2026, 10, 14)
        store.configurePlan(PlanRequest(2000, 10, 7), start)
        store.recordAttempt(start, start.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(), zone = zone)
        // Skip several dates without opening the app.
        HydrationStore(context, "rollover-week-test.db").use { reopened ->
            for (offset in 1L..6L) {
                val home = reopened.readToday(start.plusDays(offset))
                assertEquals(0L, home.totalMl)
                assertEquals(0, home.recordedAttempts)
                assertFalse(home.hasRecords)
                assertEquals(2000, home.goalMl)
                assertEquals(10, home.remainingAttempts)
                assertEquals(start.plusDays(6), home.plan?.endsOn)
            }
            assertNull(reopened.readPlan(start.minusDays(1)))
            val expired = reopened.readToday(start.plusDays(7))
            assertNull(expired.plan)
            assertNull(expired.nextAmountMl)
            assertEquals(0L, expired.totalMl)
            assertEquals(0, expired.recordedAttempts)
            assertEquals(200L, reopened.readDay(start).summary.totalMl)
            reopened.configurePlan(PlanRequest(1500, 5, 1), expired.date)
            assertEquals(2000, reopened.readDay(start).summary.goalMl)
            assertEquals(1500, reopened.readToday(expired.date).goalMl)
        }
    }

    @Test fun midnightAndDstBoundariesSeparateDaysAndRejectOldActions() = isolated("rollover-boundary-test.db") { store ->
        for (day in listOf(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 10, 14), LocalDate.of(2026, 11, 1))) {
            val nextDay = day.plusDays(1)
            val boundary = HydrationRules.dayBounds(day, zone).second
            store.configurePlan(PlanRequest(2000, 10, 7), day)
            store.recordAttempt(day, boundary - 1, zone = zone)
            assertEquals(200L, store.readToday(day).totalMl)
            assertEquals(0L, store.readToday(nextDay).totalMl)
            assertTrue(runCatching { store.recordAttempt(day, boundary, zone = zone) }.isFailure)
            assertTrue(runCatching { store.addExtra(100, day, boundary, zone) }.isFailure)
            store.recordAttempt(nextDay, boundary, zone = zone)
            assertEquals(1, store.readToday(nextDay).recordedAttempts)
            assertEquals(200L, store.readToday(nextDay).totalMl)
            assertEquals(200L, store.readDay(day).summary.totalMl)
            assertEquals(2000, store.readDay(day).summary.goalMl)
            assertEquals(boundary - 1, store.readDay(day).entries.single().recordedAt)
        }
    }

    @Test fun dayReadsDoNotCreateOrChangeSnapshots() = isolated("rollover-readonly-test.db") { store ->
        val start = LocalDate.of(2026, 10, 14)
        store.configurePlan(PlanRequest(2000, 10, 7), start)
        fun changes(): Long = store.readableDatabase.rawQuery("SELECT total_changes()", null).use { it.moveToFirst(); it.getLong(0) }
        val before = changes()
        repeat(2) { for (offset in 0L..8L) store.readToday(start.plusDays(offset)) }
        assertEquals(before, changes())
        store.readableDatabase.rawQuery("SELECT COUNT(*) FROM daily_plans", null).use {
            it.moveToFirst(); assertEquals(1, it.getInt(0))
        }
        assertNull(store.readDay(start.plusDays(1)).summary.goalMl)
    }

    @Test fun travelChangesTodayWithoutMovingExistingRecords() = isolated("rollover-zone-test.db") { store ->
        val instant = Instant.parse("2026-10-15T04:30:00Z")
        val tokyo = ZoneId.of("Asia/Tokyo")
        val firstDate = instant.atZone(zone).toLocalDate()
        val secondDate = instant.atZone(tokyo).toLocalDate()
        assertNotEquals(firstDate, secondDate)
        store.configurePlan(PlanRequest(2000, 10, 7), firstDate)
        store.recordAttempt(firstDate, instant.toEpochMilli(), zone = zone)
        assertEquals(0L, store.readToday(secondDate).totalMl)
        assertTrue(runCatching { store.recordAttempt(firstDate, instant.toEpochMilli(), zone = tokyo) }.isFailure)
        store.recordAttempt(secondDate, instant.toEpochMilli(), zone = tokyo)
        assertEquals(200L, store.readDay(firstDate).summary.totalMl)
        assertEquals(200L, store.readDay(secondDate).summary.totalMl)
        assertEquals(1, store.readToday(firstDate).recordedAttempts)
        assertEquals(listOf(firstDate), store.readHistory(secondDate).map { it.date })
    }

    @Test fun todayOnlyPlanExpiresAtNextLocalDate() = isolated("rollover-today-test.db") { store ->
        val date = LocalDate.of(2026, 10, 14)
        store.configurePlan(PlanRequest(2000, 10, 1), date)
        assertNotNull(store.readToday(date).plan)
        assertNull(store.readToday(date.plusDays(1)).plan)
        assertTrue(store.readDay(date).entries.isEmpty())
    }
}
