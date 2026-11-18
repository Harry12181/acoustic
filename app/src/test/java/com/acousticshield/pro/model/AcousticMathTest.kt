package com.acousticshield.pro.model

import org.junit.Assert.*
import org.junit.Test

class AcousticMathTest {
    @Test fun empiricalValuesRemainExact() {
        assertEquals(listOf(0.0, 21.7, 19.5, 13.6, 8.3, 5.3), BarrierMaterial.entries.map { it.attenuationDb })
    }
    @Test fun alarmAppliesDiminishingReturnsAndDistance() {
        val result = AcousticMath.alarm(95.0, BarrierMaterial.HEAVY_WOOL, BarrierMaterial.COTTON, 2.0)
        assertEquals(53.6294000867, result.predictedDb, 0.000001)
        assertEquals(35.35, result.barrierLoss, 0.000001)
    }
    @Test fun alarmHasTwentyDbFloor() {
        assertEquals(20.0, AcousticMath.alarm(80.0, BarrierMaterial.HEAVY_WOOL, BarrierMaterial.HEAVY_WOOL, 100.0).predictedDb, 0.0)
    }
    @Test fun uncoveredAlarmAtOneMeterEqualsBaseline() {
        assertEquals(95.0, AcousticMath.alarm(95.0, BarrierMaterial.NONE, BarrierMaterial.NONE, 1.0).predictedDb, 0.0)
    }
    @Test fun roomCoverageSaturatesAndUsesThirtyDbFloor() {
        val wool = AcousticMath.room(50.0, 500.0, 1.5, 21.7)
        assertEquals(21.7, wool.reductionDb, 0.00001)
        assertEquals(30.0, wool.finalDb, 0.00001)
        val foam = AcousticMath.room(70.0, 45.0, 4.5, 5.3)
        assertEquals(0.53, foam.reductionDb, 0.00001)
    }
    @Test fun comfortBoundariesAreInclusiveAsSpecified() {
        assertEquals("Calm Focus", RoomPrediction(0.0, 0.0, 54.9).comfort)
        assertEquals("Moderate Ambient", RoomPrediction(0.0, 0.0, 55.0).comfort)
        assertEquals("Moderate Ambient", RoomPrediction(0.0, 0.0, 70.0).comfort)
        assertEquals("Overstimulating", RoomPrediction(0.0, 0.0, 70.1).comfort)
    }
    @Test fun nioshThreeDbExchangeRateMatchesKnownExamples() {
        assertEquals(8.0, AcousticMath.allowedHours(85.0), 0.00001)
        assertEquals(4.0, AcousticMath.allowedHours(88.0), 0.00001)
        assertEquals(0.25, AcousticMath.allowedHours(100.0), 0.00001)
        assertEquals("0h 15m 0s", AcousticMath.duration(AcousticMath.allowedHours(100.0) * 3600))
    }
    @Test fun doseAccumulatesAcrossDifferentLevels() {
        val dose = AcousticMath.dosePercent(85.0, 14400.0) + AcousticMath.dosePercent(88.0, 7200.0)
        assertEquals(100.0, dose, 0.00001)
        assertEquals(0.0, AcousticMath.dosePercent(79.9, 28800.0), 0.0)
    }
    @Test fun leqUsesEnergyAverageInsteadOfArithmeticMean() {
        val energy = Math.pow(10.0, 8.0) + Math.pow(10.0, 9.0)
        assertEquals(87.40362689, AcousticMath.leq(energy, 2.0), 0.000001)
    }
    @Test fun invalidDistanceIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { AcousticMath.alarm(95.0, BarrierMaterial.NONE, BarrierMaterial.NONE, 0.0) }
    }
}
