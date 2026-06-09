package com.cappsconsulting.prism.companion.hal

import kotlinx.coroutines.flow.Flow

/**
 * The native-Android HAL — redesign of `prism/hal/base.py` for the platform pivot
 * (Epigenome 024, Doc 3.0 §2/§4). This is queued replatform item 1: "rebuild hal/
 * natively for Android."
 *
 * What ports as a *renamed, re-targeted interface* (the underlying hardware concept
 * still exists, just produced by different parts):
 *   - `CameraHAL`  -> [CameraSource]      (CameraX, see `hal/android/CameraXSource`)
 *   - `MicHAL`     -> [MicrophoneSource]  (`AudioRecord`)
 *   - `SpeakerHAL` -> [SpeakerOutput]     (`TextToSpeech` + `ToneGenerator`/`SoundPool`)
 *   - `HapticHAL`  -> [HapticOutput]      (`VibrationEffect` composable waveforms)
 *
 * What's **gone, on purpose, not just renamed:**
 *   - `LedHAL` / `LedState` — there is no LED ring. Doc 2.3 §2 is explicit that
 *     simulating one on a rectangular screen "would be a worse ring." It's replaced
 *     by [com.cappsconsulting.prism.companion.presentation.PresentationState] — a
 *     redesigned concept (full-canvas color/motion/form, not discrete hue+pulse on
 *     a ring) that the Compose layer renders, not a HAL device.
 *   - `GpioHAL` — there is no physical shutter button or GPIO bus on a phone. The
 *     "press the shutter" gesture becomes a tap on the live camera presentation,
 *     handled directly by the Compose UI layer and routed to
 *     [com.cappsconsulting.prism.companion.orchestrator.CompanionOrchestrator]
 *     exactly where `register_button_callback("shutter", …)` used to route it —
 *     same seam, phone-native trigger.
 *
 * Every method here is `suspend`/`Flow`-based (Kotlin coroutines) where the Python
 * original was `async def` (asyncio) — the direct, idiomatic translation; nothing
 * about the *contracts* changes, only the concurrency vocabulary they're spoken in.
 */

/** 640x480 RGB; matches the contract `vision_engine.py` / `recognition.py` already expect — no inference-side changes needed. */
data class CameraFrame(val widthPx: Int, val heightPx: Int, val rgb: ByteArray) {
    init {
        require(rgb.size == widthPx * heightPx * 3) { "expected ${widthPx * heightPx * 3} RGB bytes, got ${rgb.size}" }
    }
}

interface CameraSource {
    suspend fun start()
    suspend fun stop()

    /** Suspends until the next frame is available — the `await capture_frame()` translation. */
    suspend fun captureFrame(): CameraFrame
}

interface MicrophoneSource {
    suspend fun start()
    suspend fun stop()

    /** Raw 16-bit PCM, mono — same wire shape `recognition.py`'s optional voice path expects. */
    suspend fun recordUtterance(maxSeconds: Double = 10.0): ByteArray
}

interface SpeakerOutput {
    suspend fun speak(text: String, rate: Int = 150, volume: Float = 1.0f)
    suspend fun playTone(frequencyHz: Float, durationSeconds: Double, volume: Float = 0.5f)
    suspend fun playSound(soundId: String)

    /** True once enough of [speak]'s audio has rendered that the companion's "voice" is audible — used to gate visual sync in [com.cappsconsulting.prism.companion.presentation.PresentationState]. */
    val isSpeaking: Flow<Boolean>
}

/**
 * Doc 2.3 §2: "a composable haptic API can render an actual heartbeat *waveform*
 * — a soft-then-sharp double-pulse with the right rhythm and decay — rather than
 * one generic buzz standing in for a heartbeat... [and] shape the *spark* ...
 * distinctly from the *bloom*, where a single-purpose motor could only really do
 * one gesture well."
 *
 * [HapticGesture] names each *distinguishable feeling* the choreography needs —
 * the redesign surfaces gestures the Python `pulse(pattern: str, intensity: float)`
 * stringly-typed API only gestured at (`"heartbeat"`, `"single"`); see
 * `hal/android/AndroidHaptics.kt` for how each compiles to a `VibrationEffect`.
 */
enum class HapticGesture {
    /** Beat 2 — "the faintest haptic tick... the texture of *something deciding to begin*." */
    SPARK_TICK,

    /** Beat 3 — "a real heartbeat pattern: soft-sharp, soft-sharp... decaying gently between beats." */
    HEARTBEAT,

    /** A single gentle pulse — acknowledgement, not narrative (e.g. "I heard you"). Direct port of the original's generic `pulse("single")`. */
    SINGLE_PULSE,
}

interface HapticOutput {
    suspend fun play(gesture: HapticGesture, intensity: Float = 0.8f)
    suspend fun off()

    /** True if this device's actuator can render *shaped* waveforms (`VibrationEffect.Composition`, API 30+); false means [play] degrades to plain on/off timing — see `AndroidHaptics`. */
    val supportsComposition: Boolean
}

/**
 * The redesign's seam-preserving replacement for `HALBundle` — same role
 * ("everything the orchestrator needs to touch the outside world, gathered for
 * lifecycle management"), [GpioHAL]/[LedHAL] simply no longer named here because
 * nothing fills those roles on this platform (see class kdoc).
 */
data class CompanionHal(
    val camera: CameraSource,
    val microphone: MicrophoneSource,
    val speaker: SpeakerOutput,
    val haptics: HapticOutput,
) {
    suspend fun startAll() {
        camera.start()
        microphone.start()
    }

    suspend fun stopAll() {
        camera.stop()
        microphone.stop()
        haptics.off()
    }
}
