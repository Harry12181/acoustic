package com.acousticshield.pro.data

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.acousticshield.pro.model.MeasurementSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.util.Locale

object SessionExporter {
    private fun csvCell(value: String): String {
        val isNumber = value.toDoubleOrNull()?.isFinite() == true
        val safe = if (!isNumber && value.trimStart().firstOrNull() in listOf('=', '+', '-', '@', '\t', '\r', '\n')) "'$value" else value
        return "\"${safe.replace("\"", "\"\"")}\""
    }

    suspend fun createShareIntent(context: Context, sessions: List<MeasurementSession>): Intent = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "reports").apply { mkdirs() }
        // Expire old reports; only user-selected summaries are written to the share cache.
        directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 86400000L }?.forEach { it.delete() }
        val report = File(directory, "AcousticShield-${System.currentTimeMillis()}.csv")
        report.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.appendLine("session_id,label,started_at_utc,duration_seconds,leq_estimate_db,min_estimate_db,peak_estimate_db,model_dose_percent,source,calibration_offset_db,measurement_scope")
            sessions.forEach { s ->
                val numeric = { n: Double -> String.format(Locale.US, "%.3f", n) }
                writer.appendLine(listOf(s.id, s.label, Instant.ofEpochMilli(s.startedAt).toString(), numeric(s.durationSeconds),
                    numeric(s.leqDb), numeric(s.minDb), numeric(s.peakDb), numeric(s.dosePercent), s.source,
                    numeric(s.calibrationOffset), "Educational unweighted estimate; not dBA; no certified protection adjustment")
                    .joinToString(",") { csvCell(it) })
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", report)
        Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "AcousticShield Pro session summaries")
            clipData = ClipData.newRawUri("AcousticShield CSV", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
