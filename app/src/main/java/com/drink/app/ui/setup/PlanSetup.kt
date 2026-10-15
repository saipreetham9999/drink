package com.drink.app.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.drink.app.hydration.domain.HydrationRules
import com.drink.app.ui.format.displayTime
import com.drink.app.hydration.domain.PlanRules
import com.drink.app.hydration.model.HomeState
import com.drink.app.hydration.model.PlanRequest
import com.drink.app.reminders.domain.ReminderRules
import com.drink.app.reminders.model.ReminderConfig
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun PlanSetup(home: HomeState, config: ReminderConfig, busy: Boolean, error: Boolean,
    onSave: (PlanRequest, ReminderConfig) -> Unit, onHistory: () -> Unit, onCancel: (() -> Unit)? = null) {
    var goal by rememberSaveable(home.date) { mutableStateOf((home.plan?.goalMl ?: 2000).toString()) }
    var attempts by rememberSaveable(home.date) { mutableStateOf((home.plan?.attempts ?: 10).toString()) }
    var weekly by rememberSaveable(home.date) { mutableStateOf(home.plan?.endsOn?.isAfter(home.date) == true) }
    var sleep by rememberSaveable { mutableStateOf(editableTime(config.sleepTime)) }
    var sleepPm by rememberSaveable { mutableStateOf(config.sleepTime.hour >= 12) }
    var wake by rememberSaveable { mutableStateOf(editableTime(config.wakeTime)) }
    var wakePm by rememberSaveable { mutableStateOf(config.wakeTime.hour >= 12) }
    val g = goal.toIntOrNull()
    val a = attempts.toIntOrNull()
    val valid = g != null && a != null && HydrationRules.validPlan(g, a)
    val s = enteredTime(sleep, sleepPm)
    val w = enteredTime(wake, wakePm)
    val draft = if (s != null && w != null && s != w) config.copy(sleepConfigured = true, sleepTime = s, wakeTime = w) else null
    val fits = valid && draft != null && PlanRules.valid(PlanRequest(g!!, a!!, if (weekly) 7 else 1), draft, home.date)
    Text("Your water plan", style = MaterialTheme.typography.headlineLarge)
    OutlinedTextField(goal, { goal = it }, label = { Text("Daily goal (mL)") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !busy, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(attempts, { attempts = it }, label = { Text("Attempts") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !busy, modifier = Modifier.weight(1f))
        Column(Modifier.weight(1f).padding(top = 12.dp)) {
            if (valid) {
                val range = HydrationRules.attemptRange(g!!, a!!)
                val low = range.first
                val high = range.last
                Text(if (low == high) "$low mL" else "$low-$high mL", style = MaterialTheme.typography.titleLarge)
                Text("per attempt")
            } else Text("Enter goal and attempts")
        }
    }
    if (!valid) Text("Use a goal of 1-100,000 mL and 1-32 attempts, at least 1 mL each.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(!weekly, { weekly = false }, label = { Text("Today") }, enabled = !busy)
        FilterChip(weekly, { weekly = true }, label = { Text("7 days") }, enabled = !busy)
    }
    Text("Reminders", style = MaterialTheme.typography.titleLarge)
    SleepFields(sleep, sleepPm, wake, wakePm, !busy, { sleep = it }, { sleepPm = it }, { wake = it }, { wakePm = it })
    if (s != null && s == w) Text("Sleep and wake times must be different.")
    if (valid && draft != null && !fits) Text("Use fewer attempts or more waking time. Reminders need at least 15 minutes between them.")
    if (fits) {
        Text("Your reminder times", style = MaterialTheme.typography.titleMedium)
        val slots = ReminderRules.slots(home.date, a!!, draft!!, ZoneId.systemDefault())
        val now = ZonedDateTime.now()
        slots.forEachIndexed { index, slot ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(displayTime(slot.toLocalTime()) + if (slot.toInstant() <= now.toInstant()) " - passed today" else "")
                Text("${HydrationRules.attemptAmount(g!!, a, index)} mL")
            }
        }
        if (weekly) Text("Daily through ${home.date.plusDays(6)}. Times follow your sleeping hours.")
        Text("Passed times are skipped. Android may delay reminders.", style = MaterialTheme.typography.bodySmall)
    }
    if (error) Text("Could not save your plan. Please try again.")
    Button(onClick = { onSave(PlanRequest(g!!, a!!, if (weekly) 7 else 1), draft!!) },
        enabled = !busy && fits, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Saving..." else "Start plan") }
    Text("You can edit today's plan until you record your first drink.", style = MaterialTheme.typography.bodySmall)
    TextButton(onClick = onHistory, enabled = !busy) { Text("History") }
    onCancel?.let { TextButton(onClick = it, enabled = !busy) { Text("Cancel") } }
}

