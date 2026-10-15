package com.drink.app.ui.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
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
import com.drink.app.camera.domain.CameraSessionLimit
import com.drink.app.camera.domain.DrinkGestureDetector
import com.drink.app.camera.domain.PoseFrame
import com.drink.app.ui.theme.DrinkTheme
import kotlinx.coroutines.delay

@Composable
fun CameraSetup(autoStart: Boolean = false, reminderSession: Boolean = false,
    elapsedRealtime: () -> Long = SystemClock::elapsedRealtime, analysisEnabled: Boolean = false,
    onGesture: () -> Unit = {}, onBackground: () -> Unit = {}, sessionFooter: @Composable () -> Unit = {}, onBack: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val session = remember(context) { CameraPreviewSession(context) }
    val view = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FIT_CENTER
            setBackgroundColor(android.graphics.Color.BLACK)
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
    var autoStartPending by remember { mutableStateOf(autoStart) }
    var timedOut by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableLongStateOf(0L) }
    val limit = remember { CameraSessionLimit() }
    val gestures = remember { DrinkGestureDetector() }
    var pose by remember { mutableStateOf<PoseFrame?>(null) }
    var analysisFailed by remember { mutableStateOf(false) }
    var frames by remember { mutableIntStateOf(0) }
    var inferenceTotal by remember { mutableLongStateOf(0) }
    var inferenceMax by remember { mutableLongStateOf(0) }
    fun stop() {
        autoStartPending = false
        requested = false
        starting = false
        active = false
        limit.stop()
        session.stop()
        pose = null; gestures.reset()
    }
    fun start() {
        if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        if (active || starting) return
        requested = false
        failed = false
        denied = false
        timedOut = false
        starting = true
        gestures.reset(); pose = null; analysisFailed = false; frames = 0; inferenceTotal = 0; inferenceMax = 0
        limit.start(elapsedRealtime())
        remainingSeconds = limit.remainingSeconds(elapsedRealtime())
        session.start(owner, view, onReady = {
            if (limit.remainingMillis(elapsedRealtime()) == 0L) { stop(); timedOut = true }
            else { starting = false; active = true }
        },
            onError = { stop(); failed = true },
            onFrame = if (!analysisEnabled) null else ({ frame ->
                if (limit.remainingMillis(elapsedRealtime()) > 0 && (starting || active)) {
                    pose = frame; frames++; inferenceTotal += frame.inferenceMs
                    inferenceMax = maxOf(inferenceMax, frame.inferenceMs)
                    if (gestures.accept(frame) != null) onGesture()
                }
            }), onDetectionError = { analysisFailed = true; pose = null })
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (requested) {
            if (granted) start() else { requested = false; denied = true }
        }
    }
    fun requestStart() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) start()
        else { requested = true; permission.launch(Manifest.permission.CAMERA) }
    }
    LaunchedEffect(starting, active) {
        while (starting || active) {
            val remaining = limit.remainingMillis(elapsedRealtime())
            remainingSeconds = limit.remainingSeconds(elapsedRealtime())
            if (remaining == 0L) { stop(); timedOut = true; break }
            delay(minOf(remaining, 250L))
        }
    }
    DisposableEffect(owner, session) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (autoStartPending) { autoStartPending = false; requestStart() }
                else if (requested && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) start()
            }
            // Permission dialogs may pause the Activity: retain only the permission request,
            // never an active preview. A stopped Activity invalidates that request too.
            if (event == Lifecycle.Event.ON_PAUSE) {
                session.stop(); limit.stop(); starting = false; active = false
                pose = null; gestures.reset(); onBackground()
            }
            if (event == Lifecycle.Event.ON_STOP) stop()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); session.stop(); limit.stop() }
    }
    BackHandler { stop(); onBack() }
    DrinkTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = { stop(); onBack() }) { Text("Back") }
                Text(if (reminderSession) "Water reminder camera" else "Camera setup", style = MaterialTheme.typography.headlineMedium)
                Text(if (reminderSession) "You opened a water reminder with camera detection on. This visible session stops within 4 minutes or when you leave the app."
                    else "Tap Start for a live preview, for up to 4 minutes. Leaving this screen or the app stops the camera.")
                Text(if (analysisEnabled) "Live movement analysis only. No photos or video saved. Keep your face, shoulders and hands visible; confirm any suggested drink."
                    else "No photos or video are saved or uploaded. This preview does not add water.")
                if (active || starting) {
                    Box(Modifier.fillMaxWidth().height(240.dp).clipToBounds()) {
                        AndroidView(factory = { view }, modifier = Modifier.fillMaxSize())
                        if (analysisEnabled) PoseOverlay(pose, Modifier.fillMaxSize())
                    }
                }
                Text(when {
                    denied -> "Camera permission was denied. You can still record water manually. Allow camera access in app settings, then tap Start."
                    timedOut -> "4-minute limit reached. Camera stopped."
                    failed -> "Camera unavailable. Close other camera apps and try Start again."
                    starting -> "Starting camera..."
                    active && analysisEnabled -> "Camera active - looking for possible drinks"
                    active -> "Camera active - preview only"
                    requested -> "Waiting for camera permission"
                    else -> "Camera stopped"
                })
                if (active || starting) Text("Session ends in ${remainingSeconds / 60}:${(remainingSeconds % 60).toString().padStart(2, '0')}")
                if (active || starting || requested) {
                    Button(onClick = { stop() }) { Text("Stop") }
                } else {
                    Button(onClick = { requestStart() }) { Text("Start") }
                }
                if (analysisEnabled) {
                    Text(when {
                        analysisFailed -> "Detection unavailable. You can still record water manually. Stop and retry."
                        frames == 0 -> "Waiting for pose landmarks..."
                        pose?.points.isNullOrEmpty() -> "No person tracked. Face the camera with shoulders and hands visible."
                        else -> "Tracked landmarks: ${pose?.points?.count { it.confidence >= .7f }} / 33"
                    })
                    if (frames > 0) Text("Analyzed $frames frames; inference avg ${inferenceTotal / frames} ms, max $inferenceMax ms.", style = MaterialTheme.typography.bodySmall)
                }
                sessionFooter()
                if (denied) TextButton(onClick = {
                    stop()
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
                }) { Text("Open app settings") }
            }
        }
    }
}
