package com.acousticshield.pro.model

import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

enum class BarrierMaterial(val label: String, val attenuationDb: Double) {
    NONE("None (Uncovered)", 0.0),
    HEAVY_WOOL("Heavy Wool (Winter Coat)", 21.7),
    COTTON("Cotton (Jeans/Blanket)", 19.5),
    SILK("Silk (Scarf)", 13.6),
    POLYESTER("Polyester (Backpack)", 8.3),
    SPONGE_FOAM("Sponge Foam (Cushion)", 5.3)
}

data class AlarmPrediction(val predictedDb: Double, val barrierLoss: Double, val distanceLoss: Double)
data class RoomPrediction(val affordableSqFt: Double, val reductionDb: Double, val finalDb: Double) {
    val comfort: String get() = when {
        finalDb < 55 -> "Calm Focus"
        finalDb <= 70 -> "Moderate Ambient"
        else -> "Overstimulating"
    }
}
data class AlarmPreset(
    val id: String,
    val name: String,
    val layer1: BarrierMaterial,
    val layer2: BarrierMaterial,
    val distance: Float
)
data class MeasurementSession(
    val id: String,
    val label: String,
    val startedAt: Long,
    val durationSeconds: Double,
    val leqDb: Double,
    val minDb: Double,
    val peakDb: Double,
    val dosePercent: Double,
    val source: String,
    val calibrationOffset: Double
)

object AcousticMath {
    fun alarm(baseline: Double, first: BarrierMaterial, second: BarrierMaterial, distance: Double): AlarmPrediction {
        require(baseline.isFinite() && distance.isFinite() && distance >= 1.0)
        val barrier = first.attenuationDb + second.attenuationDb * 0.70
        val distanceLoss = 20.0 * log10(distance)
        return AlarmPrediction(maxOf(20.0, baseline - barrier - distanceLoss), barrier, distanceLoss)
    }

    fun room(noise: Double, budget: Double, costPerSqFt: Double, maxAbsorption: Double): RoomPrediction {
        require(listOf(noise, budget, costPerSqFt, maxAbsorption).all { it.isFinite() })
        require(budget >= 0 && costPerSqFt > 0 && maxAbsorption >= 0)
        val area = budget / costPerSqFt
        val reduction = maxAbsorption * minOf(1.0, area / 100.0)
        return RoomPrediction(area, reduction, maxOf(30.0, noise - reduction))
    }

    fun netExposure(environment: Double, barrier: BarrierMaterial): Double =
        maxOf(30.0, environment - barrier.attenuationDb)

    fun allowedHours(db: Double): Double {
        require(db.isFinite())
        return 8.0 / 2.0.pow((db - 85.0) / 3.0)
    }

    // An 80 dB integration threshold is used only for the optional cumulative-dose model.
    fun dosePercent(db: Double, seconds: Double): Double {
        require(db.isFinite() && seconds.isFinite() && seconds >= 0)
        return if (db < 80.0) 0.0 else seconds / (allowedHours(db) * 3600.0) * 100.0
    }

    fun duration(seconds: Double): String {
        val total = seconds.coerceIn(0.0, 315360000.0).roundToLong()
        return "${total / 3600}h ${(total % 3600) / 60}m ${total % 60}s"
    }

    fun leq(energySeconds: Double, seconds: Double): Double =
        if (seconds <= 0.0 || energySeconds <= 0.0) 0.0 else 10.0 * log10(energySeconds / seconds)
}

fun Double.oneDecimal(): String = String.format(Locale.US, "%.1f", this)
