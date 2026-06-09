package com.cappsconsulting.prism.companion.hal.android

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.cappsconsulting.prism.companion.hal.SpeakerOutput
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

/**
 * [TextToSpeech] + [ToneGenerator] implementation of [SpeakerOutput] — replatform
 * item 1's speaker half.
 *
 * [speak] suspends until the utterance finishes — the `await tts.speak(text)` shape
 * the original `SpeakerHAL.speak` had — so callers never advance the pipeline before
 * the companion's voice is done. Rate and volume modulation come straight from
 * [com.cappsconsulting.prism.companion.orchestrator.CompanionOrchestrator.speakWithFeltVoice]:
 * `rate` is wpm (mapped to TTS's relative float: 150 wpm → 1.0), `volume` is 0.0–1.0.
 *
 * TTS initialization is asynchronous; [ttsReady] defers any [speak] call until the
 * engine confirms it's ready — the same "wait for the device to be genuinely ready"
 * instinct [com.cappsconsulting.prism.companion.recognition.RecognitionEngine]'s
 * `isEnrolled()` gate provides for the recognition path.
 *
 * [playTone] and [playSound] use [ToneGenerator] for simple feedback tones. Arbitrary
 * frequency synthesis (for Doc 2.3 §5's eventual ambient sound design) would need
 * `AudioTrack` + sine-wave generation — named here, not yet built.
 */
class AndroidSpeaker(context: Context) : SpeakerOutput {

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: Flow<Boolean> = _isSpeaking.asStateFlow()

    private val ttsReady = CompletableDeferred<Unit>()
    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            ttsReady.complete(Unit)
        } else {
            ttsReady.completeExceptionally(RuntimeException("TTS init failed (status=$status)"))
        }
    }

    init {
        tts.language = Locale.US
    }

    override suspend fun speak(text: String, rate: Int, volume: Float) {
        ttsReady.await()
        tts.setSpeechRate(rate / 150f)
        suspendCancellableCoroutine { continuation ->
            val utteranceId = "speak_${System.nanoTime()}"
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String) {
                    if (id == utteranceId) _isSpeaking.value = true
                }
                override fun onDone(id: String) {
                    if (id == utteranceId) { _isSpeaking.value = false; continuation.resume(Unit) }
                }
                @Suppress("DEPRECATION")
                override fun onError(id: String) {
                    if (id == utteranceId) { _isSpeaking.value = false; continuation.resume(Unit) }
                }
                override fun onError(id: String, errorCode: Int) {
                    if (id == utteranceId) { _isSpeaking.value = false; continuation.resume(Unit) }
                }
            })
            val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume) }
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            continuation.invokeOnCancellation { tts.stop() }
        }
    }

    override suspend fun playTone(frequencyHz: Float, durationSeconds: Double, volume: Float) {
        val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, (volume * 100).toInt().coerceIn(0, 100))
        toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, (durationSeconds * 1000).toInt())
        delay((durationSeconds * 1000 + 50).toLong())
        toneGen.release()
    }

    override suspend fun playSound(soundId: String) {
        // Named sounds (e.g. "chime", "whoosh") — SoundPool-backed implementation is
        // follow-on work for Doc 2.3 §5's ambient sound design. Fallback to a tone for now.
        playTone(440f, 0.25, 0.6f)
    }

    fun release() {
        tts.stop()
        tts.shutdown()
    }
}
