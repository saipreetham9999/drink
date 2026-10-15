package com.drink.app.camera

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.drink.app.MainActivity
import com.drink.app.hydration.data.HydrationStore
import com.drink.app.hydration.model.PlanRequest
import com.drink.app.reminders.model.SleepHours
import com.drink.app.ui.dev.ConfirmationTestScreen
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class ConfirmationUiTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun nodes(root: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> = buildList {
        if (root != null) { add(root); for (i in 0 until root.childCount) addAll(nodes(root.getChild(i))) }
    }
    private fun awaitCondition(message: String, predicate: () -> Boolean) {
        val end = System.nanoTime() + 20_000_000_000
        while (System.nanoTime() < end) { if (predicate()) return; Thread.sleep(100) }
        fail(message)
    }
    private fun visible(text: String) = nodes(instrumentation.uiAutomation.rootInActiveWindow).any { it.text?.toString() == text }
    private fun click(text: String) {
        var forward = true
        awaitCondition("Click $text") {
            val current = nodes(instrumentation.uiAutomation.rootInActiveWindow)
            var node = current.firstOrNull { it.text?.toString() == text }
            while (node != null && !node.isClickable) node = node.parent
            if (node?.isEnabled == true && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) true
            else {
                if (current.firstOrNull { it.isScrollable }?.performAction(if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) != true) forward = !forward
                false
            }
        }
    }
    @Test fun rejectingConfirmingAndCorrectingOnlyAffectTestRecords() {
        val context = instrumentation.targetContext
        val name = "confirmation-ui-test.db"
        context.deleteDatabase(name); context.deleteSharedPreferences(name)
        val day = LocalDate.now()
        HydrationStore(context, name).use { it.savePlan(PlanRequest(2000, 10, 1), SleepHours(LocalTime.of(22, 0), LocalTime.of(8, 0)), day) }
        val before = HydrationStore(context).use { it.readToday().totalMl }
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.setContent { ConfirmationTestScreen(name, onBack = {}) } }
                click("Simulate possible drink")
                awaitCondition("Suggestion") { visible("Possible drink detected") }
                assertEquals(0L, HydrationStore(context, name).use { it.readToday().totalMl })
                click("Reject")
                awaitCondition("Rejected") { visible("Suggestion rejected. No water recorded.") }
                click("Simulate possible drink")
                click("Confirm 200 mL")
                awaitCondition("Saved") { visible("Recorded 200 mL.") }
                click("Simulate possible drink")
                awaitCondition("Amount field") { nodes(instrumentation.uiAutomation.rootInActiveWindow).any { it.isEditable } }
                val field = nodes(instrumentation.uiAutomation.rootInActiveWindow).first { it.isEditable }
                assertTrue(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "150")
                }))
                click("Confirm extra 150 mL")
                awaitCondition("Correction saved") { visible("Recorded 150 mL.") }
            }
            HydrationStore(context, name).use { assertEquals(350L, it.readToday().totalMl); assertEquals(1, it.readToday().recordedAttempts) }
            assertEquals(before, HydrationStore(context).use { it.readToday().totalMl })
        } finally { context.deleteDatabase(name); context.deleteSharedPreferences(name) }
    }
}
