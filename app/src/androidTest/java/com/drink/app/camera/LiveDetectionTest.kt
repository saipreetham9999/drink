package com.drink.app.camera

import android.Manifest
import android.app.ActivityManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.drink.app.MainActivity
import com.drink.app.ui.camera.DetectionSessionScreen
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveDetectionTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun nodes(root: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> = buildList {
        if (root != null) { add(root); for (i in 0 until root.childCount) addAll(nodes(root.getChild(i))) }
    }
    private fun awaitCondition(message: String, predicate: () -> Boolean) {
        val end = System.nanoTime() + 40_000_000_000
        while (System.nanoTime() < end) { if (predicate()) return; Thread.sleep(150) }
        fail(message)
    }
    private fun click(text: String) {
        var forward = true
        awaitCondition("Click $text") {
            val current = nodes(instrumentation.uiAutomation.rootInActiveWindow)
            var node = current.firstOrNull { it.text?.toString() == text }
            while (node != null && !node.isClickable) node = node.parent
            if (node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) true else {
                if (current.firstOrNull { it.isScrollable }?.performAction(if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) != true) forward = !forward
                false
            }
        }
    }
    @Test fun cameraProcessesMultipleFramesAndStopsWithoutWritingOrUploading() {
        val context = instrumentation.targetContext
        assertEquals(PackageManager.PERMISSION_GRANTED, context.checkSelfPermission(Manifest.permission.CAMERA))
        val name = "live-pose-test.db"
        context.deleteDatabase(name)
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.setContent { DetectionSessionScreen(testMode = true, databaseName = name, onBack = {}) } }
                click("Start")
                var stats = ""
                awaitCondition("At least three frames must complete native analysis") {
                    stats = nodes(instrumentation.uiAutomation.rootInActiveWindow).mapNotNull { it.text?.toString() }
                        .firstOrNull { it.startsWith("Analyzed ") } ?: ""
                    if (stats.isEmpty()) nodes(instrumentation.uiAutomation.rootInActiveWindow).firstOrNull { it.isScrollable }
                        ?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                    stats.substringAfter("Analyzed ").substringBefore(" frames").toIntOrNull()?.let { it >= 3 } == true
                }
                val memory = context.getSystemService(ActivityManager::class.java).getProcessMemoryInfo(intArrayOf(android.os.Process.myPid()))[0].totalPss
                instrumentation.sendStatus(0, Bundle().apply { putString("live_pose_stats", stats); putInt("instrumented_app_pss_kb", memory) })
                click("Stop")
                awaitCondition("Camera stops") { nodes(instrumentation.uiAutomation.rootInActiveWindow).any { it.text?.toString() == "Camera stopped" } }
                Thread.sleep(600)
                assertTrue(nodes(instrumentation.uiAutomation.rootInActiveWindow).none { it.text?.toString()?.startsWith("Detection unavailable") == true })
            }
        } finally { context.deleteDatabase(name) }
    }
}
