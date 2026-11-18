package com.acousticshield.pro.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.acousticshield.pro.model.*
import com.acousticshield.pro.ui.components.*
import com.acousticshield.pro.ui.theme.*
import com.acousticshield.pro.viewmodel.AcousticViewModel

@Composable
fun NioshTrackerScreen(vm: AcousticViewModel) {
    val net = AcousticMath.netExposure(vm.environmentalNoiseDb, vm.protection)
    val belowReference = net < 85.0
    val allowedSeconds = AcousticMath.allowedHours(net) * 3600
    var plannedMinutes by rememberSaveable { mutableFloatStateOf(60f) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    val plannedDose = AcousticMath.dosePercent(net, plannedMinutes * 60.0)
    val previewProgress by animateFloatAsState(if (belowReference) .15f else (plannedDose / 100).toFloat().coerceIn(0f, 1f), label = "planned allowance")
    val actualProgress by animateFloatAsState((vm.dailyDose / 100).toFloat().coerceIn(0f, 1f), label = "daily dose")
    ScreenColumn {
        ScreenHeading("Make room for quiet", "Your ears.\nYour daily allowance.", "Explore the NIOSH 85 dBA / 8-hour reference model.")
        StatusBanner("Educational exposure estimate", "Fabric is not certified hearing protection. Phone readings are unweighted estimates, so this app cannot determine a safe listening limit.", CautionAmber)
        Panel {
            Text("EXPOSURE PLANNER", style = MaterialTheme.typography.labelMedium, color = SafeTeal)
            ValueRow("Environmental noise", "${vm.environmentalNoiseDb.oneDecimal()} dB", soundColor(vm.environmentalNoiseDb))
            Slider(value = vm.environmentalNoiseDb.toFloat().coerceIn(75f, 120f), onValueChange = { vm.setEnvironment(it.toDouble()) }, valueRange = 75f..120f,
                modifier = Modifier.semantics { contentDescription = "Environmental noise level in decibels" })
            if (vm.environmentalNoiseDb < 75 || vm.environmentalNoiseDb > 120) {
                Text("Sent meter reading retained. Moving the slider selects 75–120 dB.", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
            }
            MaterialSelector("Hypothetical barrier · not ear protection", vm.protection, vm::updateProtection)
            ValueRow("Modeled net exposure", "${net.oneDecimal()} dB", soundColor(net))
            HorizontalDivider(color = MutedSlate.copy(alpha = .2f))
            Text(if (belowReference) "8+ hours" else AcousticMath.duration(allowedSeconds), style = MaterialTheme.typography.headlineLarge, color = if (belowReference) SafeTeal else CautionAmber)
            Text(if (belowReference) "Below 85 dB · no limit in the simplified project model" else "Reference duration at this constant level", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
            ValueRow("Planned exposure", "${plannedMinutes.toInt()} min")
            Slider(value = plannedMinutes, onValueChange = { plannedMinutes = it }, valueRange = 5f..480f, steps = 94,
                modifier = Modifier.semantics { contentDescription = "Planned exposure in minutes" })
            LinearProgressIndicator(progress = { previewProgress }, modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (plannedDose < 100) SafeTeal else DangerCrimson, trackColor = MutedSlate.copy(alpha = .15f))
            ValueRow("Estimated planned dose", "${plannedDose.oneDecimal()}%", if (plannedDose < 100) SafeTeal else DangerCrimson)
            Text("Below 85 dB, the project indicator uses a fixed 15% fill. Dose math integrates levels at or above 80 dB; below 85 dB still contributes.", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
        }
        Panel {
            Text("TODAY · MICROPHONE SESSIONS", style = MaterialTheme.typography.labelMedium, color = MutedSlate)
            Text("${vm.dailyDose.oneDecimal()}%", style = MaterialTheme.typography.headlineLarge, color = if (vm.dailyDose < 100) SafeTeal else DangerCrimson)
            LinearProgressIndicator(progress = { actualProgress }, modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (vm.dailyDose < 100) SafeTeal else DangerCrimson, trackColor = MutedSlate.copy(alpha = .15f))
            Text("Accumulated only while a microphone session is recording, using its actual estimate without fabric deductions. Paused periods and manual simulations are excluded. Starts a new ledger each local day.", color = MutedSlate, style = MaterialTheme.typography.bodySmall)
            Text("At the planner's current level: ${AcousticMath.duration(allowedSeconds * (1 - vm.dailyDose / 100).coerceAtLeast(0.0))} of model allowance remaining.", style = MaterialTheme.typography.bodyMedium)
            if (vm.dailyDose >= 100) StatusBanner("Daily model allowance reached", "Reduce sound exposure and use properly fitted, certified protection when needed.", DangerCrimson)
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) { Text("Reset today's estimate") }
        }
        ScienceNote("NIOSH: T = 8 / 2^((L − 85) / 3) hours. The recommendation uses A-weighted occupational measurements. Below 85 dB is not unlimited safety. This app's unweighted calculations and empirical fabric data are educational approximations.")
    }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false }, title = { Text("Reset today's dose?") },
        text = { Text("This resets the microphone estimate for today. Saved session summaries stay available.") },
        confirmButton = { TextButton(onClick = { vm.clearDailyDose(); confirmReset = false }) { Text("Reset") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } })
}
