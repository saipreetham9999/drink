package com.drink.app.ui.dev

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.drink.app.camera.presentation.DrinkConfirmationController
import com.drink.app.dev.data.DevTestTime
import com.drink.app.ui.camera.DrinkCandidatePanel
import com.drink.app.ui.theme.DrinkTheme

@Composable
fun ConfirmationTestScreen(databaseName: String = "dev_hydration.db", onBack: () -> Unit) {
    val context = LocalContext.current
    val controller = remember(databaseName) { DrinkConfirmationController(context, databaseName, DevTestTime(context, databaseName)::now) }
    DisposableEffect(controller) { onDispose { controller.close() } }
    BackHandler(onBack = onBack)
    DrinkTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = onBack) { Text("Back to Dev") }
                Text("Test drink confirmation", style = MaterialTheme.typography.headlineMedium)
                Text("Simulated suggestion only. Uses the hydration sandbox's separate records and test date. First save a plan in Test plans and intake.")
                Button(onClick = controller::offer, enabled = !controller.busy && controller.pending == null) { Text("Simulate possible drink") }
                DrinkCandidatePanel(controller)
            }
        }
    }
}
