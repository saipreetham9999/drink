package com.drink.app.camera.preview

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraState
import androidx.camera.core.Preview
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import android.util.Size
import com.drink.app.camera.detection.PoseAnalyzer
import com.drink.app.camera.domain.PoseFrame
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer

/** Owns preview and optional local analysis. No video recording or hydration access. */
class CameraPreviewSession(private val context: Context) {
    private var generation = 0
    private var provider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var cameraInfo: CameraInfo? = null
    private var errorObserver: Observer<CameraState>? = null
    private var analysis: ImageAnalysis? = null
    private var analyzer: PoseAnalyzer? = null
    private var worker: ExecutorService? = null

    // Called on the main thread, including the asynchronous provider callback.
    fun start(owner: LifecycleOwner, view: PreviewView, onReady: () -> Unit, onError: () -> Unit,
        onFrame: ((PoseFrame) -> Unit)? = null, onDetectionError: () -> Unit = {}) {
        stop()
        val request = generation
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (request != generation) return@addListener
            try {
                val cameraProvider = future.get()
                val selector = when {
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    else -> error("No camera available")
                }
                provider = cameraProvider
                val resolution = ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .setResolutionStrategy(ResolutionStrategy(Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)).build()
                val useCase = Preview.Builder().setResolutionSelector(resolution).build()
                preview = useCase
                useCase.setSurfaceProvider(view.surfaceProvider)
                if (onFrame != null) {
                    val main = ContextCompat.getMainExecutor(context)
                    val processor = PoseAnalyzer(context.applicationContext, selector == CameraSelector.DEFAULT_FRONT_CAMERA,
                        onFrame = { frame -> main.execute { if (request == generation) onFrame(frame) } },
                        onError = { main.execute { if (request == generation) onDetectionError() } })
                    analyzer = processor
                    worker = Executors.newSingleThreadExecutor()
                    analysis = ImageAnalysis.Builder().setResolutionSelector(resolution)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also { it.setAnalyzer(worker!!, processor) }
                }
                val camera = if (analysis == null) cameraProvider.bindToLifecycle(owner, selector, useCase)
                    else cameraProvider.bindToLifecycle(owner, selector, useCase, analysis!!)
                cameraInfo = camera.cameraInfo
                val observer = Observer<CameraState> { state ->
                    if (request == generation && state.error != null) {
                        stop()
                        onError()
                    }
                }
                errorObserver = observer
                camera.cameraInfo.cameraState.observe(owner, observer)
                if (request == generation) onReady()
            } catch (_: Exception) {
                stop()
                onError()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stop() {
        generation++ // Prevent an outstanding initialization callback from opening the camera.
        errorObserver?.let { cameraInfo?.cameraState?.removeObserver(it) }
        errorObserver = null
        cameraInfo = null
        analysis?.clearAnalyzer()
        analyzer?.let { processor ->
            processor.stopped.set(true)
            worker?.execute { processor.release() }
        }
        worker?.shutdown(); worker = null; analyzer = null
        analysis?.let { provider?.unbind(it) }; analysis = null
        preview?.let { provider?.unbind(it) }
        preview = null
        provider = null
    }
}
