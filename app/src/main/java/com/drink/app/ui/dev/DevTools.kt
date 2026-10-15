package com.drink.app.ui.dev

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.drink.app.ui.theme.DrinkTheme
import com.drink.app.dev.notification.DevNotification
import com.drink.app.ui.camera.DetectionControl

@Composable
fun DevTools(onBack: () -> Unit, onCamera: () -> Unit, onDetection: () -> Unit) {
    val context = LocalContext.current
    var notificationStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var blocked by rememberSaveable { mutableStateOf(false) }
    var cameraReminder by rememberSaveable { mutableStateOf(false) }
    var sandbox by rememberSaveable { mutableStateOf<String?>(null) }
    fun sendSample() {
        val sent = DevNotification.send(context, cameraReminder = cameraReminder)
        blocked = !sent
        notificationStatus = if (sent && cameraReminder) "Camera test reminder sent. Tap it in your notification panel; the Camera detection switch controls whether the camera opens."
            else if (sent) "Sample sent. Check your notification panel."
            else "Notifications are blocked. Allow them in notification settings, then try again."
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) sendSample() else {
            blocked = true
            notificationStatus = "Notification permission denied. Allow it in notification settings, then try again."
        }
    }
    fun requestSample(camera: Boolean) {
        cameraReminder = camera
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else sendSample()
    }
    if (sandbox != null) {
        if (sandbox == "confirmation") ConfirmationTestScreen(onBack = { sandbox = null })
        else HydrationSandbox(historyInitiallyOpen = sandbox == "history", onBack = { sandbox = null })
        return
    }
    DrinkTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = onBack) { Text("Back") }
                Text("Dev", style = MaterialTheme.typography.headlineMedium)
                Text("Test features under development.")
                Text("Camera", style = MaterialTheme.typography.titleLarge)
                OutlinedButton(onClick = onCamera, modifier = Modifier.fillMaxWidth()) { Text("Camera setup") }
                OutlinedButton(onClick = onDetection, modifier = Modifier.fillMaxWidth()) { Text("Test drinking detection") }
                OutlinedButton(onClick = { sandbox = "confirmation" }, modifier = Modifier.fillMaxWidth()) { Text("Test drink confirmation") }
                DetectionControl()
                Text("This switch is shared with Home. Camera setup tests Start/Stop and the 4-minute limit. The camera reminder below tests notification taps.",
                    style = MaterialTheme.typography.bodySmall)
                Text("Notifications", style = MaterialTheme.typography.titleLarge)
                OutlinedButton(onClick = { requestSample(false) }, modifier = Modifier.fillMaxWidth()) { Text("Test notification") }
                OutlinedButton(onClick = { requestSample(true) }, modifier = Modifier.fillMaxWidth()) { Text("Test camera reminder") }
                notificationStatus?.let { Text(it) }
                if (blocked) TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }) { Text("Notification settings") }
                Text("Hydration and reminders", style = MaterialTheme.typography.titleLarge)
                OutlinedButton(onClick = { sandbox = "hydration" }, modifier = Modifier.fillMaxWidth()) { Text("Test plans and intake") }
                OutlinedButton(onClick = { sandbox = "history" }, modifier = Modifier.fillMaxWidth()) { Text("Test history") }
                Text("Sandbox tests use separate data. Try goal/attempt calculations, sleep/reminder previews, recording, extras, history, next-day reset and seven-day expiry. Timed/background notification delivery still needs phone testing.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
