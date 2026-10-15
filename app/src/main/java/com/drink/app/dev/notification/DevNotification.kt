package com.drink.app.dev.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.drink.app.MainActivity
import com.drink.app.R
import com.drink.app.reminders.notification.ReminderNotifier
import com.drink.app.camera.CameraSessionActivity

/** Immediate sample only: does not schedule reminders or access hydration/delivery state. */
object DevNotification {
    const val TAG = "drink_dev_sample"
    const val ID = 101
    const val CAMERA_ID = 102

    fun send(context: Context, channel: String = ReminderNotifier.CHANNEL, cameraReminder: Boolean = false): Boolean {
        ReminderNotifier.createChannel(context, channel)
        if (!ReminderNotifier.allowed(context, channel)) return false
        val id = if (cameraReminder) CAMERA_ID else ID
        val intent = if (cameraReminder) Intent(context, CameraSessionActivity::class.java)
            .setAction(CameraSessionActivity.DEV_TAP).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            else Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val open = PendingIntent.getActivity(context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_water_notification)
            .setContentTitle(if (cameraReminder) "Drink camera reminder test" else "Drink test notification")
            .setContentText(if (cameraReminder) "Test: tap to check camera reminder routing. Camera detection switch must be on. No water is recorded."
                else "This is a sample notification from Dev. No water was recorded.")
            .setContentIntent(open).setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE).build()
        return try {
            context.getSystemService(NotificationManager::class.java).notify(TAG, id, notification)
            true
        } catch (_: SecurityException) { false }
    }
}
