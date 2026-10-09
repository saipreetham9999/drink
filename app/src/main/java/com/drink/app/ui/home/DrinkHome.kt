package com.drink.app.ui.home

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.drink.app.hydration.domain.HydrationRules
import com.drink.app.hydration.model.HomeState
import com.drink.app.hydration.model.PlanRequest
import com.drink.app.reminders.domain.ReminderRules
import com.drink.app.reminders.model.ReminderConfig
import java.time.LocalTime

@Composable
fun SleepSetup(config: ReminderConfig, busy: Boolean, error: Boolean, onSave: (ReminderConfig) -> Unit, onCancel: (() -> Unit)? = null) {
    var sleep by rememberSaveable { mutableStateOf(config.sleepTime.toString()) }
    var wake by rememberSaveable { mutableStateOf(config.wakeTime.toString()) }
    val context = LocalContext.current
    Text("Your sleeping hours", style = MaterialTheme.typography.headlineMedium)
    Text("We will space your water reminders across the hours you are awake.")
    listOf("Sleep time" to sleep, "Wake time" to wake).forEachIndexed { index, (label, value) ->
        OutlinedButton(onClick = {
            val time = LocalTime.parse(value)
            TimePickerDialog(context, { _, hour, minute ->
                val chosen = LocalTime.of(hour, minute).toString()
                if (index == 0) sleep = chosen else wake = chosen
            }, time.hour, time.minute, true).show()
        }, enabled = !busy) { Text("$label: $value") }
    }
    if (error) Text("Could not save. Check that these hours allow your planned attempts, then retry.", color = MaterialTheme.colorScheme.error)
    Button(onClick = { onSave(config.copy(sleepConfigured = true, sleepTime = LocalTime.parse(sleep), wakeTime = LocalTime.parse(wake))) },
        enabled = !busy && sleep != wake) { Text("Save sleeping hours") }
    onCancel?.let { OutlinedButton(onClick = it) { Text("Back") } }
}

@Composable
fun PlanSetup(home: HomeState, config: ReminderConfig, busy: Boolean, error: Boolean,
    onSave: (PlanRequest) -> Unit, onHistory: () -> Unit, onCancel: (() -> Unit)? = null) {
    var goal by rememberSaveable(home.date, home.goalMl) { mutableStateOf((home.plan?.goalMl ?: 2000).toString()) }
    var attempts by rememberSaveable(home.date, home.plan?.attempts) { mutableStateOf((home.plan?.attempts ?: 10).toString()) }
    var weekly by rememberSaveable { mutableStateOf(home.plan?.endsOn?.isAfter(home.date) == true) }
    val g = goal.toIntOrNull()
    val a = attempts.toIntOrNull()
    val valid = g != null && a != null && HydrationRules.validPlan(g, a)
    val fits = valid && ReminderRules.supports(home.date, a!!, config)
    Text("Set your water plan", style = MaterialTheme.typography.headlineMedium)
    Text(home.date.toString())
    OutlinedTextField(goal, { goal = it }, label = { Text("Daily water (mL)") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !busy)
    OutlinedTextField(attempts, { attempts = it }, label = { Text("Number of attempts") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !busy)
    Row {
        RadioButton(!weekly, { weekly = false }, enabled = !busy); Text("Today")
        RadioButton(weekly, { weekly = true }, enabled = !busy); Text("7 days")
    }
    if (valid) {
        val low = g!! / a!!
        val high = low + if (g % a > 0) 1 else 0
        Text(if (low == high) "$low mL per attempt" else "$low–$high mL per attempt, totaling $g mL")
    }
    if (!fits) Text("Use 1–32 attempts, at least 1 mL each, with room for 15-minute reminder spacing.")
    Text("You can edit before recording your first drink. After recording, today's plan is fixed.")
    if (error) Text("Could not save the plan. Try again.", color = MaterialTheme.colorScheme.error)
    Button(onClick = { onSave(PlanRequest(g!!, a!!, if (weekly) 7 else 1)) }, enabled = fits && !busy) { Text("Start plan") }
    OutlinedButton(onClick = onHistory, enabled = !busy) { Text("History") }
    onCancel?.let { OutlinedButton(onClick = it, enabled = !busy) { Text("Cancel") } }
}

@Composable
fun DrinkHome(home: HomeState, busy: Boolean, error: Boolean, onRecord: () -> Unit,
    onExtra: (Int) -> Unit, onEdit: () -> Unit, onRetry: () -> Unit, onHistory: () -> Unit,
    reminderControls: @Composable () -> Unit) {
    val plan = checkNotNull(home.plan)
    Text("Drink", style = MaterialTheme.typography.headlineLarge)
    Text(home.date.toString())
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${home.totalMl} / ${home.goalMl} mL", style = MaterialTheme.typography.headlineMedium)
            Text("${HydrationRules.completionPercent(home.totalMl, home.goalMl)}% completed")
            LinearProgressIndicator(progress = { HydrationRules.progress(home.totalMl, home.goalMl) }, modifier = Modifier.fillMaxWidth())
            Text("${home.recordedAttempts} / ${plan.attempts} attempts recorded")
            if (plan.endsOn > home.date) Text("Daily plan through ${plan.endsOn}")
        }
    }
    val remaining = home.recordedAttempts < plan.attempts
    val amount = if (remaining) HydrationRules.attemptAmount(plan.goalMl, plan.attempts, home.recordedAttempts) else 0
    Button(onClick = onRecord, enabled = !busy && remaining, modifier = Modifier.fillMaxWidth()) {
        Text(if (remaining) "Record $amount mL" else "All attempts recorded")
    }
    if (!home.hasRecords) OutlinedButton(onClick = onEdit, enabled = !busy) { Text("Edit today's plan") }
    else Text("Today's plan is fixed after your first recorded drink.")
    var extraOpen by rememberSaveable { mutableStateOf(false) }
    var extra by rememberSaveable { mutableStateOf("100") }
    OutlinedButton(onClick = { extraOpen = true }, enabled = !busy) { Text("Add extra water") }
    if (extraOpen) AlertDialog(onDismissRequest = { if (!busy) extraOpen = false },
        title = { Text("Extra water") }, text = {
            OutlinedTextField(extra, { extra = it }, label = { Text("Amount (mL)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }, confirmButton = {
            TextButton(onClick = { HydrationRules.validAmount(extra)?.let(onExtra); extraOpen = false },
                enabled = !busy && HydrationRules.validAmount(extra) != null) { Text("Record extra") }
        }, dismissButton = { TextButton(onClick = { extraOpen = false }) { Text("Cancel") } })
    OutlinedButton(onClick = onHistory, enabled = !busy) { Text("History") }
    if (error) { Text("Could not save or load. Try again.", color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = onRetry, enabled = !busy) { Text("Retry") } }
    reminderControls()
    Text("Saved only on this device.", style = MaterialTheme.typography.bodySmall)
}
