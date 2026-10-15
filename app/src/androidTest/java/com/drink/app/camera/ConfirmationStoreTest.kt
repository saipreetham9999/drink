package com.drink.app.camera

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.drink.app.camera.domain.PendingDrink
import com.drink.app.hydration.data.HydrationStore
import com.drink.app.hydration.model.PlanRequest
import com.drink.app.reminders.model.SleepHours
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class ConfirmationStoreTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val day = LocalDate.of(2026, 10, 14)
    private val zone = ZoneId.of("America/Chicago")
    private val now = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private val hours = SleepHours(LocalTime.of(22, 0), LocalTime.of(8, 0))
    private fun fixture(test: (HydrationStore, String) -> Unit) {
        val name = "confirmed-intake-test.db"
        context.deleteDatabase(name)
        try { HydrationStore(context, name).use { store ->
            store.savePlan(PlanRequest(2000, 10, 1), hours, day)
            test(store, name)
        } } finally { context.deleteDatabase(name) }
    }
    private fun candidate(store: HydrationStore, token: String = "candidate-1") = PendingDrink(token, now, store.readToday(day))
    private fun save(store: HydrationStore, candidate: PendingDrink, amount: Int = 200, planned: Boolean = true) =
        store.recordConfirmedIntake(candidate.token, candidate.shown, candidate.detectedAt, amount, planned, now, zone)

    @Test fun confirmedServingIsExactlyOnceIncludingReopening() = fixture { store, name ->
        val offered = candidate(store)
        assertEquals(0L, store.readToday(day).totalMl)
        assertTrue(save(store, offered))
        assertFalse(save(store, offered))
        HydrationStore(context, name).use { reopened ->
            assertFalse(save(reopened, offered))
            assertEquals(200L, reopened.readToday(day).totalMl)
            assertEquals(1, reopened.readToday(day).recordedAttempts)
        }
    }
    @Test fun correctionsAreExtrasAndPreservePlannedAllocation() = fixture { store, _ ->
        assertTrue(save(store, candidate(store), 150, false))
        assertEquals(0, store.readToday(day).recordedAttempts)
        assertEquals(200, store.readToday(day).nextAmountMl)
        assertTrue(save(store, candidate(store, "second")))
        assertEquals(350L, store.readToday(day).totalMl)
        assertEquals(1, store.readToday(day).recordedAttempts)
    }
    @Test fun staleSnapshotAndDayChangeCannotRecord() = fixture { store, _ ->
        val offered = candidate(store)
        store.recordAttempt(day, now, zone = zone)
        assertTrue(runCatching { save(store, offered) }.isFailure)
        assertEquals(200L, store.readToday(day).totalMl)
        val fresh = candidate(store, "fresh")
        assertTrue(runCatching { store.recordConfirmedIntake(fresh.token, fresh.shown, now, 200, true,
            day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), zone) }.isFailure)
        assertEquals(1, store.readDay(day).entries.size)
    }
    @Test fun failedInsertRollsBackAndRetryDoesNotLoseToken() = fixture { store, _ ->
        val offered = candidate(store)
        store.writableDatabase.execSQL("CREATE TRIGGER fail_confirm BEFORE INSERT ON intake WHEN NEW.candidate_token IS NOT NULL BEGIN SELECT RAISE(ABORT, 'test failure'); END")
        assertTrue(runCatching { save(store, offered) }.isFailure)
        assertEquals(0L, store.readToday(day).totalMl)
        assertFalse(store.readToday(day).hasRecords)
        store.writableDatabase.execSQL("DROP TRIGGER fail_confirm")
        assertTrue(save(store, offered))
        assertEquals(200L, store.readToday(day).totalMl)
    }
    @Test fun versionFourUpgradeKeepsRecordsPlansAndSleepHours() = fixture { store, name ->
        store.recordAttempt(day, now, zone = zone)
        // Reconstruct the previous intake shape while retaining all other schema-4 tables.
        store.writableDatabase.execSQL("DROP INDEX intake_candidate")
        store.writableDatabase.execSQL("ALTER TABLE intake RENAME TO intake_latest")
        store.writableDatabase.execSQL("CREATE TABLE intake (id INTEGER PRIMARY KEY, recorded_at INTEGER NOT NULL, amount_ml INTEGER NOT NULL CHECK(amount_ml > 0), local_date TEXT NOT NULL, is_attempt INTEGER NOT NULL DEFAULT 0)")
        store.writableDatabase.execSQL("INSERT INTO intake SELECT id, recorded_at, amount_ml, local_date, is_attempt FROM intake_latest")
        store.writableDatabase.execSQL("DROP TABLE intake_latest")
        store.writableDatabase.execSQL("CREATE INDEX intake_date ON intake(local_date)")
        store.writableDatabase.version = 4
        store.close()
        HydrationStore(context, name).use { upgraded ->
            assertEquals(5, upgraded.readableDatabase.version)
            assertEquals(200L, upgraded.readToday(day).totalMl)
            assertEquals(hours, upgraded.readSleepHours())
            assertEquals(10, upgraded.readToday(day).plan?.attempts)
            upgraded.addExtra(100, day, now, zone)
            assertEquals(300L, upgraded.readToday(day).totalMl)
        }
    }
}
