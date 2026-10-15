package com.drink.app.camera

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.core.logging.TasksStatsDummyLogger
import com.google.mediapipe.tasks.core.logging.TasksStatsLoggerFactory
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest

@RunWith(AndroidJUnit4::class)
class PoseModelTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @Test fun runtimeHasNoTelemetryOrNetworkPermissionAndModelHashIsPinned() {
        assertTrue(TasksStatsLoggerFactory.create(context, "pose_landmarker", "video") is TasksStatsDummyLogger)
        for (name in listOf("com.google.mediapipe.tasks.core.logging.RemoteLoggingClient",
            "com.google.mediapipe.tasks.core.logging.TasksStatsProtoLogger", "com.google.android.datatransport.runtime.TransportRuntime")) {
            assertTrue("Unexpected telemetry class $name", runCatching { Class.forName(name) }.isFailure)
        }
        @Suppress("DEPRECATION")
        val permissions = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty().toList()
        assertFalse(permissions.contains("android.permission.INTERNET"))
        assertFalse(permissions.contains("android.permission.ACCESS_NETWORK_STATE"))
        for (permission in listOf("android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_EXTERNAL_STORAGE", "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO", "android.permission.RECORD_AUDIO")) {
            assertFalse("Detection must not request media storage or audio access", permissions.contains(permission))
        }
        val hash = context.assets.open("pose_landmarker_lite.task").use { MessageDigest.getInstance("SHA-256").digest(it.readBytes()) }
            .joinToString("") { "%02x".format(it) }
        assertEquals("59929e1d1ee95287735ddd833b19cf4ac46d29bc7afddbbf6753c459690d574a", hash)
    }

    @Test fun bundledCpuModelFindsLandmarksInOfflineGoogleTestImage() {
        val bitmap = instrumentation.context.assets.open("pose.jpg").use { BitmapFactory.decodeStream(it) }
            .copy(Bitmap.Config.ARGB_8888, false)
        val start = SystemClock.elapsedRealtime()
        val detector = PoseLandmarker.createFromOptions(context, PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath("pose_landmarker_lite.task").setDelegate(Delegate.CPU).build())
            .setRunningMode(RunningMode.VIDEO).build())
        val loadedAt = SystemClock.elapsedRealtime()
        try {
            val image = BitmapImageBuilder(bitmap).build()
            val result = try { detector.detectForVideo(image, 1L) } finally { image.close() }
            assertEquals(1, result.landmarks().size)
            assertEquals(33, result.landmarks()[0].size)
            assertTrue(result.landmarks()[0].all { it.x().isFinite() && it.y().isFinite() })
            instrumentation.sendStatus(0, Bundle().apply {
                putLong("pose_model_load_ms", loadedAt - start)
                putLong("pose_image_inference_ms", SystemClock.elapsedRealtime() - loadedAt)
            })
        } finally { detector.close(); bitmap.recycle() }
    }
}
