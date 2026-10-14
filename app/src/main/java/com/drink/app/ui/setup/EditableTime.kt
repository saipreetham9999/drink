package com.drink.app.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
fun displayTime(time: LocalTime): String = time.format(timeFormat)
fun enteredTime(value: String, pm: Boolean): LocalTime? {
    val parts = value.split(":")
    if (parts.size != 2 || !parts[0].matches(Regex("[0-9]{1,2}")) || !parts[1].matches(Regex("[0-9]{2}"))) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 1..12 || minute !in 0..59) return null
    return LocalTime.of(hour % 12 + if (pm) 12 else 0, minute)
}

@Composable
fun EditableTime(label: String, value: String, pm: Boolean, enabled: Boolean,
    onValue: (String) -> Unit, onPeriod: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value, onValue, modifier = Modifier.weight(1f), label = { Text(label) },
            placeholder = { Text("10:00") }, singleLine = true, enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            isError = enteredTime(value, pm) == null)
        Column {
            Text("$label AM/PM", style = MaterialTheme.typography.labelSmall)
            Row {
                FilterChip(selected = !pm, onClick = { onPeriod(false) }, label = { Text("AM") }, enabled = enabled)
                Spacer(Modifier.width(6.dp))
                FilterChip(selected = pm, onClick = { onPeriod(true) }, label = { Text("PM") }, enabled = enabled)
            }
        }
    }
}

@Composable
fun SleepFields(sleep: String, sleepPm: Boolean, wake: String, wakePm: Boolean, enabled: Boolean,
    onSleep: (String) -> Unit, onSleepPm: (Boolean) -> Unit, onWake: (String) -> Unit, onWakePm: (Boolean) -> Unit) {
    EditableTime("Sleep time", sleep, sleepPm, enabled, onSleep, onSleepPm)
    EditableTime("Wake time", wake, wakePm, enabled, onWake, onWakePm)
    Text("Type a time like 10:00 and choose AM or PM.", style = MaterialTheme.typography.bodySmall)
}

fun editableTime(time: LocalTime): String = time.format(DateTimeFormatter.ofPattern("h:mm", Locale.US))

