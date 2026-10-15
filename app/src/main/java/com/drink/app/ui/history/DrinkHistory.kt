package com.drink.app.ui.history

import com.drink.app.ui.theme.DrinkTheme
import com.drink.app.ui.format.displayTime
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.drink.app.R
import com.drink.app.hydration.model.DayIntake
import com.drink.app.hydration.model.HistoryDay
import com.drink.app.hydration.domain.HydrationRules
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun DrinkHistory(
    days: List<HistoryDay>, selectedDate: LocalDate?, detail: DayIntake?,
    busy: Boolean, error: Boolean, onDate: (LocalDate) -> Unit, onBack: () -> Unit, onRetry: () -> Unit,
) {
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    DrinkTheme {
        Surface(Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp),
                contentPadding = PaddingValues(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } }
                item { Text(selectedDate?.format(dateFormat) ?: stringResource(R.string.history),
                    style = MaterialTheme.typography.headlineLarge) }
                if (error) item {
                    Text(stringResource(R.string.storage_error), color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onRetry, enabled = !busy) { Text(stringResource(R.string.retry)) }
                }
                if (busy && (selectedDate == null || detail == null)) item { CircularProgressIndicator() }
                if (selectedDate == null) {
                    if (!busy && !error && days.isEmpty()) item { Text(stringResource(R.string.history_empty)) }
                    items(days, key = { it.date.toString() }) { day ->
                        Card(onClick = { onDate(day.date) }, modifier = Modifier.fillMaxWidth(), enabled = !busy,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(day.date.format(dateFormat), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.amount_ml, day.totalMl))
                                DayCompletion(day)
                            }
                        }
                    }
                } else if (detail != null && detail.summary.date == selectedDate) {
                    item {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(stringResource(R.string.amount_ml, detail.summary.totalMl), style = MaterialTheme.typography.displaySmall)
                                DayCompletion(detail.summary)
                            }
                        }
                    }
                    item { Text(stringResource(R.string.recorded_intake), style = MaterialTheme.typography.titleMedium) }
                    item { Text(stringResource(R.string.history_time_note), style = MaterialTheme.typography.bodySmall) }
                    items(detail.entries, key = { it.id }) { entry ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(displayTime(Instant.ofEpochMilli(entry.recordedAt).atZone(ZoneId.systemDefault()).toLocalTime()))
                            Text(stringResource(R.string.amount_ml, entry.amountMl.toLong()))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCompletion(day: HistoryDay) {
    val goal = day.goalMl
    if (goal == null) Text(stringResource(R.string.goal_unavailable))
    else {
        Text(stringResource(R.string.goal_summary, goal))
        Text(stringResource(R.string.completion, HydrationRules.completionPercent(day.totalMl, goal)))
        LinearProgressIndicator(progress = { HydrationRules.progress(day.totalMl, goal) }, modifier = Modifier.fillMaxWidth())
    }
}
