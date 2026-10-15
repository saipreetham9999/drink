package com.drink.app.camera.presentation

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.*
import com.drink.app.camera.domain.ConfirmDrink
import com.drink.app.camera.domain.PendingDrink
import com.drink.app.hydration.data.HydrationStore
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.Executors

/** Only the explicit confirm action can write intake; camera callbacks can only offer candidates. */
class DrinkConfirmationController(context: Context, databaseName: String,
    private val now: () -> Long = System::currentTimeMillis, private val onRecorded: () -> Unit = {}) : AutoCloseable {
    private val store = HydrationStore(context.applicationContext, databaseName)
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var closed = false
    private var generation = 0
    var pending by mutableStateOf<PendingDrink?>(null); private set
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null); private set

    fun offer() {
        if (closed || busy || pending != null) return
        val epoch = generation; val timestamp = now(); val zone = ZoneId.systemDefault()
        busy = true
        executor.execute {
            val result = runCatching { PendingDrink(UUID.randomUUID().toString(), timestamp,
                store.readToday(java.time.Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate())) }
            main.post {
                if (!closed) {
                    busy = false
                    if (epoch == generation) {
                        pending = result.getOrNull()
                        message = if (result.isFailure) "Could not load today's plan. Try a new suggestion." else null
                    }
                }
            }
        }
    }

    fun reject() { if (!busy) { generation++; pending = null; message = "Suggestion rejected. No water recorded." } }
    fun discard() { generation++; pending = null }
    fun confirm(amount: Int) {
        val candidate = pending ?: return
        if (closed || busy) return
        busy = true
        val timestamp = now(); val zone = ZoneId.systemDefault()
        executor.execute {
            val result = runCatching { ConfirmDrink { offered, ml, planned, at, recordingZone ->
                store.recordConfirmedIntake(offered.token, offered.shown, offered.detectedAt, ml, planned, at, recordingZone)
            }(candidate, amount, timestamp, zone) }
            if (result.getOrNull() == true) runCatching(onRecorded)
            main.post {
                if (!closed) {
                    busy = false
                    if (result.isSuccess) {
                        pending = null
                        message = if (result.getOrNull() == true) "Recorded $amount mL." else "This suggestion was already recorded."
                    } else message = "Could not save. The day, plan or intake may have changed. Retry or reject this suggestion."
                }
            }
        }
    }

    override fun close() { closed = true; generation++; executor.execute { store.close() }; executor.shutdown() }
}
