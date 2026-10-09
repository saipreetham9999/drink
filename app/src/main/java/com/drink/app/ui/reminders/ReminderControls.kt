package com.drink.app.ui.reminders

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.drink.app.reminders.model.ReminderConfig
import com.drink.app.ui.home.SleepSetup

@Composable
fun ReminderControls(config: ReminderConfig, busy: Boolean, error: Boolean, allowed: Boolean,
    onSave: (ReminderConfig) -> Unit, onRetry: () -> Unit, onSettings: () -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    if (editing) SleepSetup(config, busy, error, { onSave(it); editing = false }, { editing = false })
    else {
        Text("Sleep ${config.sleepTime} · Wake ${config.wakeTime}")
        OutlinedButton(onClick = { onSave(config.copy(enabled = !config.enabled)) }, enabled = !busy) {
            Text(if (config.enabled) "Turn reminders off" else "Turn reminders on")
        }
        Text("Reminders follow your attempts during waking hours. Android may delay them.")
        OutlinedButton(onClick = { editing = true }, enabled = !busy) { Text("Change sleeping hours") }
        if (config.enabled && !allowed) TextButton(onClick = onSettings) { Text("Allow notifications") }
        if (error) { Text("Could not update reminders. Check sleeping hours and try again.")
            TextButton(onClick = onRetry, enabled = !busy) { Text("Retry") } }
    }
}
