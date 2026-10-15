package com.drink.app.hydration.presentation

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.drink.app.hydration.data.HydrationStore
import com.drink.app.hydration.model.PlanRequest
import com.drink.app.reminders.model.ReminderConfig
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class ControllerRolloverTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun settled(controller: HydrationController, action: () -> Unit) {
        instrumentation.runOnMainSync(action)
        val deadline = System.nanoTime() + 15_000_000_000L
        while (System.nanoTime() < deadline) {
            var busy = true
            instrumentation.runOnMainSync { busy = controller.busy }
            if (!busy) return
            Thread.sleep(10)
        }
        fail("Controller did not finish loading")
    }

    @Test fun resumeRefreshResetsDayAndExpiresWeekWithoutDeviceClockChange() {
        val context = instrumentation.targetContext
        val name = "controller-rollover-test.db"
        context.deleteDatabase(name)
        val start = ZonedDateTime.of(2026, 10, 14, 23, 59, 0, 0, ZoneId.systemDefault())
        val time = AtomicReference(start)
        val changes = AtomicInteger()
        var controller: HydrationController? = null
        try {
            val store = HydrationStore(context, name)
            store.configurePlan(PlanRequest(2000, 10, 7), start.toLocalDate())
            store.recordAttempt(start.toLocalDate(), start.toInstant().toEpochMilli())
            instrumentation.runOnMainSync {
                controller = HydrationController(store, onDataChanged = { changes.incrementAndGet() }, currentTime = { time.get() })
            }
            val active = controller!!
            settled(active) { active.retry() }
            assertEquals(200L, active.home?.totalMl)
            time.set(start.plusDays(1))
            settled(active) { active.startRefreshing() }
            instrumentation.runOnMainSync { active.stopRefreshing() }
            // startRefreshing posts a refresh; runOnMainSync(stop) executes after that posted refresh.
            settled(active) { active.retry() }
            assertEquals(start.toLocalDate().plusDays(1), active.home?.date)
            assertEquals(0L, active.home?.totalMl)
            assertEquals(0, active.home?.recordedAttempts)
            assertFalse(active.home!!.hasRecords)
            time.set(start.plusDays(6))
            settled(active) { active.retry() }
            assertNotNull(active.home?.plan)
            time.set(start.plusDays(7))
            settled(active) { active.retry() }
            assertNull(active.home?.plan)
            assertNull(active.home?.nextAmountMl)
            assertTrue(changes.get() >= 4)
            HydrationStore(context, name).use { assertEquals(200L, it.readDay(start.toLocalDate()).summary.totalMl) }
        } finally {
            instrumentation.runOnMainSync { controller?.close() }
            context.deleteDatabase(name)
        }
    }

    @Test fun staleActionsRecoverDateAndNotifySchedulerWithoutWriting() {
        val context = instrumentation.targetContext
        val name = "controller-stale-day-test.db"
        context.deleteDatabase(name)
        val start = ZonedDateTime.of(2026, 10, 14, 23, 59, 0, 0, ZoneId.systemDefault())
        val time = AtomicReference(start)
        val changes = AtomicInteger()
        var controller: HydrationController? = null
        try {
            val store = HydrationStore(context, name)
            store.configurePlan(PlanRequest(2000, 10, 7), start.toLocalDate())
            instrumentation.runOnMainSync {
                controller = HydrationController(store, onDataChanged = { changes.incrementAndGet() }, currentTime = { time.get() })
            }
            val active = controller!!
            settled(active) { active.retry() }
            val before = changes.get()
            time.set(start.plusDays(1))
            settled(active) { active.recordAttempt() }
            assertTrue(active.error)
            assertEquals(start.toLocalDate().plusDays(1), active.home?.date)
            assertEquals(0L, active.home?.totalMl)
            assertEquals(before + 1, changes.get())
            time.set(start.plusDays(2))
            settled(active) { active.addExtra(100) }
            assertTrue(active.error)
            assertEquals(before + 2, changes.get())
            time.set(start.plusDays(7))
            var saved = false
            settled(active) { active.configurePlan(PlanRequest(1500, 5, 1), ReminderConfig(sleepConfigured = true)) { saved = true } }
            assertFalse(saved)
            assertTrue(active.error)
            assertNull(active.home?.plan)
            assertEquals(before + 3, changes.get())
            HydrationStore(context, name).use {
                assertTrue(it.readHistory(start.toLocalDate().plusDays(8)).isEmpty())
                assertEquals(2000, it.readPlan(start.toLocalDate())?.goalMl)
            }
        } finally {
            instrumentation.runOnMainSync { controller?.close() }
            context.deleteDatabase(name)
        }
    }
}
