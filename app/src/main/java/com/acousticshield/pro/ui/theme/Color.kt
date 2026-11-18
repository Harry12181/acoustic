package com.acousticshield.pro.ui.theme

import androidx.compose.ui.graphics.Color

val DeepSlate = Color(0xFF0F172A)
val ElevatedSlate = Color(0xFF1E293B)
val CrispWhite = Color(0xFFF8FAFC)
val MutedSlate = Color(0xFF94A3B8)
val SafeTeal = Color(0xFF14B8A6)
val CautionAmber = Color(0xFFF59E0B)
val DangerCrimson = Color(0xFFEF4444)
fun soundColor(db: Double): Color = when {
    db < 70.0 -> SafeTeal
    db < 85.0 -> CautionAmber
    else -> DangerCrimson
}
