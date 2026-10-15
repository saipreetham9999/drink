package com.drink.app.ui.camera

import android.Manifest
import android.graphics.Bitmap
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
import com.drink.app.MainActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CameraSetupTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun nodes(root: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> = buildList {
        if (root != null) {
            add(root)
            for (index in 0 until root.childCount) addAll(nodes(root.getChild(index)))
        }
    }
    private fun awaitCondition(message: String, predicate: () -> Boolean) {
        val deadline = System.nanoTime() + 20_000_000_000L
        while (System.nanoTime() < deadline) {
            if (predicate()) return
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
    private fun preview(view: View): PreviewView? {
        if (view is PreviewView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) preview(view.getChildAt(index))?.let { return it }
        return null
    }

    @Test fun denialKeepsManualTrackingAvailableAndDoesNotStartPreview() {
        assertEquals("Run this check with camera permission denied", PackageManager.PERMISSION_DENIED,
            instrumentation.targetContext.checkSelfPermission(Manifest.permission.CAMERA))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            click("Camera setup")
            awaitCondition("Initially stopped") { visible("Camera stopped") }
            click("Start")
            awaitCondition("Permission denial control") {
                nodes(instrumentation.uiAutomation.rootInActiveWindow).any {
                    it.viewIdResourceName == "com.android.permissioncontroller:id/permission_deny_button"
                }
            }
            val deny = nodes(instrumentation.uiAutomation.rootInActiveWindow).first {
                it.viewIdResourceName == "com.android.permissioncontroller:id/permission_deny_button"
            }
            assertTrue(deny.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            awaitCondition("Permission recovery") { visible("Open app settings") }
            assertFalse(visible("Camera active - preview only"))
            scenario.onActivity { assertNull(preview(it.window.decorView)) }
            click("Back")
            awaitCondition("Main app remains accessible") { visible("Camera setup") }
        }
    }

    @Test fun allowingPermissionStartsRequestedPreview() {
        assertEquals(PackageManager.PERMISSION_DENIED,
            instrumentation.targetContext.checkSelfPermission(Manifest.permission.CAMERA))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            click("Camera setup")
            click("Start")
            val allowId = "com.android.permissioncontroller:id/permission_allow_foreground_only_button"
            awaitCondition("Allow camera control") {
                nodes(instrumentation.uiAutomation.rootInActiveWindow).any { it.viewIdResourceName == allowId }
            }
            val allow = nodes(instrumentation.uiAutomation.rootInActiveWindow).first { it.viewIdResourceName == allowId }
            assertTrue(allow.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            awaitCondition("Grant should start the requested preview") {
                var streaming = false
                scenario.onActivity { streaming = preview(it.window.decorView)?.previewStreamState?.value == PreviewView.StreamState.STREAMING }
                streaming
            }
            click("Stop")
            awaitCondition("Stopped after permission grant") { visible("Camera stopped") }
        }
    }

    @Test fun explicitSessionStreamsStopsAndDoesNotResumeInBackground() {
        assertEquals("Grant camera permission for this device integration test", PackageManager.PERMISSION_GRANTED,
            instrumentation.targetContext.checkSelfPermission(Manifest.permission.CAMERA))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var backed = false
            scenario.onActivity { activity -> activity.setContent { CameraSetup { backed = true } } }
            awaitCondition("Camera should start stopped") { visible("Camera stopped") }
            repeat(2) {
                click("Start")
                var cameraView: PreviewView? = null
                awaitCondition("Preview should actually stream frames") {
                    var streaming = false
                    scenario.onActivity { activity ->
                        cameraView = preview(activity.window.decorView)
                        streaming = cameraView?.previewStreamState?.value == PreviewView.StreamState.STREAMING
                    }
                    streaming
                }
                if (it == 0) {
                    val image = instrumentation.uiAutomation.takeScreenshot()
                    assertNotNull(image)
                    val file = File(instrumentation.targetContext.getExternalFilesDir(null), "camera-preview-check.png")
                    file.outputStream().use { output -> image!!.compress(Bitmap.CompressFormat.PNG, 100, output) }
                    image!!.recycle()
                }
                click("Stop")
                awaitCondition("Stop status") { visible("Camera stopped") }
                awaitCondition("Preview should release its stream") {
                    var idle = false
                    instrumentation.runOnMainSync { idle = cameraView?.previewStreamState?.value == PreviewView.StreamState.IDLE }
                    idle
                }
            }
            click("Start")
            awaitCondition("Active status") { visible("Camera active - preview only") }
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            awaitCondition("Returning must require Start") { visible("Camera stopped") }
            click("Back")
            instrumentation.runOnMainSync { assertTrue(backed) }
            scenario.recreate()
            awaitCondition("Recreation returns to normal app") { visible("Camera setup") }
            assertFalse(visible("Camera active - preview only"))
        }
    }
}
