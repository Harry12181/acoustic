package com.acousticshield.pro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.acousticshield.pro.model.*
import com.acousticshield.pro.ui.components.*
import com.acousticshield.pro.ui.theme.*
import com.acousticshield.pro.viewmodel.AcousticViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorySheet(vm: AcousticViewModel, onDismiss: () -> Unit, onExport: () -> Unit, exporting: Boolean) {
    var deleteTarget by remember { mutableStateOf<MeasurementSession?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = DeepSlate) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.90f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Text("Your sound journal", style = MaterialTheme.typography.headlineLarge)
                Text("${vm.history.size} of 100 saved summaries · on this device", color = MutedSlate, modifier = Modifier.padding(top = 6.dp))
            }
            item {
                Button(onClick = onExport, enabled = vm.history.isNotEmpty() && !exporting, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.IosShare, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(if (exporting) "Preparing CSV…" else "Share all as CSV")
                }
            }
            if (vm.history.size > 1) item {
                Panel {
                    Text("Recent energy averages", style = MaterialTheme.typography.titleSmall)
                    Sparkline(vm.history.take(20).reversed().map { it.leqDb })
                    Text("Last ${minOf(20, vm.history.size)} sessions · oldest to newest · relative scale", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
                }
            }
            if (vm.history.isEmpty()) item {
                Panel {
                    Icon(Icons.Rounded.GraphicEq, null, tint = SafeTeal)
                    Text("Every quiet change starts somewhere.", style = MaterialTheme.typography.titleMedium)
                    Text("Start a session on the Live Meter, let it capture a few seconds, then tap Save.", color = MutedSlate)
                }
            }
            items(vm.history, key = { it.id }) { s ->
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.label, style = MaterialTheme.typography.titleMedium)
                            Text(Instant.ofEpochMilli(s.startedAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a")), style = MaterialTheme.typography.bodySmall, color = MutedSlate)
                        }
                        IconButton(onClick = { deleteTarget = s }) { Icon(Icons.Rounded.DeleteOutline, "Delete session ${s.label}", tint = MutedSlate) }
                    }
                    Text(s.source, style = MaterialTheme.typography.labelSmall, color = if (s.source.startsWith("Manual")) CautionAmber else SafeTeal)
                    ValueRow("Energy average / peak", "${s.leqDb.oneDecimal()} / ${s.peakDb.oneDecimal()} dB")
                    ValueRow("Minimum", "${s.minDb.oneDecimal()} dB")
                    ValueRow("Captured time", AcousticMath.duration(s.durationSeconds))
                    ValueRow("Estimated dose", "${s.dosePercent.oneDecimal()}%")
                    Text("Correction: ${s.calibrationOffset.oneDecimal()} dB · unweighted estimate", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
                }
            }
        }
    }
    deleteTarget?.let { target ->
        AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("Delete session?") }, text = { Text("Remove “${target.label}” from this device? Today's dose total will stay unchanged.") },
            confirmButton = { TextButton(onClick = { vm.deleteSession(target.id); deleteTarget = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } })
    }
}
