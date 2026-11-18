package com.acousticshield.pro.data

import android.content.Context
import com.acousticshield.pro.model.AlarmPreset
import com.acousticshield.pro.model.BarrierMaterial
import com.acousticshield.pro.model.MeasurementSession
import org.json.JSONArray
import org.json.JSONObject

class AcousticStore(context: Context) {
    val preferences = context.getSharedPreferences("acoustic_shield", Context.MODE_PRIVATE)

    fun number(key: String, fallback: Double, range: ClosedFloatingPointRange<Double>): Double =
        preferences.getFloat(key, fallback.toFloat()).toDouble().let { if (it.isFinite()) it.coerceIn(range) else fallback }

    fun putNumber(key: String, value: Double) { preferences.edit().putFloat(key, value.toFloat()).apply() }
    fun boolean(key: String, default: Boolean = false) = preferences.getBoolean(key, default)
    fun putBoolean(key: String, value: Boolean) { preferences.edit().putBoolean(key, value).apply() }
    fun text(key: String, fallback: String) = preferences.getString(key, fallback) ?: fallback
    fun putText(key: String, value: String) { preferences.edit().putString(key, value).apply() }
    private fun array(key: String) = runCatching { JSONArray(text(key, "[]")) }.getOrElse { JSONArray() }

    fun sessions(): List<MeasurementSession> {
        val array = array("sessions")
        return (0 until array.length()).mapNotNull { i ->
            runCatching {
                val o = array.getJSONObject(i)
                MeasurementSession(o.getString("id"), o.getString("label"), o.getLong("start"),
                    o.getDouble("duration"), o.getDouble("leq"), o.getDouble("min"), o.getDouble("peak"),
                    o.getDouble("dose"), o.getString("source"), o.getDouble("offset"))
            }.getOrNull()
        }.take(100)
    }

    fun saveSessions(items: List<MeasurementSession>) {
        val result = JSONArray()
        items.take(100).forEach { s -> result.put(JSONObject().apply {
            put("id", s.id); put("label", s.label); put("start", s.startedAt); put("duration", s.durationSeconds)
            put("leq", s.leqDb); put("min", s.minDb); put("peak", s.peakDb); put("dose", s.dosePercent)
            put("source", s.source); put("offset", s.calibrationOffset)
        }) }
        putText("sessions", result.toString())
    }

    fun presets(): List<AlarmPreset> {
        val array = array("presets")
        return (0 until array.length()).mapNotNull { i -> runCatching {
            val o = array.getJSONObject(i)
            AlarmPreset(o.getString("id"), o.getString("name"), BarrierMaterial.valueOf(o.getString("first")),
                BarrierMaterial.valueOf(o.getString("second")), o.getDouble("distance").toFloat().coerceIn(1f, 5f))
        }.getOrNull() }.take(20)
    }

    fun savePresets(items: List<AlarmPreset>) {
        val result = JSONArray()
        items.take(20).forEach { p -> result.put(JSONObject().apply {
            put("id", p.id); put("name", p.name); put("first", p.layer1.name)
            put("second", p.layer2.name); put("distance", p.distance.toDouble())
        }) }
        putText("presets", result.toString())
    }

    fun doses(): Map<String, Double> = runCatching {
        val o = JSONObject(text("daily_dose", "{}"))
        o.keys().asSequence().associateWith { o.getDouble(it) }.filterValues { it.isFinite() && it >= 0 }
    }.getOrDefault(emptyMap())

    fun saveDoses(doses: Map<String, Double>) {
        val o = JSONObject()
        doses.entries.sortedByDescending { it.key }.take(30).forEach { o.put(it.key, it.value) }
        putText("daily_dose", o.toString())
    }
}
