package com.drink.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.drink.app.hydration.domain.HydrationRules
import com.drink.app.hydration.model.HomeState

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

