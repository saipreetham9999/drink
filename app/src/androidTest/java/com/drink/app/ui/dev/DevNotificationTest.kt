package com.drink.app.ui.dev

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.drink.app.MainActivity
import com.drink.app.dev.notification.DevNotification
import com.drink.app.hydration.data.HydrationStore
import com.drink.app.reminders.notification.ReminderNotifier
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DevNotificationTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val manager get() = context.getSystemService(NotificationManager::class.java)
    private fun nodes(root: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> = buildList {
        if (root != null) {
            add(root)
            for (index in 0 until root.childCount) addAll(nodes(root.getChild(index)))
        }
    }
    private fun awaitCondition(message: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + 20_000_000_000L
        while (System.nanoTime() < deadline) {
            if (condition()) return
            Thread.sleep(100)
        }
        fail(message)
    }
    private fun visible(text: String) = nodes(instrumentation.uiAutomation.rootInActiveWindow).any { it.text?.toString() == text }
    private fun click(text: String) {
        awaitCondition("Missing $text") { visible(text) }
        var node = nodes(instrumentation.uiAutomation.rootInActiveWindow).first { it.text?.toString() == text }
        while (!node.isClickable) node = node.parent ?: error("No clickable parent")
        assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }
    private fun permissionButton(id: String) {
        val resource = "com.android.permissioncontroller:id/$id"
        awaitCondition("Missing permission button") {
            nodes(instrumentation.uiAutomation.rootInActiveWindow).any { it.viewIdResourceName == resource }
        }
        assertTrue(nodes(instrumentation.uiAutomation.rootInActiveWindow).first {
            it.viewIdResourceName == resource
        }.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }
    private fun samples() = manager.activeNotifications.filter { it.tag == DevNotification.TAG && it.id == DevNotification.ID }

    @Test fun allowPostsSampleAndRepeatedTapReusesItWithoutRecordingWater() {
        assertEquals(PackageManager.PERMISSION_DENIED, context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS))
        val before = HydrationStore(context).use { it.readToday().totalMl }
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                click("Dev")
                click("Test notification")
                permissionButton("permission_allow_button")
                awaitCondition("Sample was not posted") { samples().size == 1 }
                val notification = samples().single().notification
                assertEquals(ReminderNotifier.CHANNEL, notification.channelId)
                assertEquals("Drink test notification", notification.extras.getString(Notification.EXTRA_TITLE))
                assertTrue(notification.extras.getString(Notification.EXTRA_TEXT)!!.contains("sample"))
                click("Test notification")
                awaitCondition("Success message") { visible("Sample sent. Check your notification panel.") }
                assertEquals(1, samples().size)
                notification.contentIntent.send()
                instrumentation.waitForIdleSync()
            }
            assertEquals(before, HydrationStore(context).use { it.readToday().totalMl })
        } finally { manager.cancel(DevNotification.TAG, DevNotification.ID) }
    }

    @Test fun deniedPermissionShowsSettingsWithoutPosting() {
        assertEquals(PackageManager.PERMISSION_DENIED, context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS))
        manager.cancel(DevNotification.TAG, DevNotification.ID)
        ActivityScenario.launch(MainActivity::class.java).use {
            click("Dev")
            click("Test notification")
            permissionButton("permission_deny_button")
            awaitCondition("Recovery link") { visible("Notification settings") }
            assertTrue(samples().isEmpty())
        }
    }

    @Test fun blockedChannelDoesNotClaimDelivery() {
        val channel = "dev-notification-blocked-test"
        try {
            manager.createNotificationChannel(NotificationChannel(channel, "Test blocked", NotificationManager.IMPORTANCE_NONE))
            assertFalse(DevNotification.send(context, channel))
            assertTrue(samples().isEmpty())
        } finally { manager.deleteNotificationChannel(channel) }
    }
}
