package com.drink.app.hydration.presentation

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.drink.app.hydration.data.HistoryFixture
import com.drink.app.hydration.data.HydrationStore
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class HistoryNavigationTest {
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
        fail("History did not load")
    }

    @Test fun datesDetailsBackAndRestoreUseOnlyEligibleRecordedDays() {
        val context = instrumentation.targetContext
        val name = "history-navigation-test.db"
        context.deleteDatabase(name)
        var controller: HydrationController? = null
        val time = AtomicReference(HistoryFixture.today.atTime(12, 0).atZone(ZoneId.systemDefault()))
        try {
            val store = HydrationStore(context, name)
            HistoryFixture.seed(store)
            instrumentation.runOnMainSync { controller = HydrationController(store, currentTime = { time.get() }) }
            val active = controller!!
            settled(active) { active.retry() }
            settled(active) { active.openHistory() }
            assertEquals(3, active.history.size)
            settled(active) { active.selectDate(HistoryFixture.today) }
            settled(active) { active.selectDate(HistoryFixture.today.minusDays(3)) }
            assertNull(active.selectedDate)
            settled(active) { active.selectDate(HistoryFixture.recent) }
            assertEquals(1101L, active.detail?.summary?.totalMl)
            assertEquals(1001, active.detail?.summary?.goalMl)
            instrumentation.runOnMainSync { active.goBack() }
            assertTrue(active.historyOpen); assertNull(active.detail); assertNull(active.selectedDate)
            instrumentation.runOnMainSync { active.goBack() }
            assertFalse(active.historyOpen)
            settled(active) { active.selectDate(HistoryFixture.recent) }
            assertNull(active.selectedDate)

            HydrationStore(context, name).use { restoredStore ->
                var restored: HydrationController? = null
                try {
                    instrumentation.runOnMainSync {
                        restored = HydrationController(restoredStore, historyInitiallyOpen = true,
                            initiallySelectedDate = HistoryFixture.recent, currentTime = { time.get() })
                    }
                    settled(restored!!) { restored!!.retry() }
                    assertEquals(HistoryFixture.recent, restored!!.selectedDate)
                    assertEquals(4, restored!!.detail?.entries?.size)
                    // Travel/clock change makes the selected date Today: return to the dates list.
                    time.set(time.get().minusDays(1))
                    settled(restored!!) { restored!!.retry() }
                    assertNull(restored!!.selectedDate)
                    assertNull(restored!!.detail)
                    assertEquals(listOf(HistoryFixture.older, HistoryFixture.legacy), restored!!.history.map { it.date })
                } finally { instrumentation.runOnMainSync { restored?.close() } }
            }
        } finally {
            instrumentation.runOnMainSync { controller?.close() }
            context.deleteDatabase(name)
        }
    }

    @Test fun invalidRestoredSelectionReturnsToListWithoutAStorageError() {
        val context = instrumentation.targetContext
        val name = "history-invalid-selection-test.db"
        context.deleteDatabase(name)
        var controller: HydrationController? = null
        try {
            val store = HydrationStore(context, name)
            HistoryFixture.seed(store)
            instrumentation.runOnMainSync {
                controller = HydrationController(store, historyInitiallyOpen = true,
                    initiallySelectedDate = HistoryFixture.today.plusDays(1),
                    currentTime = { HistoryFixture.today.atTime(12, 0).atZone(ZoneId.systemDefault()) })
            }
            settled(controller!!) { controller!!.retry() }
            assertTrue(controller!!.historyOpen)
            assertFalse(controller!!.error)
            assertNull(controller!!.selectedDate)
            assertNull(controller!!.detail)
            assertEquals(3, controller!!.history.size)
        } finally {
            instrumentation.runOnMainSync { controller?.close() }
            context.deleteDatabase(name)
        }
    }
}
