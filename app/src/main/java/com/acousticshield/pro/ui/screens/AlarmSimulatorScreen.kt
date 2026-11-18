package com.acousticshield.pro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acousticshield.pro.model.*
import com.acousticshield.pro.ui.components.*
import com.acousticshield.pro.ui.theme.*
import com.acousticshield.pro.viewmodel.AcousticViewModel

@Composable
fun AlarmSimulatorScreen(vm: AcousticViewModel) {
    val sourceDb = vm.simulatorInputDb ?: vm.baselineSpeakerDb
    val prediction = AcousticMath.alarm(sourceDb, vm.layer1, vm.layer2, vm.distanceMeters.toDouble())
    var naming by rememberSaveable { mutableStateOf(false) }
    var presetName by rememberSaveable { mutableStateOf("") }
    var showPresets by rememberSaveable { mutableStateOf(false) }
    ScreenColumn {
        ScreenHeading("Find your wake-up balance", "Less muffling.\nMore certainty.", "Explore how layers and distance change an alarm.")
        Panel {
            Text("PREDICTED AT THE BED", style = MaterialTheme.typography.labelMedium, color = MutedSlate)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(prediction.predictedDb.oneDecimal(), fontSize = 64.sp, color = if (prediction.predictedDb >= 75) SafeTeal else DangerCrimson)
                Text("dB", color = MutedSlate, modifier = Modifier.padding(bottom = 14.dp))
            }
            Text(if (vm.simulatorInputDb == null) "From your ${vm.baselineSpeakerDb.oneDecimal()} dB speaker profile" else "From a sent ${sourceDb.oneDecimal()} dB reading · assumed at 1 m", color = MutedSlate, style = MaterialTheme.typography.bodySmall)
            if (vm.simulatorInputDb != null) TextButton(onClick = vm::useSpeakerBaseline) { Text("Use calibrated speaker profile") }
        }
        if (prediction.predictedDb < 75.0) {
            StatusBanner("Risk of Oversleeping: Too Quiet to Wake", "Below the project's 75 dB reference. Reduce barriers or move the phone closer.", DangerCrimson)
        } else {
            StatusBanner("Optimal Wake-Up Volume", "Meets the project's 75 dB reference. Individual wake-up needs vary; test while awake.", SafeTeal)
        }
        Panel {
            Text("Build your setup", style = MaterialTheme.typography.titleMedium)
            MaterialSelector("Barrier layer 1", vm.layer1, vm::setFirst)
            MaterialSelector("Barrier layer 2 · 70% additional loss", vm.layer2, vm::setSecond)
            ValueRow("Distance from bed", "${vm.distanceMeters.toDouble().oneDecimal()} m")
            Slider(value = vm.distanceMeters, onValueChange = vm::setDistance, valueRange = 1f..5f, steps = 7,
                modifier = Modifier.semantics { contentDescription = "Distance from bed in meters" })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("1 m", color = MutedSlate, style = MaterialTheme.typography.bodySmall)
                Text("5 m", color = MutedSlate, style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider(color = MutedSlate.copy(alpha = .2f))
            ValueRow("Combined barrier loss", "−${prediction.barrierLoss.oneDecimal()} dB")
            ValueRow("Distance loss", "−${prediction.distanceLoss.oneDecimal()} dB")
            OutlinedButton(onClick = { presetName = ""; naming = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.BookmarkAdd, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Save alarm setup")
            }
        }
        Panel {
            TextButton(onClick = { showPresets = !showPresets }, modifier = Modifier.fillMaxWidth()) {
                Text("Saved setups (${vm.presets.size})", modifier = Modifier.weight(1f))
                Icon(if (showPresets) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
            }
            if (showPresets) {
                if (vm.presets.isEmpty()) Text("Save your first setup to compare it later.", color = MutedSlate)
                vm.presets.forEach { preset ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { vm.loadPreset(preset) }, modifier = Modifier.weight(1f)) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(preset.name)
                                Text("${preset.layer1.label.substringBefore(" (")} + ${preset.layer2.label.substringBefore(" (")} · ${preset.distance} m", color = MutedSlate, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { vm.deletePreset(preset.id) }) { Icon(Icons.Rounded.DeleteOutline, "Delete ${preset.name}", tint = MutedSlate) }
                    }
                }
                Text("Up to 20 setups. Saved setups use your current speaker profile or sent reading.", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
            }
        }
        ScienceNote("A science-fair prediction, not an alarm guarantee. Fabric losses depend on frequency, coverage, and fit. Free-field distance loss does not capture room reflections. Keep phones uncovered and ventilated; do not cover safety alarms.")
    }
    if (naming) AlertDialog(onDismissRequest = { naming = false }, title = { Text("Name this setup") },
        text = { OutlinedTextField(value = presetName, onValueChange = { presetName = it.take(40) }, label = { Text("Setup name") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { vm.savePreset(presetName); naming = false; showPresets = true }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { naming = false }) { Text("Cancel") } })
}
