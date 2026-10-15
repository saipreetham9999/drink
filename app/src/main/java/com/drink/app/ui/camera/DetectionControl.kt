package com.drink.app.ui.camera

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.drink.app.camera.data.DetectionPreferences

@Composable
fun DetectionControl() {
    val context = LocalContext.current
    val preferences = remember(context) { DetectionPreferences(context) }
    var enabled by remember { mutableStateOf(preferences.enabled) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Camera detection", style = MaterialTheme.typography.titleMedium)
            Switch(checked = enabled, onCheckedChange = { preferences.enabled = it; enabled = it },
                modifier = Modifier.semantics { contentDescription = "Camera detection" })
        }
        Text("When on, tapping a water reminder starts a visible camera session for up to 4 minutes. " +
            "Possible drinks need confirmation before water is recorded. No video is saved.",
            style = MaterialTheme.typography.bodySmall)
    }
}
