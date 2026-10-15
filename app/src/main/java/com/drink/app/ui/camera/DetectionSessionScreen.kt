package com.drink.app.ui.camera

import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.drink.app.camera.presentation.DrinkConfirmationController
import com.drink.app.reminders.scheduling.ReminderScheduler
import com.drink.app.dev.data.DevTestTime
import com.drink.app.ui.theme.DrinkTheme

@Composable
fun DetectionSessionScreen(autoStart: Boolean = false, testMode: Boolean = false,
    databaseName: String = if (testMode) "dev_hydration.db" else "hydration.db", onBack: () -> Unit) {
    val context = LocalContext.current
    val confirmations = remember(databaseName) {
        DrinkConfirmationController(context, databaseName, now = if (testMode) DevTestTime(context, databaseName)::now else System::currentTimeMillis, onRecorded = {
            if (!testMode) ReminderScheduler.reconcile(context)
        })
    }
    DisposableEffect(confirmations) { onDispose { confirmations.close() } }
    val feedback = remember { SnackbarHostState() }
    LaunchedEffect(confirmations.message) { confirmations.message?.let { feedback.showSnackbar(it) } }
    Box(Modifier.fillMaxSize()) {
    CameraSetup(autoStart = autoStart, reminderSession = !testMode, analysisEnabled = true,
        onGesture = { confirmations.offer() }, onBackground = confirmations::discard,
        sessionFooter = {
            if (testMode) Text("DEV TEST - confirmations use the hydration sandbox and its test date. Set a test plan in Dev first.")
        }, onBack = onBack)
        SnackbarHost(feedback, Modifier.align(Alignment.BottomCenter).safeDrawingPadding())
    }
    if (confirmations.pending != null) DrinkTheme {
        AlertDialog(onDismissRequest = { if (!confirmations.busy) confirmations.reject() },
            confirmButton = {}, text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DrinkCandidatePanel(confirmations)
                }
            })
    }
}
