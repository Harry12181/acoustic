package com.acousticshield.pro.ui.screens

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
import com.acousticshield.pro.model.oneDecimal
import com.acousticshield.pro.ui.components.*
import com.acousticshield.pro.ui.theme.*
import com.acousticshield.pro.viewmodel.AcousticViewModel
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalibrationScreen(vm: AcousticViewModel) {
    val profiles = listOf("Samsung Galaxy A15 5G" to 95.0, "iPhone 15 Pro" to 92.5, "Google Pixel 8" to 90.0)
    val profile = profiles.firstOrNull { abs(it.second - vm.baselineSpeakerDb) < 0.01 }?.first ?: "Custom"
    var expanded by rememberSaveable { mutableStateOf(false) }
    ScreenColumn {
        ScreenHeading("Make it yours", "One phone.\nA better baseline.", "Tune the speaker model and microphone correction.")
        Panel {
            Text("Speaker profile", style = MaterialTheme.typography.titleMedium)
            Text("$profile · ${vm.baselineSpeakerDb.oneDecimal()} dB", color = SafeTeal, style = MaterialTheme.typography.headlineSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                profiles.forEach { (name, db) ->
                    FilterChip(selected = profile == name, onClick = { vm.setBaseline(db) }, label = { Text(name) })
                }
                if (profile == "Custom") Surface(color = SafeTeal.copy(alpha = .12f), shape = MaterialTheme.shapes.small) {
                    Text("Custom profile active", color = SafeTeal, modifier = Modifier.padding(10.dp))
                }
            }
            ValueRow("Speaker baseline at 1 m", "${vm.baselineSpeakerDb.oneDecimal()} dB")
            Slider(value = vm.baselineSpeakerDb.toFloat(), onValueChange = { vm.setBaseline(it.toDouble()) }, valueRange = 80f..105f, steps = 49,
                modifier = Modifier.semantics { contentDescription = "Speaker baseline in decibels" })
            Text("80 dB                                      105 dB", color = MutedSlate, style = MaterialTheme.typography.bodySmall)
            Text("Saved automatically · used immediately by the alarm simulator", color = SafeTeal, style = MaterialTheme.typography.bodySmall)
            Text("Device presets are project assumptions, not manufacturer specifications or measurements of this phone.", color = MutedSlate, style = MaterialTheme.typography.bodySmall)
        }
        Panel {
            Text("Microphone correction", style = MaterialTheme.typography.titleMedium)
            ValueRow("Calibration offset", "${if (vm.calibrationOffsetDb >= 0) "+" else ""}${vm.calibrationOffsetDb.oneDecimal()} dB")
            Slider(value = vm.calibrationOffsetDb.toFloat(), onValueChange = { vm.setOffset(it.toDouble()) }, valueRange = -20f..20f, steps = 79,
                modifier = Modifier.semantics { contentDescription = "Microphone calibration offset in decibels" })
            Text("Compare a steady sound with a reference meter. Set the correction to reference reading minus phone reading. A single offset cannot correct frequency response or automatic gain control.", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
            TextButton(onClick = { vm.setOffset(0.0) }) { Text("Reset correction to 0 dB") }
            if (vm.sessionExists) Text("Changing correction saves the current session so its calibration stays consistent.", color = CautionAmber, style = MaterialTheme.typography.bodySmall)
        }
        Panel {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                Text("About the Science Fair Data", modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
            }
            if (expanded) {
                Text("Empirical barriers", style = MaterialTheme.typography.titleSmall, color = SafeTeal)
                Text("The supplied project dataset assigns measured losses to wool (21.7 dB), cotton (19.5 dB), silk (13.6 dB), polyester (8.3 dB), and sponge foam (5.3 dB). The brief provides no raw observations or test protocol; these constants are not universal material ratings.", color = MutedSlate)
                Text("Stacking & distance", style = MaterialTheme.typography.titleSmall, color = SafeTeal)
                Text("The second layer adds 70% of its listed loss. The inverse-square free-field model subtracts 20 × log10(distance / 1 m). Prediction floors at 20 dB. Indoors, reflections and frequency content can change the result.", color = MutedSlate)
                Text("Classroom comparison", style = MaterialTheme.typography.titleSmall, color = SafeTeal)
                Text("Affordable area = budget / price. Coverage saturates at 100 sq ft. Projected reduction = empirical loss × coverage, with a 30 dB room floor. This is a demonstration, not a validated room-acoustics design tool.", color = MutedSlate)
                Text("NIOSH exchange rate", style = MaterialTheme.typography.titleSmall, color = SafeTeal)
                Text("The reference is 8 hours at 85 dBA, with exposure time halving for every 3 dB increase: 88 dBA → 4 h; 100 dBA → 15 min. This app estimates unweighted dB and does not implement a certified A-weighted dosimeter.", color = MutedSlate)
                Text("Sources & scope", style = MaterialTheme.typography.titleSmall, color = SafeTeal)
                Text("NIOSH Criteria for a Recommended Standard: Occupational Noise Exposure (1998), DHHS publication 98-126. Empirical barrier and speaker constants are supplied by the science-fair brief. No material listed here is certified ear protection.", color = MutedSlate)
            }
        }
        ScienceNote("Private by design: no network permission, no audio files, and no analytics. Settings, the latest 100 session summaries, and 20 alarm setups stay on this device. CSV sharing happens only when you choose it.")
    }
}
