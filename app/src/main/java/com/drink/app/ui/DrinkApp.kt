package com.drink.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.drink.app.hydration.model.PlanRequest
import com.drink.app.hydration.presentation.HydrationController
import com.drink.app.ui.history.DrinkHistory
import com.drink.app.ui.home.*
import com.drink.app.reminders.model.ReminderConfig
import com.drink.app.reminders.presentation.ReminderController
import com.drink.app.ui.reminders.ReminderControls

@Composable
fun DrinkApp(controller: HydrationController, reminders: ReminderController,
    onSaveReminders: (ReminderConfig) -> Unit, onConfigurePlan: (PlanRequest) -> Unit,
    onNotificationSettings: () -> Unit) {
    var editing by rememberSaveable(controller.home?.date) { mutableStateOf(false) }
    BackHandler(enabled = controller.historyOpen || editing) {
        if (editing) editing = false else controller.goBack()
    }
    if (reminders.ready && reminders.config.sleepConfigured && controller.historyOpen) {
        DrinkHistory(controller.history, controller.selectedDate, controller.detail, controller.busy, controller.error,
            onDate = controller::selectDate, onBack = controller::goBack, onRetry = controller::retry)
    } else MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val home = controller.home
                when {
                    !reminders.ready -> CircularProgressIndicator()
                    !reminders.config.sleepConfigured -> SleepSetup(reminders.config, reminders.busy, reminders.error, onSaveReminders)
                    home == null -> {
                        if (controller.busy) CircularProgressIndicator()
                        if (controller.error) TextButton(onClick = controller::retry) { Text("Retry loading") }
                    }
                    home.plan == null || (editing && !home.hasRecords) ->
                        PlanSetup(home, reminders.config, controller.busy, controller.error,
                            onSave = { onConfigurePlan(it); editing = false },
                            onHistory = controller::openHistory, onCancel = if (home.plan == null) null else ({ editing = false }))
                    else -> DrinkHome(home, controller.busy, controller.error, controller::recordAttempt,
                        controller::addExtra, { editing = true }, controller::retry, controller::openHistory) {
                        ReminderControls(reminders.config, reminders.busy, reminders.error, reminders.notificationsAllowed,
                            onSaveReminders, reminders::refresh, onNotificationSettings)
                    }
                }
            }
        }
    }
}
