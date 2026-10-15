package com.drink.app.hydration.presentation

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.drink.app.hydration.data.HydrationStore
import com.drink.app.hydration.model.DayIntake
import com.drink.app.hydration.model.HistoryDay
import com.drink.app.hydration.model.HomeState
import com.drink.app.hydration.model.PlanRequest
import com.drink.app.hydration.domain.SavePlan
import com.drink.app.hydration.domain.RecordAttempt
import com.drink.app.reminders.model.ReminderConfig
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.Executors

/** Handles hydration actions and screen state; storage work stays off the main thread. */
class HydrationController(
    private val store: HydrationStore,
    historyInitiallyOpen: Boolean = false,
    initiallySelectedDate: LocalDate? = null,
    private val onDataChanged: () -> Unit = {},
    private val currentTime: () -> ZonedDateTime = { ZonedDateTime.now() },
) : AutoCloseable {
    private val storageExecutor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var closed = false
    var home by mutableStateOf<HomeState?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf(false)
        private set
    var historyOpen by mutableStateOf(historyInitiallyOpen)
        private set
    var selectedDate by mutableStateOf(if (historyInitiallyOpen) initiallySelectedDate else null)
        private set
    var history by mutableStateOf<List<HistoryDay>>(emptyList())
        private set
    var detail by mutableStateOf<DayIntake?>(null)
        private set

    private val periodicRefresh = object : Runnable {
        override fun run() {
            retry()
            if (!closed) handler.postDelayed(this, 30_000)
        }
    }

    fun recordAttempt() {
        if (busy || closed) return
        val shown = home ?: return
        val now = currentTime()
        val timestamp = now.toInstant().toEpochMilli()
        update(action = {
            RecordAttempt { plan, completed, recordedAt ->
                store.recordAttempt(plan.date, recordedAt, expectedCompleted = completed, expectedPlan = plan, zone = now.zone)
            }(shown, timestamp, now.zone)
        }, afterSave = onDataChanged)
    }

    fun addExtra(amountMl: Int) {
        val shown = home ?: return
        val now = currentTime()
        if (!busy && !closed) update(action = { store.addExtra(amountMl, shown.date, now.toInstant().toEpochMilli(), now.zone) }, afterSave = onDataChanged)
    }

    fun configurePlan(request: PlanRequest, config: ReminderConfig, afterSave: () -> Unit) {
        val expectedDate = home?.date ?: return
        if (!busy && !closed) update(action = { SavePlan(store::savePlan)(request, config, expectedDate, currentTime()) }, afterSave = {
            onDataChanged(); afterSave()
        })
    }

    fun openHistory() {
        if (busy || closed) return
        historyOpen = true
        update()
    }

    fun selectDate(date: LocalDate) {
        if (busy || closed || !historyOpen || date >= currentTime().toLocalDate() || history.none { it.date == date }) return
        selectedDate = date
        detail = null
        update()
    }

    fun goBack() {
        if (selectedDate != null) { selectedDate = null; detail = null }
        else historyOpen = false
    }

    fun retry() {
        if (!busy && !closed) update()
    }

    fun startRefreshing() {
        if (closed) return
        handler.removeCallbacks(periodicRefresh)
        handler.post(periodicRefresh)
    }

    fun stopRefreshing() {
        handler.removeCallbacks(periodicRefresh)
    }

    private data class Snapshot(val home: HomeState, val history: List<HistoryDay>, val detail: DayIntake?)

    private fun update(action: () -> Unit = {}, afterSave: () -> Unit = {}) {
        busy = true
        val requestedDate = selectedDate
        val readHistory = historyOpen
        storageExecutor.execute {
            val result = runCatching {
                action()
                val today = currentTime().toLocalDate()
                val days = if (readHistory) store.readHistory(today) else emptyList()
                val validSelection = requestedDate?.takeIf { date -> days.any { it.date == date } }
                Snapshot(store.readToday(today), days, validSelection?.let(store::readDay))
            }
            val recovered = if (result.isFailure) runCatching { store.readToday(currentTime().toLocalDate()) }.getOrNull() else null
            handler.post {
                if (!closed) {
                    val oldDate = home?.date
                    home = result.getOrNull()?.home ?: recovered
                    result.getOrNull()?.let { snapshot ->
                        if (readHistory) history = snapshot.history
                        if (readHistory && selectedDate == requestedDate) {
                            detail = snapshot.detail
                            selectedDate = snapshot.detail?.summary?.date
                        }
                    }
                    error = result.isFailure
                    busy = false
                    if (home != null && oldDate != home?.date) onDataChanged()
                    if (result.isSuccess) {
                        afterSave()
                    }
                }
            }
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        stopRefreshing()
        storageExecutor.execute { store.close() }
        storageExecutor.shutdown()
    }
}
