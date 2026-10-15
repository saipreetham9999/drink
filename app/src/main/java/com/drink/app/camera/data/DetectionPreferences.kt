package com.drink.app.camera.data

import android.content.Context

/** Explicit opt-in; independent of reminder enablement and hydration records. */
class DetectionPreferences(context: Context, name: String = "camera_detection") {
    private val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    var enabled: Boolean
        get() = preferences.getBoolean("enabled", false)
        set(value) { preferences.edit().putBoolean("enabled", value).apply() }
}
