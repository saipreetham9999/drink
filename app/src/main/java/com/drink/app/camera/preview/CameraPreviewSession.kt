package com.drink.app.camera.preview

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraState
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer

/** Owns only this preview use case. No capture, analysis, recording or hydration access. */
class CameraPreviewSession(private val context: Context) {
    private var generation = 0
    private var provider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var cameraInfo: CameraInfo? = null
    private var errorObserver: Observer<CameraState>? = null

    // Called on the main thread, including the asynchronous provider callback.
    fun start(owner: LifecycleOwner, view: PreviewView, onReady: () -> Unit, onError: () -> Unit) {
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
                val useCase = Preview.Builder().build()
                preview = useCase
                useCase.setSurfaceProvider(view.surfaceProvider)
                val camera = cameraProvider.bindToLifecycle(owner, selector, useCase)
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
        preview?.let { provider?.unbind(it) }
        preview = null
        provider = null
    }
}
