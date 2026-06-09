package com.cappsconsulting.prism.companion.hal.android

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.cappsconsulting.prism.companion.hal.MicrophoneSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/**
 * [AudioRecord] implementation of [MicrophoneSource] — replatform item 1's mic half.
 * Records raw 16-bit mono PCM at 16 kHz, matching the wire shape
 * [com.cappsconsulting.prism.companion.recognition.RecognitionEngine] expects.
 *
 * [start]/[stop] manage the [AudioRecord] lifecycle. [recordUtterance] starts and
 * stops actual recording around the capture window — the mic isn't continuously open
 * between calls, preserving battery and audio privacy in the session gaps.
 *
 * Silence detection: consecutive 50 ms chunks below [SILENCE_THRESHOLD_RMS] (roughly
 * background-noise level for a child's indoor environment) end the recording after
 * [SILENCE_WINDOW_CHUNKS] * 50 ms of quiet (~400 ms), matching the Python original's
 * `_detect_utterance_end` heuristic. Recording always waits at least half a second
 * before considering silence — so a child's brief pause mid-sentence doesn't truncate.
 */
class AndroidMicrophone : MicrophoneSource {

    @Volatile private var audioRecord: AudioRecord? = null

    override suspend fun start() {
        if (audioRecord != null) return
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            .coerceAtLeast(SAMPLE_RATE * 2) // at least 1 second of buffer
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize,
        )
    }

    override suspend fun stop() {
        audioRecord?.release()
        audioRecord = null
    }

    override suspend fun recordUtterance(maxSeconds: Double): ByteArray = withContext(Dispatchers.IO) {
        val record = audioRecord ?: return@withContext ByteArray(0)
        val maxSamples = (SAMPLE_RATE * maxSeconds).toInt()
        val chunkSize = SAMPLE_RATE / 20 // 50 ms chunks
        val minSamplesBeforeSilence = SAMPLE_RATE / 2 // 500 ms minimum recording
        val capturedChunks = mutableListOf<ShortArray>()
        var totalSamples = 0
        var silenceChunks = 0

        record.startRecording()
        try {
            while (totalSamples < maxSamples) {
                val chunk = ShortArray(chunkSize)
                val read = record.read(chunk, 0, chunkSize)
                if (read <= 0) break
                capturedChunks += chunk.copyOf(read)
                totalSamples += read

                var rmsSquaredSum = 0L
                for (i in 0 until read) { val s = chunk[i].toLong(); rmsSquaredSum += s * s }
                val rms = sqrt(rmsSquaredSum.toDouble() / read)

                if (rms < SILENCE_THRESHOLD_RMS && totalSamples > minSamplesBeforeSilence) {
                    silenceChunks++
                    if (silenceChunks >= SILENCE_WINDOW_CHUNKS) break
                } else {
                    silenceChunks = 0
                }
            }
        } finally {
            record.stop()
        }

        // Pack short samples → little-endian PCM byte array
        val pcm = ByteArray(totalSamples * 2)
        var offset = 0
        for (chunk in capturedChunks) {
            for (sample in chunk) {
                pcm[offset++] = (sample.toInt() and 0xFF).toByte()
                pcm[offset++] = ((sample.toInt() shr 8) and 0xFF).toByte()
            }
        }
        pcm
    }

    companion object {
        private const val SAMPLE_RATE = 16_000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val SILENCE_THRESHOLD_RMS = 400.0 // empirical: typical indoor background
        private const val SILENCE_WINDOW_CHUNKS = 8 // 8 × 50 ms = 400 ms of consecutive quiet
    }
}
