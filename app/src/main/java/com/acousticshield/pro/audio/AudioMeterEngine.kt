package com.acousticshield.pro.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.log10
import kotlin.math.sqrt

data class AudioFrame(
    val db: Double = 30.0,
    val waveform: List<Float> = List(60) { 0f },
    val running: Boolean = false,
    val error: String? = null,
    val capturedAt: Long = 0L
)

class AudioMeterEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val recorderLock = Mutex()
    private val mutableFrames = MutableStateFlow(AudioFrame())
    val frames = mutableFrames.asStateFlow()
    @Volatile var calibrationOffsetDb = 0.0
    private var job: Job? = null

    @SuppressLint("MissingPermission")
    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            recorderLock.withLock {
                var recorder: AudioRecord? = null
                try {
                    val minimum = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                    check(minimum > 0) { "This microphone does not support 44.1 kHz mono recording. Try manual mode." }
                    recorder = AudioRecord(
                        MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum * 2, 8192)
                    )
                    check(recorder.state == AudioRecord.STATE_INITIALIZED) { "Microphone initialization failed. Try manual mode." }
                    recorder.startRecording()
                    check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Microphone could not start. Another app may be using it." }
                    val buffer = ShortArray(2048)
                    var lastData = SystemClock.elapsedRealtime()
                    while (isActive) {
                        val count = recorder.read(buffer, 0, buffer.size, AudioRecord.READ_NON_BLOCKING)
                        check(count >= 0) { "Microphone read failed ($count). Stop and retry." }
                        if (count > 0) {
                            lastData = SystemClock.elapsedRealtime()
                            var squares = 0.0
                            for (i in 0 until count) squares += buffer[i].toDouble() * buffer[i].toDouble()
                            val rms = sqrt(squares / count)
                            val rawDb = if (rms <= 0.0) 30.0 else (20.0 * log10(rms / 32767.0) + 100.0).coerceIn(30.0, 120.0)
                            val wave = List(60) { index -> buffer[(index * count / 60).coerceAtMost(count - 1)] / 32768f }
                            mutableFrames.value = AudioFrame(rawDb + calibrationOffsetDb.coerceIn(-20.0, 20.0), wave, true, capturedAt = lastData)
                        } else {
                            check(SystemClock.elapsedRealtime() - lastData < 5000L) { "No microphone data received. Check system microphone access or use manual mode." }
                        }
                        delay(45)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    mutableFrames.value = mutableFrames.value.copy(running = false, error = failure.message ?: "Microphone unavailable.")
                } finally {
                    recorder?.let { audio ->
                        runCatching { if (audio.recordingState == AudioRecord.RECORDSTATE_RECORDING) audio.stop() }
                        audio.release()
                    }
                    mutableFrames.value = mutableFrames.value.copy(running = false)
                }
            }
        }
    }

    fun stop() { job?.cancel(); job = null }
    fun close() { stop(); scope.cancel() }
}
