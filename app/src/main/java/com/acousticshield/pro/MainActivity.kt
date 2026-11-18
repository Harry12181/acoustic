package com.acousticshield.pro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.acousticshield.pro.data.SessionExporter
import com.acousticshield.pro.model.oneDecimal
import com.acousticshield.pro.ui.screens.*
import com.acousticshield.pro.ui.theme.*
import com.acousticshield.pro.viewmodel.AcousticViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var vm: AcousticViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = ViewModelProvider(this)[AcousticViewModel::class.java]
        setContent { AcousticShieldTheme { AcousticApp(vm) } }
    }

    override fun onStart() {
        super.onStart()
        vm.setPermission(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
        vm.updateForeground(true)
    }

    override fun onStop() {
        vm.updateForeground(false)
        super.onStop()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AcousticApp(vm: AcousticViewModel) {
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        var historyOpen by rememberSaveable { mutableStateOf(false) }
        var exporting by remember { mutableStateOf(false) }
        val labels = listOf("Live Meter", "Alarm Sim", "Architect", "NIOSH", "Calibrate")
        val icons = listOf(Icons.Rounded.GraphicEq, Icons.Rounded.Alarm, Icons.Rounded.Architecture, Icons.Rounded.HealthAndSafety, Icons.Rounded.Tune)
        Scaffold(
            containerColor = DeepSlate,
            topBar = {
                TopAppBar(
                    title = { Text("AcousticShield Pro", fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSlate),
                    actions = {
                        Surface(color = SafeTeal.copy(alpha = .12f), shape = MaterialTheme.shapes.small) {
                            Text("${vm.baselineSpeakerDb.oneDecimal()} dB", color = SafeTeal, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp))
                        }
                        IconButton(onClick = { historyOpen = true }) { Icon(Icons.Rounded.History, "Saved sessions", tint = MutedSlate) }
                    }
                )
            },
            bottomBar = {
                NavigationBar(containerColor = DeepSlate, tonalElevation = 0.dp) {
                    labels.forEachIndexed { i, label ->
                        NavigationBarItem(selected = vm.selectedTab == i, onClick = { vm.selectedTab = i },
                            icon = { Icon(icons[i], null, modifier = Modifier.size(23.dp)) },
                            label = { Text(label, fontSize = 10.sp, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = SafeTeal, selectedTextColor = SafeTeal,
                                indicatorColor = SafeTeal.copy(alpha = .12f), unselectedIconColor = MutedSlate, unselectedTextColor = MutedSlate))
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                when (vm.selectedTab) {
                    0 -> MeterScreen(vm, onSend = {
                        vm.sendMeasurement()
                        scope.launch { snackbar.showSnackbar("${vm.currentDb.oneDecimal()} dB sent to Simulator & NIOSH") }
                    }, onHistory = { historyOpen = true })
                    1 -> AlarmSimulatorScreen(vm)
                    2 -> ClassroomArchitectScreen(vm)
                    3 -> NioshTrackerScreen(vm)
                    else -> CalibrationScreen(vm)
                }
            }
        }
        if (historyOpen) HistorySheet(vm, onDismiss = { historyOpen = false }, exporting = exporting, onExport = {
            exporting = true
            scope.launch {
                try {
                    val send = SessionExporter.createShareIntent(this@MainActivity, vm.history.toList())
                    startActivity(Intent.createChooser(send, "Share acoustic session summaries"))
                } catch (_: Exception) {
                    historyOpen = false
                    snackbar.showSnackbar("Could not share CSV. Check that a sharing app is installed and retry.")
                } finally { exporting = false }
            }
        })
    }
}
