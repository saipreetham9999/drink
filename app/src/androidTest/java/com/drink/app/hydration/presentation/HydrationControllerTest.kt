package com.drink.app.hydration.presentation

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.drink.app.hydration.data.HydrationStore
import com.drink.app.hydration.model.PlanRequest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class HydrationControllerTest {
    @Test fun repeatedTapWhileSavingIsIgnoredAndConfirmedStateUpdates() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "controller-recording-test.db"
        context.deleteDatabase(name)
        val loaded = CountDownLatch(1)
        val recorded = CountDownLatch(1)
        var controller: HydrationController? = null
        try {
            val store = HydrationStore(context, name)
            store.configurePlan(PlanRequest(2000, 10, 1))
            instrumentation.runOnMainSync {
                controller = HydrationController(store, onDataChanged = {
                    if (controller?.home?.hasRecords == true) recorded.countDown() else loaded.countDown()
                })
                controller!!.retry()
            }
            assertTrue("Initial state was not loaded", loaded.await(15, TimeUnit.SECONDS))
            instrumentation.runOnMainSync {
                controller!!.recordAttempt()
                assertTrue(controller!!.busy)
                controller!!.recordAttempt()
            }
            assertTrue("Confirmed intake was not loaded", recorded.await(15, TimeUnit.SECONDS))
            instrumentation.runOnMainSync {
                val home = controller!!.home!!
                assertFalse(controller!!.busy)
                assertFalse(controller!!.error)
                assertEquals(1, home.recordedAttempts)
                assertEquals(200L, home.totalMl)
                assertEquals(1800L, home.remainingMl)
                assertEquals(9, home.remainingAttempts)
            }
            HydrationStore(context, name).use { assertEquals(1, it.readDay(java.time.LocalDate.now()).entries.size) }
        } finally {
            instrumentation.runOnMainSync { controller?.close() }
            context.deleteDatabase(name)
        }
    }
}
