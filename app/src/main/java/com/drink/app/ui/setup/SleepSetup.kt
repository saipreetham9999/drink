package com.drink.app.ui.setup

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.drink.app.reminders.model.ReminderConfig

@Composable
fun SleepSetup(config: ReminderConfig, busy: Boolean, error: Boolean,
    onSave: (ReminderConfig) -> Unit, onCancel: (() -> Unit)? = null) {
    var sleep by rememberSaveable { mutableStateOf(editableTime(config.sleepTime)) }
    var sleepPm by rememberSaveable { mutableStateOf(config.sleepTime.hour >= 12) }
    var wake by rememberSaveable { mutableStateOf(editableTime(config.wakeTime)) }
    var wakePm by rememberSaveable { mutableStateOf(config.wakeTime.hour >= 12) }
    val s = enteredTime(sleep, sleepPm)
    val w = enteredTime(wake, wakePm)
    Text("Sleeping hours", style = MaterialTheme.typography.headlineMedium)
    SleepFields(sleep, sleepPm, wake, wakePm, !busy, { sleep = it }, { sleepPm = it }, { wake = it }, { wakePm = it })
    if (s != null && s == w) Text("Sleep and wake times must be different.")
    if (error) Text("Could not save. Check that these hours allow your planned reminders.")
    Button(onClick = { onSave(config.copy(sleepConfigured = true, sleepTime = s!!, wakeTime = w!!)) },
        enabled = !busy && s != null && w != null && s != w) { Text("Save sleeping hours") }
    onCancel?.let { TextButton(onClick = it, enabled = !busy) { Text("Back") } }
}
