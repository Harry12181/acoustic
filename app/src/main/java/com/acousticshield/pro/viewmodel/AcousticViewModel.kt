package com.acousticshield.pro.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.acousticshield.pro.audio.AudioMeterEngine
import com.acousticshield.pro.data.AcousticStore
import com.acousticshield.pro.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import kotlin.math.pow

class AcousticViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AcousticStore(application)
    private val engine = AudioMeterEngine()
    val frames = engine.frames
    var selectedTab by mutableIntStateOf(0)
    var baselineSpeakerDb by mutableDoubleStateOf(store.number("baseline", 95.0, 80.0..105.0)); private set
    var calibrationOffsetDb by mutableDoubleStateOf(store.number("offset", 0.0, -20.0..20.0)); private set
    var environmentalNoiseDb by mutableDoubleStateOf(store.number("environment", 85.0, 10.0..140.0)); private set
    var simulatorInputDb by mutableStateOf<Double?>(null); private set
    var layer1 by mutableStateOf(material("layer1")); private set
    var layer2 by mutableStateOf(material("layer2")); private set
    var distanceMeters by mutableFloatStateOf(store.number("distance", 1.0, 1.0..5.0).toFloat()); private set
    var protection by mutableStateOf(material("protection")); private set
    var classroomNoise by mutableFloatStateOf(store.number("classroom", 75.0, 50.0..95.0).toFloat()); private set
    var budget by mutableFloatStateOf(store.number("budget", 150.0, 20.0..500.0).toFloat()); private set
    var manualMode by mutableStateOf(store.boolean("manual")); private set
    var manualDb by mutableFloatStateOf(store.number("manual_db", 65.0, 30.0..120.0).toFloat()); private set
    var liveRequested by mutableStateOf(false); private set
    var permissionGranted by mutableStateOf(false); private set
    var foreground by mutableStateOf(false); private set
    var peakDb by mutableDoubleStateOf(30.0); private set
    var currentDb by mutableDoubleStateOf(if (manualMode) manualDb.toDouble() else 30.0); private set
    var history by mutableStateOf(store.sessions()); private set
    var presets by mutableStateOf(store.presets()); private set
    var sessionActive by mutableStateOf(false); private set
    var sessionExists by mutableStateOf(false); private set
    var sessionSeconds by mutableDoubleStateOf(0.0); private set
    var sessionDose by mutableDoubleStateOf(0.0); private set
    var sessionLeq by mutableDoubleStateOf(0.0); private set
    var sessionPeak by mutableDoubleStateOf(0.0); private set
    var sessionLabel by mutableStateOf("Noise check")
    private var sessionStart = 0L
    private var sessionSource = ""
    private var sessionOffset = 0.0
    private var sessionEnergy = 0.0
    private var sessionMin = Double.POSITIVE_INFINITY
    private var lastTick = SystemClock.elapsedRealtime()
    private var doseLedger = store.doses().toMutableMap()
    private var today = LocalDate.now().toString()
    var dailyDose by mutableDoubleStateOf(doseLedger[today] ?: 0.0); private set
    private var ticksSinceSave = 0

    init {
        engine.calibrationOffsetDb = calibrationOffsetDb
        viewModelScope.launch {
            frames.collect { frame ->
                if (!manualMode && frame.running) {
                    currentDb = frame.db
                    peakDb = maxOf(peakDb, currentDb)
                }
                if (frame.error != null && !frame.running) {
                    liveRequested = false
                    pauseSession()
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val now = SystemClock.elapsedRealtime()
                val elapsed = ((now - lastTick) / 1000.0).coerceIn(0.0, 2.0)
                lastTick = now
                refreshDay()
                if (sessionActive && foreground) {
                    val valid = manualMode || (frames.value.running && now - frames.value.capturedAt < 2000)
                    if (valid) {
                        sessionSeconds += elapsed
                        sessionEnergy += 10.0.pow(currentDb / 10.0) * elapsed
                        sessionLeq = AcousticMath.leq(sessionEnergy, sessionSeconds)
                        sessionMin = minOf(sessionMin, currentDb)
                        sessionPeak = maxOf(sessionPeak, currentDb)
                        val dose = AcousticMath.dosePercent(currentDb, elapsed)
                        sessionDose += dose
                        // Manual demonstrations never contribute to the microphone ledger.
                        if (!manualMode) {
                            dailyDose += dose
                            doseLedger[today] = dailyDose
                            if (++ticksSinceSave >= 10) persistDose()
                        }
                    }
                }
            }
        }
    }

    private fun material(key: String) = runCatching {
        BarrierMaterial.valueOf(store.text(key, BarrierMaterial.NONE.name))
    }.getOrDefault(BarrierMaterial.NONE)

    private fun refreshDay() {
        val date = LocalDate.now().toString()
        if (date != today) {
            persistDose()
            today = date
            dailyDose = doseLedger[date] ?: 0.0
        }
    }

    private fun persistDose() { store.saveDoses(doseLedger); ticksSinceSave = 0 }
    fun clearDailyDose() { refreshDay(); dailyDose = 0.0; doseLedger[today] = 0.0; persistDose() }

    fun updateForeground(value: Boolean) {
        foreground = value
        if (!value) { pauseSession(); liveRequested = false }
        refreshDay()
        syncEngine()
    }

    fun setPermission(granted: Boolean) {
        permissionGranted = granted
        if (!granted) { liveRequested = false; pauseSession() }
        syncEngine()
    }

    private fun syncEngine() {
        if (foreground && permissionGranted && liveRequested && !manualMode) engine.start() else engine.stop()
    }

    fun toggleLive() {
        liveRequested = !liveRequested
        if (!liveRequested) pauseSession()
        syncEngine()
    }

    fun setManual(enabled: Boolean) {
        if (sessionExists) saveSession()
        manualMode = enabled
        store.putBoolean("manual", enabled)
        liveRequested = false
        currentDb = if (enabled) manualDb.toDouble() else 30.0
        peakDb = currentDb
        syncEngine()
    }

    fun updateManualDb(value: Float) {
        manualDb = value.coerceIn(30f, 120f)
        store.putNumber("manual_db", manualDb.toDouble())
        if (manualMode) { currentDb = manualDb.toDouble(); peakDb = maxOf(peakDb, currentDb) }
    }

    fun setBaseline(value: Double) { baselineSpeakerDb = value.coerceIn(80.0, 105.0); store.putNumber("baseline", baselineSpeakerDb) }
    fun setOffset(value: Double) {
        if (sessionExists) saveSession()
        calibrationOffsetDb = value.coerceIn(-20.0, 20.0)
        engine.calibrationOffsetDb = calibrationOffsetDb
        store.putNumber("offset", calibrationOffsetDb)
    }
    fun setEnvironment(value: Double) { environmentalNoiseDb = value.coerceIn(10.0, 140.0); store.putNumber("environment", environmentalNoiseDb) }
    fun sendMeasurement() { setEnvironment(currentDb); simulatorInputDb = currentDb }
    fun useSpeakerBaseline() { simulatorInputDb = null }
    fun setFirst(value: BarrierMaterial) { layer1 = value; store.putText("layer1", value.name) }
    fun setSecond(value: BarrierMaterial) { layer2 = value; store.putText("layer2", value.name) }
    fun setDistance(value: Float) { distanceMeters = value.coerceIn(1f, 5f); store.putNumber("distance", distanceMeters.toDouble()) }
    fun updateProtection(value: BarrierMaterial) { protection = value; store.putText("protection", value.name) }
    fun setClassroom(value: Float) { classroomNoise = value.coerceIn(50f, 95f); store.putNumber("classroom", classroomNoise.toDouble()) }
    fun updateBudget(value: Float) { budget = value.coerceIn(20f, 500f); store.putNumber("budget", budget.toDouble()) }
    fun resetPeak() { peakDb = currentDb }

    fun startOrResumeSession() {
        if (!foreground || (!manualMode && !frames.value.running)) return
        if (!sessionExists) {
            sessionExists = true
            sessionStart = System.currentTimeMillis()
            sessionSource = if (manualMode) "Manual simulation" else "Microphone estimate"
            sessionOffset = calibrationOffsetDb
            sessionSeconds = 0.0; sessionEnergy = 0.0; sessionMin = Double.POSITIVE_INFINITY
            sessionPeak = 0.0; sessionDose = 0.0; sessionLeq = 0.0
        }
        lastTick = SystemClock.elapsedRealtime()
        sessionActive = true
    }

    fun pauseSession() { sessionActive = false; persistDose() }

    fun saveSession() {
        pauseSession()
        if (sessionExists && sessionSeconds > 0) {
            val session = MeasurementSession(UUID.randomUUID().toString(), sessionLabel.trim().take(60).ifBlank { "Noise check" },
                sessionStart, sessionSeconds, sessionLeq, sessionMin, sessionPeak, sessionDose, sessionSource, sessionOffset)
            history = (listOf(session) + history).take(100)
            store.saveSessions(history)
        }
        sessionExists = false
    }

    fun deleteSession(id: String) { history = history.filterNot { it.id == id }; store.saveSessions(history) }
    fun savePreset(name: String) {
        val p = AlarmPreset(UUID.randomUUID().toString(), name.trim().take(40).ifBlank { "Alarm setup ${presets.size + 1}" }, layer1, layer2, distanceMeters)
        presets = (listOf(p) + presets).take(20)
        store.savePresets(presets)
    }
    fun loadPreset(p: AlarmPreset) { setFirst(p.layer1); setSecond(p.layer2); setDistance(p.distance) }
    fun deletePreset(id: String) { presets = presets.filterNot { it.id == id }; store.savePresets(presets) }

    override fun onCleared() { engine.close(); persistDose(); super.onCleared() }
}
