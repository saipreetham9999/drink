package com.drink.app.ui.dev

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.drink.app.MainActivity
import com.drink.app.camera.CameraSessionActivity
import com.drink.app.camera.data.DetectionPreferences
import com.drink.app.dev.notification.DevNotification
import com.drink.app.hydration.data.HydrationStore
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DevTestingHubTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun nodes(root: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> = buildList {
        if (root != null) {
            add(root)
            for (index in 0 until root.childCount) addAll(nodes(root.getChild(index)))
        }
    }
    private fun visible(text: String) = nodes(instrumentation.uiAutomation.rootInActiveWindow).any { it.text?.toString() == text }
    private fun awaitCondition(message: String, predicate: () -> Boolean) {
        val deadline = System.nanoTime() + 20_000_000_000L
        while (System.nanoTime() < deadline) {
            if (predicate()) return
            Thread.sleep(100)
        }
        fail(message)
    }
    private fun click(text: String) {
        var forward = true
        awaitCondition("Missing/could not click $text") {
            val current = nodes(instrumentation.uiAutomation.rootInActiveWindow)
            var node = current.firstOrNull { it.text?.toString() == text }
            while (node != null && !node.isClickable) node = node.parent
            if (node?.isEnabled == true && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) true
            else {
                val scroll = current.firstOrNull { it.isScrollable }
                val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                if (scroll?.performAction(action) != true) forward = !forward
                false
            }
        }
    }

    @Test fun sandboxRecordsExtrasRollsOverAndPreservesOwnerData() {
        val name = "dev-hub-instrumentation.db"
        context.deleteDatabase(name)
        context.deleteSharedPreferences(name)
        val before = HydrationStore(context).use { it.readToday().totalMl }
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.setContent { HydrationSandbox(databaseName = name, onBack = {}) } }
                awaitCondition("Sandbox plan form") { visible("Your water plan") }
                click("Start plan")
                awaitCondition("Plan saved") { visible("Record 200 mL") }
                click("Record 200 mL")
                awaitCondition("Attempt recorded") { visible("200 / 2000 mL") }
                click("Add extra water")
                click("Record extra")
                awaitCondition("Extra recorded") { visible("300 / 2000 mL") }
                click("Next test day")
                awaitCondition("Today-only plan expired") { visible("Your water plan") }
                click("History")
                awaitCondition("Sandbox history banner") { visible("DEV TEST HISTORY - separate test records") }
            }
            HydrationStore(context, name).use {
                assertEquals(300L, it.readDay(LocalDate.now()).summary.totalMl)
                assertEquals(0L, it.readToday(LocalDate.now().plusDays(1)).totalMl)
            }
            assertEquals(before, HydrationStore(context).use { it.readToday().totalMl })
        } finally {
            context.deleteDatabase(name)
            context.deleteSharedPreferences(name)
        }
    }

    @Test fun devOffersCameraReminderAndSharedOptInWithoutReplacingSample() {
        assertEquals(PackageManager.PERMISSION_GRANTED, context.checkSelfPermission(Manifest.permission.CAMERA))
        assertEquals(PackageManager.PERMISSION_GRANTED, context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS))
        val preferences = DetectionPreferences(context)
        val original = preferences.enabled
        val manager = context.getSystemService(NotificationManager::class.java)
        preferences.enabled = false
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                click("Dev")
                awaitCondition("Dev page loaded") { visible("Test features under development.") }
                awaitCondition("Detection switch") {
                    nodes(instrumentation.uiAutomation.rootInActiveWindow).any { it.contentDescription?.toString() == "Camera detection" }
                }
                var toggle = nodes(instrumentation.uiAutomation.rootInActiveWindow).first { it.contentDescription?.toString() == "Camera detection" }
                while (!toggle.isClickable) toggle = toggle.parent ?: error("No actionable switch parent")
                assertTrue(toggle.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                awaitCondition("Shared opt-in enabled") { preferences.enabled }
                click("Test notification")
                click("Test camera reminder")
                awaitCondition("Both samples posted") {
                    manager.activeNotifications.count { it.tag == DevNotification.TAG && it.id in setOf(DevNotification.ID, DevNotification.CAMERA_ID) } == 2
                }
                val camera = manager.activeNotifications.first { it.tag == DevNotification.TAG && it.id == DevNotification.CAMERA_ID }
                camera.notification.contentIntent.send()
                awaitCondition("Camera reminder opens session") { visible("Camera setup") && visible("Camera active - looking for possible drinks") }
            }
        } finally {
            instrumentation.runOnMainSync {
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .filterIsInstance<CameraSessionActivity>().forEach { it.finish() }
            }
            preferences.enabled = original
            manager.cancel(DevNotification.TAG, DevNotification.ID)
            manager.cancel(DevNotification.TAG, DevNotification.CAMERA_ID)
        }
    }
}
