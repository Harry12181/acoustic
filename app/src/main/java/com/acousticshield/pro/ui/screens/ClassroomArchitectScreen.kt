package com.acousticshield.pro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acousticshield.pro.model.*
import com.acousticshield.pro.ui.components.*
import com.acousticshield.pro.ui.theme.*
import com.acousticshield.pro.viewmodel.AcousticViewModel
import kotlin.math.roundToInt

@Composable
fun ClassroomArchitectScreen(vm: AcousticViewModel) {
    val wool = AcousticMath.room(vm.classroomNoise.toDouble(), vm.budget.toDouble(), 1.50, 21.7)
    val foam = AcousticMath.room(vm.classroomNoise.toDouble(), vm.budget.toDouble(), 4.50, 5.3)
    ScreenColumn {
        ScreenHeading("A calmer place to learn", "Small budget.\nSofter spaces.", "Compare two treatments for a sensory-friendly room.")
        Panel {
            ValueRow("Current classroom noise", "${vm.classroomNoise.toDouble().oneDecimal()} dB", soundColor(vm.classroomNoise.toDouble()))
            Slider(value = vm.classroomNoise, onValueChange = vm::setClassroom, valueRange = 50f..95f,
                modifier = Modifier.semantics { contentDescription = "Current classroom noise in decibels" })
            ValueRow("Treatment budget", "$${vm.budget.roundToInt()}", SafeTeal)
            Slider(value = vm.budget, onValueChange = vm::updateBudget, valueRange = 20f..500f, steps = 47,
                modifier = Modifier.semantics { contentDescription = "Treatment budget in dollars" })
            Text("Model: 100 sq ft is full treatment coverage", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 360.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TreatmentCard("Thrifted\nHeavy Wool", "$1.50 / sq ft", wool, Modifier.weight(1f))
                    TreatmentCard("Commercial\nSponge Foam", "$4.50 / sq ft", foam, Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TreatmentCard("Thrifted Heavy Wool", "$1.50 / sq ft", wool)
                    TreatmentCard("Commercial Sponge Foam", "$4.50 / sq ft", foam)
                }
            }
        }
        StatusBanner("What your budget can do", "In this model, wool reduces ${ (wool.reductionDb - foam.reductionDb).oneDecimal()} dB more noise for the same budget. ${if (vm.budget >= 150) "Wool reaches full modeled coverage at $150." else "Full modeled wool coverage costs $150."}", SafeTeal)
        Panel {
            Text("Plan the whole room", style = MaterialTheme.typography.titleMedium)
            Text("Add quiet zones, felt chair pads, soft furnishings, and predictable break spaces. Ask learners which sounds feel difficult; comfort is personal.", color = MutedSlate)
        }
        ScienceNote("Illustrative science-fair model. Barrier attenuation is not a room absorption coefficient; real room improvement depends on reverberation, layout, and treatment placement. Follow local fire codes and use appropriately rated materials.")
    }
}

@Composable
private fun TreatmentCard(title: String, price: String, result: RoomPrediction, modifier: Modifier = Modifier) {
    val color: Color = when { result.finalDb < 55 -> SafeTeal; result.finalDb <= 70 -> CautionAmber; else -> DangerCrimson }
    Panel(modifier) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(price, style = MaterialTheme.typography.bodySmall, color = MutedSlate)
        HorizontalDivider(color = MutedSlate.copy(alpha = .2f))
        Text("FINAL ESTIMATE", style = MaterialTheme.typography.labelSmall, color = MutedSlate)
        Text("${result.finalDb.oneDecimal()}", fontSize = 36.sp, color = color)
        Text("decibels", color = MutedSlate, style = MaterialTheme.typography.bodySmall)
        Surface(color = color.copy(alpha = .12f), shape = MaterialTheme.shapes.small) {
            Text(result.comfort, color = color, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(8.dp))
        }
        Text("${result.affordableSqFt.oneDecimal()} sq ft\naffordable area", style = MaterialTheme.typography.bodySmall, color = CrispWhite)
        Text("−${result.reductionDb.oneDecimal()} dB\nmodeled reduction", style = MaterialTheme.typography.bodySmall, color = SafeTeal)
    }
}
