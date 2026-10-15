package com.drink.app.reminders.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.drink.app.reminders.domain.ReminderPlatform
import com.drink.app.reminders.notification.ReminderNotifier
import java.time.LocalDate

internal class AndroidReminderPlatform(private val context: Context, private val requestCode: Int = 100,
    private val channel: String = ReminderNotifier.CHANNEL, private val notificationId: Int = ReminderNotifier.ID) : ReminderPlatform {
    private fun pending(trigger: Long = 0): PendingIntent = PendingIntent.getBroadcast(
        context, requestCode, Intent(context, ReminderReceiver::class.java).setAction(ReminderScheduler.ACTION)
            .putExtra(ReminderScheduler.TRIGGER, trigger), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    override fun allowed() = ReminderNotifier.allowed(context, channel)
    override fun schedule(trigger: Long) {
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending(trigger))
    }
    override fun cancelAlarm() {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ReminderScheduler.ACTION)
        PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
            context.getSystemService(AlarmManager::class.java).cancel(it)
            it.cancel()
        }
    }
    override fun show(amountMl: Int, date: LocalDate, completed: Int) = ReminderNotifier.show(context, amountMl, date, completed, channel, notificationId)
    override fun clearNotificationUnless(date: LocalDate?, completed: Int, amountMl: Int?) = ReminderNotifier.clearUnless(context, date, completed, amountMl, notificationId)
}
