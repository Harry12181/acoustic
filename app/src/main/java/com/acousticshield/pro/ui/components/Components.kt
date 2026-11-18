package com.acousticshield.pro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.acousticshield.pro.model.BarrierMaterial
import com.acousticshield.pro.ui.theme.*

@Composable
fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp), content = content)
}

@Composable
fun ScreenHeading(eyebrow: String, title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelMedium, color = SafeTeal)
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MutedSlate)
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = ElevatedSlate)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun StatusBanner(title: String, body: String, color: Color) {
    Surface(color = color.copy(alpha = .12f), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = color, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = CrispWhite)
        }
    }
}

@Composable
fun ScienceNote(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Rounded.Info, contentDescription = null, tint = MutedSlate, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MutedSlate)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialSelector(label: String, value: BarrierMaterial, onSelected: (BarrierMaterial) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(value = value.label, onValueChange = {}, readOnly = true,
            label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium)
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            BarrierMaterial.entries.forEach { material ->
                DropdownMenuItem(text = { Text("${material.label} · −${material.attenuationDb} dB") },
                    onClick = { onSelected(material); expanded = false })
            }
        }
    }
}

@Composable
fun ValueRow(label: String, value: String, color: Color = CrispWhite) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MutedSlate, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = color, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun Sparkline(values: List<Double>, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(56.dp)) {
        if (values.size < 2) return@Canvas
        val low = (values.minOrNull() ?: 30.0) - 3.0
        val high = (values.maxOrNull() ?: 120.0) + 3.0
        val path = Path()
        values.forEachIndexed { i, value ->
            val x = size.width * i / (values.size - 1)
            val y = size.height * (1 - ((value - low) / (high - low)).toFloat())
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, SafeTeal, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
    }
}
