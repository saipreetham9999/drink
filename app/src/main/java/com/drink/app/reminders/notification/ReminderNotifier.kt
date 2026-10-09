package com.drink.app.reminders.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.drink.app.MainActivity
import com.drink.app.R

object ReminderNotifier {
    const val CHANNEL = "water_reminders"
    private const val ID = 100

    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.reminders), NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun allowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        return manager.areNotificationsEnabled() && manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun show(context: Context, amountMl: Int) {
        createChannel(context)
        if (!allowed(context)) return
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val open = PendingIntent.getActivity(context, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_water_notification)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText("Time for $amountMl mL. Open Drink to record it.")
            .setContentIntent(open).setAutoCancel(true).setOnlyAlertOnce(false)
            .setVisibility(Notification.VISIBILITY_PRIVATE).build()
        try { context.getSystemService(NotificationManager::class.java).notify(ID, notification) }
        catch (_: SecurityException) { /* Permission can be revoked between checking and posting. */ }
    }

    fun cancel(context: Context) { context.getSystemService(NotificationManager::class.java).cancel(ID) }
}

