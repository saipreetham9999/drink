package com.drink.app.camera

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.camera.view.PreviewView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.drink.app.MainActivity
import com.drink.app.camera.data.DetectionPreferences
import com.drink.app.camera.domain.CameraSessionLimit
import com.drink.app.hydration.data.HydrationStore
import com.drink.app.reminders.notification.ReminderNotifier
import com.drink.app.ui.camera.CameraSetup
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

@RunWith(AndroidJUnit4::class)
class CameraReminderSessionTest {
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
    private fun preview(view: View): PreviewView? {
        if (view is PreviewView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) preview(view.getChildAt(index))?.let { return it }
        return null
    }
    private fun currentPreview(): PreviewView? {
        var found: PreviewView? = null
        instrumentation.runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).forEach {
                preview(it.window.decorView)?.let { value -> found = value }
            }
        }
        return found
    }
    private fun streaming(view: PreviewView?): Boolean {
        var result = false
        instrumentation.runOnMainSync { result = view?.previewStreamState?.value == PreviewView.StreamState.STREAMING }
        return result
    }
    private fun closeCameraActivities() = instrumentation.runOnMainSync {
        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
            .filterIsInstance<CameraSessionActivity>().forEach { it.finish() }
    }

    @Test fun optInDefaultsOffAndPersistsSeparately() {
        val name = "detection-preference-test"
        context.deleteSharedPreferences(name)
        try {
            assertFalse(DetectionPreferences(context, name).enabled)
            DetectionPreferences(context, name).enabled = true
            assertTrue(DetectionPreferences(context, name).enabled)
            DetectionPreferences(context, name).enabled = false
            assertFalse(DetectionPreferences(context, name).enabled)
        } finally { context.deleteSharedPreferences(name) }
    }

    @Test fun actualReminderPendingIntentRechecksCurrentSwitchAndStartsVisiblePreview() {
        assertEquals(PackageManager.PERMISSION_GRANTED, context.checkSelfPermission(Manifest.permission.CAMERA))
        assertEquals(PackageManager.PERMISSION_GRANTED, context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS))
        val preferences = DetectionPreferences(context)
        val original = preferences.enabled
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = "camera-route-test"
        val id = 9101
        val before = HydrationStore(context).use { it.readToday().totalMl }
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                preferences.enabled = false
                assertTrue(ReminderNotifier.show(context, 200, LocalDate.now(), 0, channel, id))
                val tap = manager.activeNotifications.first { it.id == id }.notification.contentIntent
                // Turn on after posting; the same actual PendingIntent must honor the new value.
                preferences.enabled = true
                tap.send()
                awaitCondition("Reminder camera should stream") { streaming(currentPreview()) }
                assertTrue(visible("Water reminder camera"))
                val view = currentPreview()
                closeCameraActivities()
                awaitCondition("Closing reminder camera releases stream") { !streaming(view) }
                // Turn off after posting; the same PendingIntent must now route to ordinary Home.
                preferences.enabled = false
                tap.send()
                awaitCondition("Off should open normal Home") { visible("Drink") && visible("Dev") }
                assertNull(currentPreview())
            }
            assertEquals(before, HydrationStore(context).use { it.readToday().totalMl })
        } finally {
            closeCameraActivities()
            preferences.enabled = original
            manager.cancel(id)
            manager.deleteNotificationChannel(channel)
        }
    }

    @Test fun backgroundAndRecreationDoNotRestartNotificationSession() {
        assertEquals(PackageManager.PERMISSION_GRANTED, context.checkSelfPermission(Manifest.permission.CAMERA))
        val preferences = DetectionPreferences(context)
        val original = preferences.enabled
        preferences.enabled = true
        try {
            ActivityScenario.launch<CameraSessionActivity>(Intent(context, CameraSessionActivity::class.java)
                .setAction(CameraSessionActivity.REMINDER_TAP)).use { scenario ->
                awaitCondition("Auto-start from reminder") { streaming(currentPreview()) }
                val view = currentPreview()
                scenario.moveToState(Lifecycle.State.CREATED)
                awaitCondition("Background releases camera") { !streaming(view) }
                scenario.moveToState(Lifecycle.State.RESUMED)
                awaitCondition("Return remains stopped") { visible("Camera stopped") }
                assertNull(currentPreview())
                scenario.recreate()
                awaitCondition("Recreated session remains stopped") { visible("Camera stopped") }
                assertNull(currentPreview())
            }
        } finally { preferences.enabled = original }
    }

    @Test fun elapsedDeadlineStopsActualStreamWithoutWaitingFourRealMinutes() {
        assertEquals(PackageManager.PERMISSION_GRANTED, context.checkSelfPermission(Manifest.permission.CAMERA))
        val clock = AtomicLong(0)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent { CameraSetup(autoStart = true, elapsedRealtime = clock::get, onBack = {}) }
            }
            awaitCondition("Preview streams before expiry") { streaming(currentPreview()) }
            val view = currentPreview()
            clock.set(CameraSessionLimit.MAX_MILLIS)
            awaitCondition("Expiry status") { visible("4-minute limit reached. Camera stopped.") }
            awaitCondition("Expiry releases camera") { !streaming(view) }
            assertNull(currentPreview())
        }
    }

    @Test fun reminderPermissionDenialNeverStartsCameraOrRetriesOnRecreation() {
        assertEquals(PackageManager.PERMISSION_DENIED, context.checkSelfPermission(Manifest.permission.CAMERA))
        val preferences = DetectionPreferences(context)
        val original = preferences.enabled
        preferences.enabled = true
        try {
            ActivityScenario.launch<CameraSessionActivity>(Intent(context, CameraSessionActivity::class.java)
                .setAction(CameraSessionActivity.REMINDER_TAP)).use { scenario ->
                val denyId = "com.android.permissioncontroller:id/permission_deny_button"
                awaitCondition("Notification tap requests camera permission") {
                    nodes(instrumentation.uiAutomation.rootInActiveWindow).any { it.viewIdResourceName == denyId }
                }
                assertTrue(nodes(instrumentation.uiAutomation.rootInActiveWindow).first {
                    it.viewIdResourceName == denyId
                }.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                awaitCondition("Denied camera offers recovery") { visible("Open app settings") }
                assertNull(currentPreview())
                scenario.recreate()
                awaitCondition("Recreation cannot replay the tap") { visible("Camera stopped") }
                assertNull(currentPreview())
            }
        } finally { preferences.enabled = original }
    }
}
