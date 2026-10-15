package com.drink.app.hydration.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.drink.app.hydration.domain.RecordAttempt
import com.drink.app.hydration.model.HomeState
import com.drink.app.hydration.model.PlanRequest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*

@RunWith(AndroidJUnit4::class)
class AttemptRecordingTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val date = LocalDate.now()
    private val timestamp get() = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private fun record(store: HydrationStore, shown: HomeState) = RecordAttempt { plan, count, time ->
        store.recordAttempt(plan.date, time, expectedCompleted = count, expectedPlan = plan)
    }(shown, timestamp)
    private fun isolated(name: String, test: (HydrationStore) -> Unit) {
        context.deleteDatabase(name)
        try { HydrationStore(context, name).use(test) } finally { context.deleteDatabase(name) }
    }

    @Test fun fullPlanRecordsExactlyOncePerAcceptedActionAndPersists() = isolated("attempt-full-test.db") { store ->
        store.configurePlan(PlanRequest(2000, 10, 1), date)
        repeat(10) { index ->
            val shown = store.readToday(date)
            assertEquals(200, shown.nextAmountMl)
            record(store, shown)
            val saved = store.readToday(date)
            assertEquals(index + 1, saved.recordedAttempts)
            assertEquals((index + 1) * 200L, saved.totalMl)
            assertEquals(2000L - saved.totalMl, saved.remainingMl)
        }
        val finished = store.readToday(date)
        assertNull(finished.nextAmountMl)
        assertEquals(0, finished.remainingAttempts)
        assertTrue(runCatching { record(store, finished) }.isFailure)
        assertTrue(runCatching { store.recordAttempt(date, timestamp) }.isFailure)
        assertEquals(10, store.readDay(date).entries.size)
        HydrationStore(context, "attempt-full-test.db").use { reopened ->
            assertEquals(finished, reopened.readToday(date))
        }
    }

    @Test fun staleCountAndChangedPlanCannotRecordAnUnshownServing() = isolated("attempt-stale-test.db") { store ->
        store.configurePlan(PlanRequest(2000, 10, 1), date)
        val oldPlan = store.readToday(date)
        store.configurePlan(PlanRequest(1001, 3, 1), date)
        assertTrue(runCatching { record(store, oldPlan) }.isFailure)
        val shown = store.readToday(date)
        record(store, shown)
        HydrationStore(context, "attempt-stale-test.db").use { otherConnection ->
            assertTrue(runCatching { record(otherConnection, shown) }.isFailure)
        }
        assertEquals(listOf(334), store.readDay(date).entries.map { it.amountMl })
        assertEquals(1, store.readToday(date).recordedAttempts)
    }

    @Test fun failedInsertLeavesNoIntakeOrLockAndRetryRecordsOne() = isolated("attempt-failure-test.db") { store ->
        store.configurePlan(PlanRequest(2000, 10, 1), date)
        val shown = store.readToday(date)
        store.writableDatabase.execSQL("CREATE TRIGGER fail_intake BEFORE INSERT ON intake BEGIN SELECT RAISE(ABORT, 'test write failure'); END")
        assertTrue(runCatching { record(store, shown) }.isFailure)
        assertEquals(shown, store.readToday(date))
        assertFalse(store.readToday(date).hasRecords)
        store.writableDatabase.execSQL("DROP TRIGGER fail_intake")
        record(store, shown)
        assertEquals(200L, store.readToday(date).totalMl)
        assertEquals(1, store.readDay(date).entries.size)
        assertTrue(runCatching { store.configurePlan(PlanRequest(1500, 5, 1), date) }.isFailure)
    }

    @Test fun extrasLockPlanWithoutConsumingAttemptsAndRoundingStaysExact() = isolated("attempt-extra-test.db") { store ->
        store.configurePlan(PlanRequest(1001, 3, 1), date)
        listOf(0, -1, 100001).forEach { amount ->
            assertTrue(runCatching { store.addExtra(amount, date, timestamp) }.isFailure)
        }
        assertFalse(store.readToday(date).hasRecords)
        store.addExtra(100, date, timestamp)
        assertEquals(0, store.readToday(date).recordedAttempts)
        assertTrue(runCatching { store.configurePlan(PlanRequest(2000, 10, 1), date) }.isFailure)
        repeat(3) { record(store, store.readToday(date)) }
        assertEquals(listOf(100, 334, 334, 333), store.readDay(date).entries.map { it.amountMl })
        assertEquals(1101L, store.readToday(date).totalMl)
        store.addExtra(100, date, timestamp)
        assertEquals(1201L, store.readToday(date).totalMl)
        assertEquals(3, store.readToday(date).recordedAttempts)
        assertEquals(0L, store.readToday(date).remainingMl)
        assertTrue(runCatching { store.addExtra(100, date, timestamp + 86_400_000) }.isFailure)
        assertEquals(5, store.readDay(date).entries.size)
    }
}
