package com.drink.app.reminders.presentation

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.drink.app.reminders.data.ReminderStore
import com.drink.app.reminders.model.ReminderConfig
import com.drink.app.reminders.notification.ReminderNotifier
import com.drink.app.reminders.scheduling.ReminderScheduler
import java.util.concurrent.Executors

class ReminderController(context: Context) : AutoCloseable {
    private val context = context.applicationContext
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var closed = false
    private var pendingOperations = 0
    var config by mutableStateOf(ReminderConfig())
        private set
    var busy by mutableStateOf(true)
        private set
    var error by mutableStateOf(false)
        private set
    var notificationsAllowed by mutableStateOf(false)
        private set

    var ready by mutableStateOf(false)
        private set
    fun refresh() { update() }
    private var loaded = false

    fun save(value: ReminderConfig, afterSave: () -> Unit = {}) {
        if (!closed) update(action = { ReminderScheduler.configure(context, value) }, afterSave = afterSave)
    }

    private fun update(action: () -> Unit = {}, afterSave: () -> Unit = {}) {
        if (closed) return
        busy = true
        pendingOperations++
        loaded = true
        executor.execute {
            val result = runCatching {
                action()
                ReminderScheduler.reconcile(context)
                ReminderStore(context).read()
            }
            val recovered = if (result.isFailure) runCatching { ReminderStore(context).read() }.getOrNull() else null
            val allowed = ReminderNotifier.allowed(context)
            handler.post {
                if (!closed) {
                    ready = true
                    config = result.getOrNull() ?: recovered ?: config
                    notificationsAllowed = allowed
                    error = result.isFailure
                    pendingOperations--
                    busy = pendingOperations > 0
                    if (result.isSuccess) afterSave()
                }
            }
        }
    }

    override fun close() { closed = true; executor.shutdown() }
}


