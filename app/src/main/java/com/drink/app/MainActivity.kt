package com.drink.app

import android.os.Bundle
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import com.drink.app.hydration.data.HydrationStore
import com.drink.app.hydration.presentation.HydrationController
import com.drink.app.ui.DrinkApp
import com.drink.app.reminders.model.ReminderConfig
import com.drink.app.reminders.presentation.ReminderController
import java.time.LocalDate

/** Android lifecycle host; hydration actions live in HydrationController. */
class MainActivity : ComponentActivity() {
    private lateinit var controller: HydrationController
    private lateinit var reminders: ReminderController
    private var pendingReminder: ReminderConfig? = null
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingReminder?.let { reminders.save(it.copy(enabled = granted)) }
        pendingReminder = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reminders = ReminderController(applicationContext)
        @Suppress("DEPRECATION")
        val pending = savedInstanceState?.getSerializable("pendingReminder") as? ReminderConfig
        pendingReminder = pending
        controller = HydrationController(
            store = HydrationStore(applicationContext),
            historyInitiallyOpen = savedInstanceState?.getBoolean("historyOpen") ?: false,
            initiallySelectedDate = savedInstanceState?.getString("selectedDate")?.let(LocalDate::parse),
            onDataChanged = reminders::refresh,
        )
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.WHITE, android.graphics.Color.WHITE),
        )
        setContent { DrinkApp(controller, reminders, onSaveReminders = ::saveReminders,
            onConfigurePlan = { request, config -> reminders.save(config.copy(enabled = false)) {
                controller.configurePlan(request) { saveReminders(config.copy(enabled = true)) }
            } }, onNotificationSettings = {
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            }) }
    }

    private fun saveReminders(config: ReminderConfig) {
        if (config.enabled && Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pendingReminder = config
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else reminders.save(config)
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("historyOpen", controller.historyOpen)
        outState.putString("selectedDate", controller.selectedDate?.toString())
        outState.putSerializable("pendingReminder", pendingReminder)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        controller.startRefreshing()
        reminders.refresh()
    }

    override fun onPause() {
        controller.stopRefreshing()
        super.onPause()
    }

    override fun onDestroy() {
        controller.close()
        reminders.close()
        super.onDestroy()
    }
}




