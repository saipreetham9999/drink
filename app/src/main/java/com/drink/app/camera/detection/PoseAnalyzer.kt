package com.drink.app.camera.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.drink.app.camera.domain.PoseFrame
import com.drink.app.camera.domain.PosePoint
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/** Single worker, sampled at most 5 FPS. No images or model outputs are persisted. */
class PoseAnalyzer(private val context: Context, private val mirrored: Boolean,
    private val onFrame: (PoseFrame) -> Unit, private val onError: () -> Unit) : ImageAnalysis.Analyzer {
    val stopped = AtomicBoolean(false)
    private var detector: PoseLandmarker? = null
    private var raw: Bitmap? = null
    private var oriented: Bitmap? = null
    private var modelImage: MPImage? = null
    private var packed: ByteBuffer? = null
    private var last = -1L

    override fun analyze(image: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        if (stopped.get() || (last >= 0 && now - last < 200)) { image.close(); return }
        last = now
        try {
            if (detector == null) detector = PoseLandmarker.createFromOptions(context,
                PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath("pose_landmarker_lite.task").setDelegate(Delegate.CPU).build())
                    .setRunningMode(RunningMode.VIDEO).setNumPoses(1)
                    .setMinPoseDetectionConfidence(.6f).setMinPosePresenceConfidence(.6f)
                    .setMinTrackingConfidence(.6f).build())
            val width = image.width; val height = image.height
            if (raw?.width != width || raw?.height != height) {
                raw?.recycle(); raw = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                packed = ByteBuffer.allocateDirect(width * height * 4)
            }
            val plane = image.planes[0]
            val input = plane.buffer.duplicate()
            val output = packed!!; output.clear()
            for (row in 0 until height) {
                input.limit(input.capacity()); input.position(row * plane.rowStride)
                input.limit(row * plane.rowStride + width * 4); output.put(input)
            }
            output.rewind(); raw!!.copyPixelsFromBuffer(output)
            val rotation = image.imageInfo.rotationDegrees
            val matrix = Matrix().apply { setRotate(rotation.toFloat()) }
            val bounds = RectF(0f, 0f, width.toFloat(), height.toFloat())
            matrix.mapRect(bounds); matrix.postTranslate(-bounds.left, -bounds.top)
            val ow = bounds.width().toInt(); val oh = bounds.height().toInt()
            if (oriented?.width != ow || oriented?.height != oh) {
                modelImage?.close() // Owns and recycles the previous oriented bitmap.
                oriented = Bitmap.createBitmap(ow, oh, Bitmap.Config.ARGB_8888)
                modelImage = BitmapImageBuilder(oriented!!).build()
            }
            Canvas(oriented!!).drawBitmap(raw!!, matrix, null)
            val start = SystemClock.elapsedRealtime()
            val result = detector!!.detectForVideo(modelImage!!, now)
            if (!stopped.get()) onFrame(PoseFrame(now, result.landmarks().firstOrNull()?.map {
                PosePoint(it.x(), it.y(), minOf(it.visibility().orElse(0f), it.presence().orElse(0f)))
            } ?: emptyList(), ow, oh, mirrored, SystemClock.elapsedRealtime() - start))
        } catch (_: Exception) {
            if (stopped.compareAndSet(false, true)) onError()
        } catch (_: LinkageError) {
            if (stopped.compareAndSet(false, true)) onError()
        } finally { image.close() }
    }

    /** Run on the analysis executor after outstanding inference has finished. */
    fun release() {
        stopped.set(true)
        runCatching { detector?.close() }; detector = null
        raw?.recycle(); raw = null; modelImage?.close(); modelImage = null; oriented = null; packed = null
    }
}
