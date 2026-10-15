package com.drink.app.ui.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.ViewOutlineProvider
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.drink.app.camera.preview.CameraPreviewSession
import com.drink.app.ui.theme.DrinkTheme

@Composable
fun CameraSetup(onBack: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val session = remember(context) { CameraPreviewSession(context) }
    val view = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            outlineProvider = ViewOutlineProvider.BOUNDS
            clipToOutline = true
            contentDescription = "Camera preview"
        }
    }
    // Deliberately not saveable: recreation or backgrounding requires a new explicit Start.
    var requested by remember { mutableStateOf(false) }
    var starting by remember { mutableStateOf(false) }
    var active by remember { mutableStateOf(false) }
    var denied by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    fun stop() {
        requested = false
        starting = false
        active = false
        session.stop()
    }
    fun start() {
        if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        requested = false
        failed = false
        denied = false
        starting = true
        session.start(owner, view, onReady = { starting = false; active = true },
            onError = { stop(); failed = true })
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (requested) {
            if (granted) start() else { requested = false; denied = true }
        }
    }
    DisposableEffect(owner, session) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && requested &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) start()
            // Permission dialogs may pause the Activity: retain only the permission request,
            // never an active preview. A stopped Activity invalidates that request too.
            if (event == Lifecycle.Event.ON_PAUSE) {
                session.stop(); starting = false; active = false
            }
            if (event == Lifecycle.Event.ON_STOP) stop()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); session.stop() }
    }
    BackHandler { stop(); onBack() }
    DrinkTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = { stop(); onBack() }) { Text("Back") }
                Text("Camera setup", style = MaterialTheme.typography.headlineMedium)
                Text("Allow camera access to see a live preview. The camera runs only after you tap Start and while this screen is open.")
                Text("No photos or video are saved or uploaded. Drinking detection is not enabled; this preview does not add water.")
                if (active || starting) {
                    AndroidView(factory = { view }, modifier = Modifier.fillMaxWidth().height(240.dp).clipToBounds())
                }
                Text(when {
                    denied -> "Camera permission was denied. You can still record water manually. Allow camera access in app settings, then tap Start."
                    failed -> "Camera unavailable. Close other camera apps and try Start again."
                    starting -> "Starting camera..."
                    active -> "Camera active - preview only"
                    requested -> "Waiting for camera permission"
                    else -> "Camera stopped"
                })
                if (active || starting || requested) {
                    Button(onClick = { stop() }) { Text("Stop") }
                } else {
                    Button(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) start()
                        else { requested = true; permission.launch(Manifest.permission.CAMERA) }
                    }) { Text("Start") }
                }
                if (denied) TextButton(onClick = {
                    stop()
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
                }) { Text("Open app settings") }
            }
        }
    }
}
