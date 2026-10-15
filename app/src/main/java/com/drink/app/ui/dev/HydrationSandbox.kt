package com.drink.app.ui.dev

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.drink.app.hydration.data.HydrationStore
import com.drink.app.hydration.presentation.HydrationController
import com.drink.app.reminders.model.ReminderConfig
import com.drink.app.ui.history.DrinkHistory
import com.drink.app.ui.home.DrinkHome
import com.drink.app.ui.setup.PlanSetup
import com.drink.app.ui.theme.DrinkTheme
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicReference

/** Uses the production actions/screens, with its own database and virtual calendar. */
@Composable
fun HydrationSandbox(historyInitiallyOpen: Boolean = false, databaseName: String = "dev_hydration.db", onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context, databaseName) { context.getSharedPreferences(databaseName, Context.MODE_PRIVATE) }
    val clock = remember {
        val date = runCatching { LocalDate.parse(preferences.getString("date", null)) }.getOrDefault(LocalDate.now())
        AtomicReference(date.atTime(12, 0).atZone(ZoneId.systemDefault()))
    }
    val controller = remember {
        HydrationController(HydrationStore(context, databaseName),
            historyInitiallyOpen = historyInitiallyOpen, currentTime = clock::get)
    }
    var editing by rememberSaveable { mutableStateOf(false) }
    var config by rememberSaveable { mutableStateOf(ReminderConfig(sleepConfigured = true)) }
    DisposableEffect(controller) {
        controller.retry()
        onDispose { controller.close() }
    }
    BackHandler {
        if (controller.historyOpen) controller.goBack() else if (editing) editing = false else onBack()
    }
    fun advance(days: Long) {
        if (controller.busy) return
        val next = clock.get().plusDays(days)
        clock.set(next)
        preferences.edit().putString("date", next.toLocalDate().toString()).apply()
        editing = false
        controller.retry()
    }
    if (controller.historyOpen) DrinkTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Text("DEV TEST HISTORY - separate test records", Modifier.safeDrawingPadding().padding(horizontal = 24.dp))
                Box(Modifier.weight(1f)) {
                    DrinkHistory(controller.history, controller.selectedDate, controller.detail, controller.busy, controller.error,
                        controller::selectDate, controller::goBack, controller::retry)
                }
            }
        }
    } else DrinkTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = onBack) { Text("Back to Dev") }
                Text("Hydration sandbox", style = MaterialTheme.typography.headlineMedium)
                Text("TEST DATA ONLY. Plans and drinks here use separate local storage and persist between visits. No real reminders are scheduled.")
                Text("Try goals, attempts, extra water, plan locking, reminder-time previews and history. New setup forms start with default sleep hours.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { advance(1) }, enabled = !controller.busy) { Text("Next test day") }
                    OutlinedButton(onClick = { advance(7) }, enabled = !controller.busy) { Text("+7 test days") }
                }
                val home = controller.home
                when {
                    home == null -> {
                        if (controller.busy) CircularProgressIndicator()
                        if (controller.error) TextButton(onClick = controller::retry) { Text("Retry") }
                    }
                    home.plan == null || (editing && !home.hasRecords) ->
                        PlanSetup(home, config, controller.busy, controller.error,
                            onSave = { plan, hours -> controller.configurePlan(plan, hours) { config = hours; editing = false } },
                            onHistory = controller::openHistory,
                            onCancel = if (home.plan == null) null else ({ editing = false }))
                    else -> DrinkHome(home, controller.busy, controller.error, controller::recordAttempt,
                        controller::addExtra, { editing = true }, controller::retry, controller::openHistory) {
                        Text("Test date: ${home.date}. Sleep/reminder slots can be checked in plan setup; notification delivery is tested from Dev.")
                    }
                }
            }
        }
    }
}
