package com.drink.app.ui.camera

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.drink.app.camera.presentation.DrinkConfirmationController
import com.drink.app.hydration.domain.HydrationRules

@Composable
fun DrinkCandidatePanel(confirmations: DrinkConfirmationController) {
    val candidate = confirmations.pending
    if (candidate != null) {
        var amount by remember(candidate.token) { mutableStateOf((candidate.shown.nextAmountMl ?: 100).toString()) }
        val ml = HydrationRules.validAmount(amount)
        Text("Possible drink detected", style = MaterialTheme.typography.titleLarge)
        Text("Please confirm what you drank. The camera cannot measure mL or verify swallowing.")
        OutlinedTextField(amount, { amount = it }, enabled = !confirmations.busy, singleLine = true,
            label = { Text("Amount (mL)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        val planned = ml != null && ml == candidate.shown.nextAmountMl
        if (candidate.shown.plan == null) Text("Set today's plan before recording. You can reject this suggestion and return Home.")
        else Text(if (planned) "Uses one planned attempt." else "Records extra water; planned attempts stay unchanged.")
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { ml?.let(confirmations::confirm) },
                enabled = ml != null && candidate.shown.plan != null && !confirmations.busy) {
                Text(if (planned) "Confirm ${ml ?: 0} mL" else "Confirm extra ${ml ?: 0} mL")
            }
            TextButton(onClick = confirmations::reject, enabled = !confirmations.busy) { Text("Reject") }
        }
    }
    if (confirmations.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    confirmations.message?.let { Text(it) }
}
