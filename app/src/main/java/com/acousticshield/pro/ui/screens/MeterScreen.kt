package com.acousticshield.pro.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acousticshield.pro.model.AcousticMath
import com.acousticshield.pro.model.oneDecimal
import com.acousticshield.pro.ui.components.*
import com.acousticshield.pro.ui.theme.*
import com.acousticshield.pro.viewmodel.AcousticViewModel

@Composable
fun MeterScreen(vm: AcousticViewModel, onSend: () -> Unit, onHistory: () -> Unit) {
    val context = LocalContext.current
    val frame by vm.frames.collectAsStateWithLifecycle()
    val hasMic = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE) }
    var attemptedPermission by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        attemptedPermission = true
        vm.setPermission(granted)
        if (granted && !vm.manualMode) vm.toggleLive()
    }
    val color = soundColor(vm.currentDb)
    ScreenColumn {
        ScreenHeading("Understand your surroundings", "A little quieter.\nA little healthier.", "A live view of the sound around you.")
        if ((!vm.permissionGranted || !hasMic) && !vm.manualMode) {
            Panel {
                Icon(Icons.Rounded.Mic, null, tint = SafeTeal)
                Text(if (hasMic) "Let sound tell its story" else "Explore without a microphone", style = MaterialTheme.typography.titleMedium)
                Text(if (hasMic) "Allow microphone access to measure. Audio stays on this device and is never saved." else "This device has no microphone. Manual mode lets you explore every tool.", color = MutedSlate)
                if (hasMic) {
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Grant Microphone Access")
                    }
                    if (attemptedPermission) TextButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                    }) { Text("Open app settings") }
                }
                OutlinedButton(onClick = { vm.setManual(true) }, modifier = Modifier.fillMaxWidth()) { Text("Try manual mode") }
            }
        }
        Panel {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (vm.manualMode) "MANUAL SIMULATION" else if (frame.running) "LIVE ESTIMATE" else "MICROPHONE PAUSED", color = SafeTeal, style = MaterialTheme.typography.labelMedium)
                Icon(if (vm.manualMode) Icons.Rounded.TouchApp else Icons.Rounded.Mic, null, tint = MutedSlate, modifier = Modifier.size(18.dp))
            }
            DecibelGauge(vm.currentDb)
            Text(when { vm.currentDb < 70 -> "Comfortable ambient sound"; vm.currentDb < 85 -> "Give your ears a quiet break"; else -> "Loud environment · reduce exposure" }, color = color,
                modifier = Modifier.align(Alignment.CenterHorizontally), style = MaterialTheme.typography.titleSmall)
            HorizontalDivider(color = MutedSlate.copy(alpha = .15f))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("PEAK HOLD", color = MutedSlate, style = MaterialTheme.typography.labelSmall)
                    Text("${vm.peakDb.oneDecimal()} dB", style = MaterialTheme.typography.titleLarge)
                }
                IconButton(onClick = vm::resetPeak) { Icon(Icons.Rounded.RestartAlt, "Reset Peak", tint = SafeTeal) }
            }
            if (!vm.manualMode) {
                Waveform(if (frame.running) frame.waveform else List(60) { 0f })
                Button(onClick = {
                    if (vm.permissionGranted) vm.toggleLive() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }, enabled = hasMic, modifier = Modifier.fillMaxWidth()) {
                    Icon(if (vm.liveRequested) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (vm.liveRequested) "Pause microphone" else "Start microphone")
                }
                if (frame.error != null) StatusBanner("Microphone unavailable", frame.error!!, CautionAmber)
            } else {
                Text("Test level · ${vm.manualDb.toDouble().oneDecimal()} dB", color = MutedSlate)
                Slider(value = vm.manualDb, onValueChange = vm::updateManualDb, valueRange = 30f..120f,
                    modifier = Modifier.semantics { contentDescription = "Manual test decibel level" })
                Text("Simulated input · no live microphone data", style = MaterialTheme.typography.bodySmall, color = CautionAmber)
            }
        }
        Button(onClick = onSend, enabled = vm.manualMode || frame.running, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
            Icon(Icons.AutoMirrored.Rounded.Send, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Send to Simulator & NIOSH")
        }
        SessionPanel(vm, onHistory, vm.manualMode || frame.running)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Manual test mode", style = MaterialTheme.typography.titleSmall)
                Text("Explore with a simulated noise level", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
            }
            Switch(checked = vm.manualMode, onCheckedChange = vm::setManual,
                modifier = Modifier.semantics { contentDescription = "Manual test mode" })
        }
        ScienceNote("Phone microphones are not calibrated sound-level meters. This unweighted estimate is not dBA and cannot establish safe exposure. Calibrate against a reference meter; use certified equipment for health decisions.")
    }
}

@Composable
private fun DecibelGauge(db: Double) {
    val animated by animateFloatAsState(((db - 30) / 90).toFloat().coerceIn(0f, 1f), tween(350), label = "decibel gauge")
    val color = soundColor(db)
    Box(Modifier.fillMaxWidth().heightIn(min = 225.dp).aspectRatio(1.3f)
        .semantics(mergeDescendants = true) { contentDescription = "Estimated level ${db.oneDecimal()} decibels" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(16.dp)) {
            val diameter = minOf(size.width, size.height) - 24.dp.toPx()
            val origin = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            drawArc(MutedSlate.copy(alpha = .15f), 140f, 260f, false, origin, Size(diameter, diameter), style = Stroke(13.dp.toPx(), cap = StrokeCap.Round))
            drawArc(color, 140f, 260f * animated, false, origin, Size(diameter, diameter), style = Stroke(13.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(db.oneDecimal(), fontSize = 58.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp)
            Text("DECIBELS · ESTIMATED", style = MaterialTheme.typography.labelSmall, color = MutedSlate)
        }
    }
}

@Composable
private fun Waveform(samples: List<Float>) {
    Canvas(Modifier.fillMaxWidth().height(48.dp).semantics { contentDescription = "Live PCM waveform" }) {
        val center = size.height / 2
        drawLine(MutedSlate.copy(alpha = .15f), Offset(0f, center), Offset(size.width, center))
        val path = Path()
        samples.forEachIndexed { i, value ->
            val x = i * size.width / (samples.size - 1).coerceAtLeast(1)
            val y = center - value * center
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, SafeTeal, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun SessionPanel(vm: AcousticViewModel, onHistory: () -> Unit, inputReady: Boolean) {
    Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Capture a moment", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onHistory) { Icon(Icons.Rounded.History, "Open saved sessions", tint = SafeTeal) }
        }
        OutlinedTextField(value = vm.sessionLabel, onValueChange = { vm.sessionLabel = it.take(60) },
            label = { Text("Session name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (vm.sessionExists) {
            Text(if (vm.sessionActive) "● Recording summary" else "Session paused", color = if (vm.sessionActive) SafeTeal else CautionAmber)
            Text(AcousticMath.duration(vm.sessionSeconds), style = MaterialTheme.typography.headlineSmall)
            ValueRow("Energy average / peak", "${vm.sessionLeq.oneDecimal()} / ${vm.sessionPeak.oneDecimal()} dB")
            ValueRow("Estimated session dose", "${vm.sessionDose.oneDecimal()}%")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { if (vm.sessionActive) vm.pauseSession() else vm.startOrResumeSession() },
                enabled = vm.sessionActive || inputReady, modifier = Modifier.weight(1f)) {
                Text(if (vm.sessionActive) "Pause" else if (vm.sessionExists) "Resume" else "Start session")
            }
            if (vm.sessionExists) OutlinedButton(onClick = vm::saveSession, enabled = vm.sessionSeconds > 0, modifier = Modifier.weight(1f)) { Text("Save") }
        }
        Text("Only summaries are saved. Pauses automatically in the background. Manual sessions stay separate from today's microphone dose.", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
    }
}
