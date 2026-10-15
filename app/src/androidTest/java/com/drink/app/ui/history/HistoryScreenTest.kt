package com.drink.app.ui.history

import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.drink.app.MainActivity
import com.drink.app.R
import com.drink.app.hydration.data.HistoryFixture
import com.drink.app.hydration.data.HydrationStore
import com.drink.app.hydration.presentation.HydrationController
import com.drink.app.reminders.presentation.ReminderController
import com.drink.app.ui.DrinkApp
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@RunWith(AndroidJUnit4::class)
class HistoryScreenTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun nodes(root: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> = buildList {
        if (root != null) {
            add(root)
            for (index in 0 until root.childCount) addAll(nodes(root.getChild(index)))
        }
    }
    private fun visible() = nodes(instrumentation.uiAutomation.rootInActiveWindow)
    private fun awaitNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        val deadline = System.nanoTime() + 15_000_000_000L
        while (System.nanoTime() < deadline) {
            visible().firstOrNull(predicate)?.let { return it }
            Thread.sleep(50)
        }
        throw AssertionError("Expected history UI node not found")
    }
    private fun click(text: String) {
        var node: AccessibilityNodeInfo? = awaitNode { it.text?.toString()?.contains(text) == true }
        while (node != null && !node.isClickable) node = node.parent
        assertNotNull("No clickable parent for $text", node)
        assertTrue(node!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }
    private fun screenshot(name: String) {
        val context = instrumentation.targetContext
        val directory = File(context.getExternalFilesDir(null), "history-ui-check")
        directory.mkdirs()
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: throw AssertionError("Screenshot unavailable")
        File(directory, name).outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
    }

    @Test fun populatedHistoryIsReadOnlyAndNavigatesBackToSetup() {
        val context = instrumentation.targetContext
        val name = "history-ui-test.db"
        context.deleteDatabase(name)
        var controller: HydrationController? = null
        var reminders: ReminderController? = null
        try {
            val store = HydrationStore(context, name)
            HistoryFixture.seed(store)
            instrumentation.runOnMainSync {
                controller = HydrationController(store, historyInitiallyOpen = true,
                    currentTime = { HistoryFixture.today.atTime(12, 0).atZone(ZoneId.systemDefault()) })
                reminders = ReminderController(context)
                controller!!.retry()
                reminders!!.refresh()
            }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    activity.setContent { DrinkApp(controller!!, reminders!!, onSaveReminders = {},
                        onConfigurePlan = { _, _ -> }, onNotificationSettings = {}) }
                }
                val date = HistoryFixture.recent.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                awaitNode { it.text?.toString()?.contains(date) == true }
                assertFalse(visible().any { it.className?.toString() == "android.widget.EditText" })
                screenshot("history-dates.png")
                click(date)
                awaitNode { it.text?.toString()?.contains(context.getString(R.string.recorded_intake)) == true }
                awaitNode { it.text?.toString()?.contains(context.getString(R.string.completion, 109L)) == true }
                awaitNode { it.text?.toString()?.contains("1101 mL") == true }
                awaitNode { it.text?.toString()?.contains("1001") == true }
                assertFalse(visible().any { it.className?.toString() == "android.widget.EditText" })
                assertFalse(visible().any { it.text?.toString() in listOf("Edit", "Delete", "Save", "Record extra") })
                awaitNode { it.text?.toString()?.let { text -> text.contains("AM") || text.contains("PM") } == true }
                screenshot("history-detail.png")
                click(context.getString(R.string.back))
                awaitNode { it.text?.toString()?.contains(date) == true }
                click(context.getString(R.string.back))
                awaitNode { it.text?.toString() in setOf("Sleeping hours", "Your water plan", "Drink") }
                // History remains reachable even though sleeping-hours setup is still needed.
                click("History")
                awaitNode { it.text?.toString()?.contains(date) == true }
                scenario.onActivity { activity ->
                    activity.setContent { DrinkHistory(emptyList(), null, null, false, true, {}, {}, {}) }
                }
                awaitNode { it.text?.toString()?.contains(context.getString(R.string.storage_error)) == true }
                assertFalse(visible().any { it.text?.toString() == context.getString(R.string.history_empty) })
            }
            assertEquals(4, store.readDay(HistoryFixture.recent).entries.size)
            assertEquals(1101L, store.readDay(HistoryFixture.recent).summary.totalMl)
        } finally {
            instrumentation.runOnMainSync { controller?.close(); reminders?.close() }
            context.deleteDatabase(name)
        }
    }
}
