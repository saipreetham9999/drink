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
import android.os.Bundle
import com.drink.app.camera.CameraSessionActivity
import com.drink.app.camera.data.DetectionPreferences
import com.drink.app.R
import java.time.LocalDate

object ReminderNotifier {
    const val CHANNEL = "water_reminders"
    internal const val ID = 100
    private const val PLAN_DATE = "drink_plan_date"
    private const val COMPLETED = "drink_completed"
    private const val AMOUNT = "drink_amount_ml"

    fun createChannel(context: Context, channel: String = CHANNEL) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(channel, context.getString(R.string.reminders), NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun allowed(context: Context, channel: String = CHANNEL): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        return manager.areNotificationsEnabled() && manager.getNotificationChannel(channel)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun show(context: Context, amountMl: Int, date: LocalDate, completed: Int,
        channel: String = CHANNEL, id: Int = ID): Boolean {
        createChannel(context, channel)
        if (!allowed(context, channel)) return false
        // Recheck opt-in when tapped, including notifications posted before the switch changed.
        val intent = Intent(context, CameraSessionActivity::class.java)
            .setAction(CameraSessionActivity.REMINDER_TAP).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val open = PendingIntent.getActivity(context, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_water_notification)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(if (DetectionPreferences(context).enabled)
                "Time for $amountMl mL. Tap to open a camera session (up to 4 minutes)."
                else "Time for $amountMl mL. Open Drink to record it.")
            .addExtras(Bundle().apply { putString(PLAN_DATE, date.toString()); putInt(COMPLETED, completed); putInt(AMOUNT, amountMl) })
            .setContentIntent(open).setAutoCancel(true).setOnlyAlertOnce(false)
            .setVisibility(Notification.VISIBILITY_PRIVATE).build()
        return try { context.getSystemService(NotificationManager::class.java).notify(id, notification); true }
        catch (_: SecurityException) { false }
    }

    fun clearUnless(context: Context, date: LocalDate?, completed: Int, amountMl: Int?, id: Int = ID) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val active = manager.activeNotifications.firstOrNull { it.id == id } ?: return
        if (date == null || active.notification.extras.getString(PLAN_DATE) != date.toString() ||
            active.notification.extras.getInt(COMPLETED, -1) != completed ||
            active.notification.extras.getInt(AMOUNT, -1) != amountMl) cancel(context, id)
    }

    fun cancel(context: Context, id: Int = ID) { context.getSystemService(NotificationManager::class.java).cancel(id) }
}

