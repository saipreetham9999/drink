package com.drink.app.camera

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import com.drink.app.MainActivity
import com.drink.app.camera.data.DetectionPreferences
import com.drink.app.ui.camera.DetectionSessionScreen

/** A notification tap opens this visible Activity directly; no background camera service. */
class CameraSessionActivity : ComponentActivity() {
    companion object {
        const val REMINDER_TAP = "com.drink.app.OPEN_REMINDER_CAMERA"
        const val DEV_TAP = "com.drink.app.OPEN_DEV_REMINDER_CAMERA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!DetectionPreferences(this).enabled || intent.action !in setOf(REMINDER_TAP, DEV_TAP)) {
            openHome()
            return
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.WHITE, android.graphics.Color.WHITE))
        setContent {
            DetectionSessionScreen(autoStart = savedInstanceState == null, testMode = intent.action == DEV_TAP,
                databaseName = if (intent.action == DEV_TAP) "dev_hydration.db" else "hydration.db", onBack = ::openHome)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isFinishing && !DetectionPreferences(this).enabled) openHome()
    }

    private fun openHome() {
        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        finish()
    }
}
