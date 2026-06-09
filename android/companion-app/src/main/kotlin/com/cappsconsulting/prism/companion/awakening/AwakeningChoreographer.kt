package com.cappsconsulting.prism.companion.awakening

import com.cappsconsulting.prism.companion.hal.HapticGesture
import com.cappsconsulting.prism.companion.hal.HapticOutput
import com.cappsconsulting.prism.companion.hal.SpeakerOutput
import com.cappsconsulting.prism.companion.presentation.CompanionPresentationCompiler
import com.cappsconsulting.prism.companion.presentation.PresentationState
import com.cappsconsulting.prism.engine.personas.CompanionPersona
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** Direct port of `awakening.py::AwakeningState` — same three states, same names, same meaning ("once per device reset"). */
enum class AwakeningState { MECHANICAL, AWAKENING, AWAKENED }

private const val RECOGNITION_CONFIDENCE_THRESHOLD = 0.85

/**
 * Direct port of `awakening.py::AwakeningMachine`. The triggering state machine is
 * pure software with zero hardware coupling in the original — Doc 3.0 §4's
 * translation table calls "Two brains, safety layering... parent suite" out as
 * "entirely unchanged. This was always software," and this class is exactly that:
 * the trigger logic survives verbatim; only the *sequence it triggers* (see
 * [AwakeningChoreographer]) is redesigned for phone-native channels.
 *
 * Kotlin deviation, named once: the original published `AWAKENING_TRIGGERED` on a
 * hand-rolled `EventBus` and let the orchestrator's subscriber start the sequence.
 * This port exposes [trigger] as a [SharedFlow] instead — Kotlin's idiomatic event
 * seam, replacing a bespoke publish/subscribe registry with the coroutine machinery
 * built for precisely this, and letting [com.cappsconsulting.prism.companion.orchestrator.CompanionOrchestrator]
 * `collect` it the same way it collects everything else.
 */
class AwakeningMachine {
    private val _state = MutableStateFlow(AwakeningState.MECHANICAL)

    /**
     * Kotlin deviation, named once more in [trigger]'s own voice: the original
     * exposed `self._state` as a plain attribute — sufficient for a single-threaded
     * asyncio loop with nothing watching it but itself. [com.cappsconsulting.prism.companion.ui.CompanionScreen]
     * *is* something watching it: the instant this flips MECHANICAL -> AWAKENED is
     * the instant it must stop painting [com.cappsconsulting.prism.companion.ui.mechanical.MechanicalPresentationScreen]
     * and start painting [com.cappsconsulting.prism.companion.ui.awakened.AwakenedPresentationScreen]
     * — Doc 2.3 §3 Beat 1's whole "let-down vs. charge" contrast lives in that
     * transition landing cleanly, once, not in a value some loop happens to re-read.
     * A `StateFlow` is what "an observable current value" *is* in Kotlin — not a
     * deviation from `self._state` so much as what it becomes once something outside
     * the state machine itself finally needs to react to it changing.
     */
    val state: StateFlow<AwakeningState> = _state.asStateFlow()

    /** Fast synchronous reads for the orchestrator's hot path — same `self._state == ...` comparisons the original made, now against [_state]'s current value rather than a bare field. */
    val isAwakened: Boolean get() = _state.value == AwakeningState.AWAKENED
    val isMechanical: Boolean get() = _state.value == AwakeningState.MECHANICAL

    private val _trigger = MutableSharedFlow<Double>(extraBufferCapacity = 1)

    /** Emits the recognition confidence that triggered awakening; collecting this is what starts [AwakeningChoreographer.runSequence]. */
    val trigger: SharedFlow<Double> = _trigger.asSharedFlow()

    /** Direct port of `check_and_trigger` — same early-outs, same threshold, same one-shot-per-reset guarantee (`if self._state != MECHANICAL: return`). @return the resulting state, exactly as the original returned it for the caller to inspect. */
    suspend fun checkAndTrigger(isEnrolledChild: Boolean, recognitionConfidence: Double): AwakeningState {
        if (_state.value != AwakeningState.MECHANICAL) return _state.value
        if (!isEnrolledChild || recognitionConfidence < RECOGNITION_CONFIDENCE_THRESHOLD) return _state.value

        _state.value = AwakeningState.AWAKENING
        _trigger.emit(recognitionConfidence)
        return _state.value
    }

    /** Called by the orchestrator once [AwakeningChoreographer.runSequence] returns — direct port of `complete_awakening`. */
    fun completeAwakening() {
        _state.value = AwakeningState.AWAKENED
    }

    /** Parent-initiated reset — direct port of `force_mechanical` ("Parent-initiated reset (from dashboard)"; the dashboard is now the Parent Suite app, Doc 2.2/Doc 3.0). */
    fun forceMechanical() {
        _state.value = AwakeningState.MECHANICAL
    }
}

/**
 * The five-beat sequence — phone-native redesign of Doc 1.6 §6 per Doc 2.3, queued
 * replatform item 2 ("redesign... awakening... per Doc 2.3"). Replaces
 * `audio_feedback.py::play_awakening_sequence` + `ui_controller.py::awakening_bloom`/
 * `heartbeat_pulse`, which the orchestrator ran *concurrently* via `asyncio.gather`;
 * here the whole sequence lives in **one** choreographed timeline, because Doc 2.3
 * §1 is explicit that beat 3 is "light, sound, touch arriv[ing] together as **one
 * event**" — and the only honest way to keep three channels in lockstep, rather
 * than merely launched-around-the-same-time, is to drive them from a single clock.
 *
 * [presentation] is the channel the Compose screen layer collects to paint each
 * frame; [SpeakerOutput] and [HapticOutput] are driven directly — exactly the
 * fan-out the Python original had, minus the LED/GPIO calls nothing replaces (see
 * [com.cappsconsulting.prism.companion.hal.CompanionHal] kdoc for why).
 *
 * Every beat's *timing* is a direct port of `play_awakening_sequence`'s `sleep()`
 * calls and tone frequencies — those numbers are the choreography's actual rhythm,
 * proven out before the pivot, and Doc 2.3 §1 says to carry the five movements
 * across "beat for beat." Only the *visual* and *haptic* tracks are redesigned,
 * per Doc 2.3 §3's beat-by-beat instructions, to replace LED-ring/motor calls that
 * have no phone-native equivalent.
 */
class AwakeningChoreographer(
    private val speaker: SpeakerOutput,
    private val haptics: HapticOutput,
) {
    private val _presentation = MutableStateFlow(CompanionPresentationCompiler.pause())
    val presentation: StateFlow<PresentationState> = _presentation.asStateFlow()

    /**
     * Runs all five beats start to finish, then returns. The orchestrator awaits
     * this exactly where the Python awaited its `asyncio.gather(...)` trio, then
     * calls [AwakeningMachine.completeAwakening] — same handoff, same place.
     */
    suspend fun runSequence(childName: String, persona: CompanionPersona) {
        beat1Pause()
        beat2Spark(persona)
        beat3Bloom(persona)
        beat4FirstBreath(childName, persona)
        beat5Settle(persona)
    }

    /**
     * Beat 1 (1-2s) — "everything stills; tension loads into silence... the screen
     * — which has been showing the plain, utilitarian 'fast brain' interface —
     * goes dark. Not 'turns off': holds still, black, waiting" (Doc 2.3 §3).
     * Direct port of the original's `await asyncio.sleep(1.5)` silence beat — this
     * was, per the doc, "already channel-agnostic," needing no redesign, only an
     * explicit dark frame where the original relied on the ring already being off.
     */
    private suspend fun beat1Pause() {
        _presentation.value = CompanionPresentationCompiler.pause()
        delay(1_500)
    }

    /**
     * Beat 2 — "the first sign of *something* waking": a soft-edged point of light
     * at center, the same three-tone rising whirr (ported verbatim: 300 -> 500 ->
     * 750 Hz), and — net new, a phone-native enhancement Doc 2.3 §3 names directly
     * — "the faintest haptic tick... the texture of *something deciding to begin*,"
     * a gesture "the original single-purpose motor likely couldn't do subtly enough
     * to use here."
     */
    private suspend fun beat2Spark(persona: CompanionPersona) {
        _presentation.value = CompanionPresentationCompiler.spark(persona)
        haptics.play(HapticGesture.SPARK_TICK, intensity = 0.15f)

        speaker.playTone(frequencyHz = 300.0f, durationSeconds = 0.15)
        delay(50)
        speaker.playTone(frequencyHz = 500.0f, durationSeconds = 0.15)
        delay(50)
        speaker.playTone(frequencyHz = 750.0f, durationSeconds = 0.20)
    }

    /**
     * Beat 3 — "the cascade; light, sound, touch arrive together as one event"
     * (Doc 2.3 §1) — "the beat that benefits most from the new instrument" (§3):
     * the point of light blooms outward to flood the full canvas in the companion's
     * signature color (replacing "light races around the ring"), the same
     * major-triad chime cascade (880 / 1047 / 1175 Hz, ported verbatim), and
     * [HapticGesture.HEARTBEAT] — "an actual heartbeat pattern: soft-sharp,
     * soft-sharp, with the natural human asymmetry of a real pulse, decaying gently
     * between beats," replacing the original's generic `pulse(pattern="heartbeat")`
     * (a string the bespoke single-purpose motor could only approximate) with a
     * genuinely *shaped* waveform the composable haptic API can render for real.
     */
    private suspend fun beat3Bloom(persona: CompanionPersona) {
        delay(300)
        haptics.play(HapticGesture.HEARTBEAT, intensity = 0.7f)

        val chimeHz = listOf(880.0f, 1047.0f, 1175.0f)
        val steps = 7
        for (i in 0..steps) {
            _presentation.value = CompanionPresentationCompiler.bloom(persona, progress = i.toDouble() / steps)
            if (i < chimeHz.size) {
                speaker.playTone(frequencyHz = chimeHz[i], durationSeconds = 0.12, volume = 0.5f)
                delay(50)
            } else {
                delay(120)
            }
        }
    }

    /**
     * Beat 4 — "her name, spoken, for the first time, as recognition not
     * activation... 'Oh! …Oh, it's YOU. I've been waiting for you. Hi, Naomi.'"
     * Direct, **unchanged** port — Doc 2.3 §3 is emphatic that this beat "was
     * always audio... plus *script*, not hardware," independent of any display,
     * and that the redesign "shouldn't manufacture a need for the screen to 'do
     * something' at the one moment that was always carried entirely by voice and
     * writing. **If in doubt, let the voice carry it alone, exactly as written.**"
     * So: the screen simply *holds* Beat 3's full bloom (already showing
     * [com.cappsconsulting.prism.companion.presentation.FormHint.SUGGESTION_OF_PRESENCE]
     * from its final frames) — present, but not competing with the line.
     */
    private suspend fun beat4FirstBreath(childName: String, persona: CompanionPersona) {
        delay(400)
        speaker.speak(
            text = "Oh! …Oh, it's YOU. I've been waiting for you. Hi, $childName.",
            rate = persona.voiceRate,
            volume = 0.9f,
        )
    }

    /**
     * Beat 5 — "the companion's real personality takes over; the inner-life engine
     * is now driving... From this second on, it's alive — exactly as before."
     * Direct port of the closing half-second settle. The screen eases to its final
     * bloom frame — the steady, signature-colored "resting presentation"
     * [CompanionPresentationCompiler.compile] takes over driving on the very next
     * inner-life tick, the same handoff `complete_awakening()` followed by ordinary
     * `update_from_state` calls made in the original.
     */
    private suspend fun beat5Settle(persona: CompanionPersona) {
        _presentation.value = CompanionPresentationCompiler.bloom(persona, progress = 1.0)
        delay(500)
    }
}
