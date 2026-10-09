package com.drink.app.reminders.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.concurrent.Executors

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        executor.execute {
            try {
                when (intent.action) {
                    ReminderScheduler.ACTION -> ReminderScheduler.deliver(context, intent.getLongExtra(ReminderScheduler.TRIGGER, 0))
                    Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
                    Intent.ACTION_TIMEZONE_CHANGED -> ReminderScheduler.reconcile(context, reset = true)
                    Intent.ACTION_TIME_CHANGED -> ReminderScheduler.clockChanged(context)
                }
            } catch (exception: Exception) { Log.e("DrinkReminders", "Could not handle reminder", exception) }
            finally { result.finish() }
        }
    }

    companion object { private val executor = Executors.newSingleThreadExecutor() }
}
