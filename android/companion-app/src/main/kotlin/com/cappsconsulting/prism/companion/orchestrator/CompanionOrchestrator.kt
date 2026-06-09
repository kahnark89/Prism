package com.cappsconsulting.prism.companion.orchestrator

import android.util.Log
import com.cappsconsulting.prism.companion.awakening.AwakeningChoreographer
import com.cappsconsulting.prism.companion.awakening.AwakeningMachine
import com.cappsconsulting.prism.companion.awakening.AwakeningState
import com.cappsconsulting.prism.companion.hal.CompanionHal
import com.cappsconsulting.prism.companion.presentation.CompanionPresentationCompiler
import com.cappsconsulting.prism.companion.presentation.PresentationMode
import com.cappsconsulting.prism.companion.presentation.PresentationState
import com.cappsconsulting.prism.companion.recognition.RecognitionEngine
import com.cappsconsulting.prism.companion.vision.VisionClassifier
import com.cappsconsulting.prism.engine.config.PrismConfig
import com.cappsconsulting.prism.engine.grounding.GroundingAccumulator
import com.cappsconsulting.prism.engine.grounding.GroundingRecordSnapshot
import com.cappsconsulting.prism.engine.innerlife.CodebookEntrySnapshot
import com.cappsconsulting.prism.engine.innerlife.InnerLifeEngine
import com.cappsconsulting.prism.engine.innerlife.InnerLifeSnapshot
import com.cappsconsulting.prism.engine.innerlife.InnerLifeState
import com.cappsconsulting.prism.engine.learninglog.CIAERRecord
import com.cappsconsulting.prism.engine.learninglog.LearningLog
import com.cappsconsulting.prism.engine.memory.MemoryEngine
import com.cappsconsulting.prism.engine.memory.MemoryNodeSnapshot
import com.cappsconsulting.prism.engine.moodline.MoodLineCompiler
import com.cappsconsulting.prism.engine.perspective.PerspectiveEngine
import com.cappsconsulting.prism.engine.perspective.PerspectiveRequest
import com.cappsconsulting.prism.engine.personas.CompanionPersona
import com.cappsconsulting.prism.engine.personas.Personas
import com.cappsconsulting.prism.engine.safety.SafetyGate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.min

private const val TAG = "CompanionOrchestrator"

/**
 * Direct ports of the four persistence seams `orchestrator.py::set_stores` injected
 * post-construction ("Persistence stores — injected after DB init"). Every one stays
 * optional and is consulted with the same `if self._x_store:` guard the original used
 * — Room-backed implementations are `:companion-app` `data/` follow-on work (mirroring
 * `prism/persistence/schema.py`'s `inner_life_state`/`memory_nodes`+`codebook`/
 * `grounding`/`sessions` tables), gated on the Android SDK + KSP this sandbox lacks;
 * naming the seam *now* is what lets that work slot in later without touching this
 * file. [SessionStore] additionally folds in the increment/end calls `orchestrator.py`
 * made directly on `self._session_store` at the matching pipeline points.
 */
interface InnerLifeStore {
    suspend fun save(personaId: String, snapshot: InnerLifeSnapshot)
}

interface MemorySnapshotStore {
    suspend fun save(nodes: List<MemoryNodeSnapshot>, codebook: Map<String, CodebookEntrySnapshot>)
}

interface GroundingStore {
    suspend fun save(records: List<GroundingRecordSnapshot>)
}

interface SessionStore {
    suspend fun startSession(personaId: String)
    suspend fun incrementEvents()
    suspend fun endSession(eventCount: Int)
}

/** Bundles the four stores for the single [CompanionOrchestrator.attachStores] call — the `set_stores(...)` translation. */
data class OrchestratorStores(
    val innerLife: InnerLifeStore? = null,
    val memory: MemorySnapshotStore? = null,
    val grounding: GroundingStore? = null,
    val session: SessionStore? = null,
)

/**
 * A trimmed, privacy-shaped projection of `VisionResult` — label and confidence,
 * nothing else. `VisionResult.rawFrame` and `VisionResult.box` never make this
 * trip: they exist for the inference step that produced them ([VisionClassifier],
 * and — for `rawFrame` — the [RecognitionEngine] call standing next to it in
 * [handleTapToLook]) and must not outlive it. [lastReadout] is read by the
 * Compose layer to paint Doc 2.3 §3 Beat 1's "labels, confidence bars" — the
 * glass-box half of the mechanical-mode let-down (Doc 1.6 §5) — and a
 * long-lived `StateFlow` is exactly the kind of place a stray pixel buffer
 * could quietly overstay its welcome. Naming the projection *as a type* is
 * what makes "we only keep what the screen needs" a compile-time fact rather
 * than a discipline someone has to remember to uphold by hand.
 */
data class VisionReadout(val label: String, val confidence: Double)

/**
 * Direct port of `prism/orchestrator.py::Orchestrator` — the on-device half of queued
 * replatform item 3 ("split orchestrator across the two-app boundary, build the
 * net-new pairing/sync layer"). Everything that conducted the *child-facing* loop —
 * capture, infer, recognize, feel, speak, remember, log — lives here, unchanged in
 * substance; everything that watched from outside (the dashboard) is gone, replaced
 * by [com.cappsconsulting.prism.sync] carrying privacy-reviewed summaries to the
 * Parent Suite on its own schedule, not a live wire into this loop. Doc 3.0 §2 is the
 * citation for *why* the gate stays here too: input/output safety must run "where
 * content is produced and spoken" — on the Companion, every time, never optionally.
 *
 * Three collapses, each one named once:
 *
 * **`AudioFeedback` + `UIController` -> direct HAL calls + [CompanionPresentationCompiler]
 * + [AwakeningChoreographer].** The original wrapped `SpeakerHAL`/`HapticHAL`/`LedHAL`
 * in two extra layers that existed mostly to hold *derivation* logic (LED-from-state,
 * rate-from-energy). That derivation is what's differentiating — Epigenome 025 again
 * — so it ports verbatim (see [speakWithFeltVoice] for `AudioFeedback.speak`'s mood
 * modulation, [CompanionPresentationCompiler] for `_compile_led`'s color math); the
 * wrapper classes that did little beyond forwarding to a HAL don't reappear, because
 * Kotlin lets this orchestrator hold a `CompanionHal` and call it directly without
 * losing anything. `play_awakening_sequence`/`awakening_bloom`/`heartbeat_pulse`
 * collapse into the single [AwakeningChoreographer.runSequence] for the reasons that
 * class's kdoc gives (one clock, one timeline, Doc 2.3 §1's "arrive together as one event").
 *
 * **`EventBus` -> [kotlinx.coroutines.flow.SharedFlow], continued.** [AwakeningMachine.trigger]
 * already established the pattern this file completes: `AWAKENING_TRIGGERED` became a
 * `SharedFlow` this class collects (see [collectAwakeningTrigger]). `INNER_LIFE_TICKED`
 * and `MEMORY_ENCODED`, by contrast, are not republished as anything — they had exactly
 * one consumer in the original (the dashboard's live telemetry), and that consumer's
 * replacement, the Parent Suite, deliberately does *not* want a live tap into every
 * tick and every encoded concept. It wants [com.cappsconsulting.prism.sync.payload.SessionSummary]
 * — batched, reviewed, privacy-shaped snapshots, the Doc 3.0 §3.1 "keep this list
 * short" instinct applied to *what gets a wire at all*, not just what crosses it.
 * Wiring this orchestrator's encode/tick events toward `:sync` is exactly the kind of
 * cross-app assembly [attachStores] already demonstrates the seam for — the host
 * app's job, not this class's.
 *
 * **The shutter button -> the tap gesture, named in [HardwareAbstractionLayer]'s kdoc
 * already**, which is why [run] has no `register_button_callback` step: the
 * registration *is* the Compose layer calling [handleTapToLook] on tap. Same seam,
 * phone-native trigger, one fewer indirection to maintain.
 *
 * @property scope Used for exactly one thing: the `asyncio.create_task(thinking_cue())`
 * translation in [handleTapToLook] (a fire-and-forget animation that must not block the
 * tap handler). [run]'s four loops use their own `coroutineScope` — the direct
 * `asyncio.gather` translation — and need no external scope at all.
 */
class CompanionOrchestrator(
    private val config: PrismConfig,
    private val hal: CompanionHal,
    private val innerLife: InnerLifeEngine,
    private val memory: MemoryEngine,
    private val moodLine: MoodLineCompiler,
    private val grounding: GroundingAccumulator,
    private val safety: SafetyGate,
    private val learningLog: LearningLog,
    private val perspective: PerspectiveEngine,
    private val vision: VisionClassifier,
    private val recognition: RecognitionEngine,
    private val awakening: AwakeningMachine,
    private val choreographer: AwakeningChoreographer,
    private val scope: CoroutineScope,
    private val nowEpochSeconds: () -> Double = { System.currentTimeMillis() / 1000.0 },
    private val nowMonotonicSeconds: () -> Double = { System.nanoTime() / 1.0e9 },
) {
    private val _presentation = MutableStateFlow(
        CompanionPresentationCompiler.compile(innerLife.getState(), activePersona(), PresentationMode.MECHANICAL)
    )

    /**
     * The single shared *visual* channel both modes paint from — color, motion,
     * form, exactly [PresentationState]'s vocabulary and nothing past its edges.
     * During an ordinary session this mirrors [CompanionPresentationCompiler.compile]'s
     * output, exactly as `_led.set_state(self._compile_led(...))` drove the ring; during
     * the awakening sequence, [collectAwakeningTrigger] re-points this same `StateFlow`
     * at [choreographer]'s frames instead. One sink for both tracks — not a deviation
     * but the *same* structural truth the original had ([UIController]'s `_led` was the
     * single target both `update_from_state` and `awakening_bloom`/`heartbeat_pulse`
     * wrote to, concurrently, via `asyncio.gather`), now expressed the idiomatic-Kotlin
     * way: by routing rather than racing.
     *
     * Not, however, the *only* thing the Compose layer collects — see [lastReadout]
     * and [awakeningState]. [PresentationState] was never meant to carry text or
     * mode transitions, and stretching it to do so would be the same mistake as
     * stretching `LedState` to log a label string: a vocabulary doing work outside
     * the thing it actually describes.
     */
    val presentation: StateFlow<PresentationState> = _presentation.asStateFlow()

    private val _lastReadout = MutableStateFlow<VisionReadout?>(null)

    /**
     * "Here's what I just saw, and how sure I am" — the textual half of the
     * mechanical-mode glass-box view (Doc 2.3 §3 Beat 1's "labels, confidence
     * bars," Doc 1.6 §5's "deliberate let-down" that makes the AI's uncertainty
     * legible rather than hidden). [PresentationState] cannot carry this — it
     * speaks color and motion, not text — so [MechanicalPresentationScreen]
     * needs its own, smaller channel to read alongside [presentation]. `null`
     * until the first tap, honestly: there is nothing to show a glass box before
     * the first frame has been looked at, and a fabricated placeholder reading
     * would be exactly the kind of "looks like it's working" dishonesty
     * [VisionClassifier]'s kdoc warns against.
     */
    val lastReadout: StateFlow<VisionReadout?> = _lastReadout.asStateFlow()

    /**
     * Forwards [AwakeningMachine.state] — [com.cappsconsulting.prism.companion.ui.CompanionScreen]
     * needs to react to the MECHANICAL -> AWAKENED transition the instant it
     * lands (see that `StateFlow`'s kdoc for why a plain getter wasn't enough
     * once something outside the machine started watching it), and [awakening]
     * itself is `private` — this is the seam that lets the Compose layer observe
     * the transition without reaching past the orchestrator that owns it.
     */
    val awakeningState: StateFlow<AwakeningState> = awakening.state

    @Volatile private var running = false
    private var stores: OrchestratorStores? = null
    private var eventCount = 0
    private var lastInteractionMonotonic = nowMonotonicSeconds()

    /**
     * Direct port of `self._is_online`, including its honest gap: the original set it
     * once in `__init__` and never again — connectivity *detection* was never wired in
     * either codebase. Here it's a plain mutable property so the host can drive it from
     * `ConnectivityManager.NetworkCallback` (the natural Android seam) the moment that
     * follow-on work happens; until then it defaults `true`, exactly as `self._is_online = True` did.
     */
    var isOnline: Boolean = true

    private fun activePersona(): CompanionPersona = Personas.get(config.activeCompanion)

    /**
     * [asyncio.gather] translation — suspends for the lifetime of the session, exactly
     * as `await orchestrator.run()` did, returning only when every loop below does
     * (which, by construction, is "never" short of cancellation or [shutdown]).
     */
    suspend fun run(): Unit = coroutineScope {
        running = true
        val persona = activePersona()
        Log.i(TAG, "Orchestrator starting — companion: ${persona.name}, child: ${config.childName}")

        stores?.session?.startSession(persona.id)

        launch { collectAwakeningTrigger() }
        launch { innerLifeTickLoop() }
        launch { memoryDecayLoop() }
        launch { saveStateLoop() }
        launch { idleWatchLoop() }
    }

    /** Direct port of `shutdown` — save, close out the session, release the hardware, in that order. */
    suspend fun shutdown() {
        running = false
        Log.i(TAG, "Orchestrator shutting down — saving state…")
        saveAll()
        stores?.session?.endSession(eventCount)
        hal.stopAll()
    }

    /** The `set_stores(...)` translation — see [OrchestratorStores]'s kdoc for why every member stays optional. */
    fun attachStores(stores: OrchestratorStores) {
        this.stores = stores
    }

    // ─── Hot path ──────────────────────────────────────────────────────

    /**
     * Direct port of `handle_shutter_press` — same thirteen steps, same order, same
     * pipeline shape ("camera -> vision -> (optional recognition) -> inner life event
     * -> safety -> mood_line -> perspective -> safety -> audio -> learning_log ->
     * memory -> grounding -> ui"), renamed for its phone-native trigger: there is no
     * shutter, only a tap on the live presentation (see [HardwareAbstractionLayer]'s
     * kdoc and this class's). That rename surfaces in exactly one other place — the
     * CIAER record's `action` field, `"snap"` -> `"tap-to-look"` — because a future
     * reader of the learning log deserves a verb that names what actually happened on
     * *this* platform, not the one it replaced.
     */
    suspend fun handleTapToLook() {
        lastInteractionMonotonic = nowMonotonicSeconds()
        eventCount += 1

        val persona = activePersona()
        val now = nowEpochSeconds()
        // Verbatim port of the orchestrator's own `t_hours` calc — note this omits the
        // `+ second/3600` term `InnerLifeEngine.tick` adds; that asymmetry is in the
        // Python original too (`orchestrator.py` line 130 vs. `inner_life.py::tick`),
        // preserved here rather than quietly "fixed" into a consistency the source never had.
        val dt = Instant.fromEpochMilliseconds((now * 1000).toLong()).toLocalDateTime(TimeZone.currentSystemDefault())
        val tHours = dt.hour + dt.minute / 60.0

        // 1. Capture frame
        val frame = hal.camera.captureFrame()

        // 2. Vision inference
        val visionResult = vision.infer(frame)
        Log.i(TAG, "[tap-to-look] vision: ${visionResult.label} (${(visionResult.confidence * 100).toInt()}%)")
        _lastReadout.value = VisionReadout(visionResult.label, visionResult.confidence)

        // 3. Recognition check (non-blocking to inner life)
        if (!awakening.isAwakened && recognition.isEnrolled()) {
            val recResult = recognition.recognize(frame)
            if (recResult.isEnrolledChild) {
                awakening.checkAndTrigger(isEnrolledChild = true, recognitionConfidence = recResult.confidence)
            }
        }

        // 4. Inner life event
        val mode = if (awakening.isMechanical) PresentationMode.MECHANICAL else PresentationMode.AWAKENED
        val prevConcepts = memory.getTopActivated(k = 5).map { it.concept }
        val event = if (visionResult.label in prevConcepts) "repeat" else "novel"
        innerLife.fireEvent(event)
        val state = innerLife.tick(now)

        // 5. UI thinking cue — `asyncio.create_task` translation: fired into [scope],
        // not this suspend function's own continuation, because handleTapToLook is
        // called directly from the tap gesture and must not block ~1.2s on an animation.
        scope.launch { playThinkingCue() }

        // 6. Input safety
        val safetyIn = safety.checkInput(visionResult.label)
        val effectiveLabel = safetyIn.sanitized.ifEmpty { "something interesting" }

        // 7. Mood-line + memory
        val topMemories = memory.getTopActivated(conceptHint = visionResult.label, k = 3)
        val moodLineText = moodLine.compile(state, persona, config.childName, topMemories, tHours)
        val memorySummary = MoodLineCompiler.memorySentence(topMemories)

        // 8. Perspective engine (the timeout/fallback policy lives inside `generate` itself)
        val response = perspective.generate(
            PerspectiveRequest(
                visionLabel = effectiveLabel,
                visionConfidence = visionResult.confidence,
                moodLine = moodLineText,
                memorySummary = memorySummary,
                childQuestion = null,
                persona = persona,
                isOnline = isOnline,
            )
        )
        Log.i(TAG, "[${persona.name}${if (response.isFallback) " (fallback)" else ""}] ${response.text}")

        // 9. Audio (output safety already applied inside the perspective engine)
        speakWithFeltVoice(response.text, persona, state)

        // 10. Memory encode
        val salience = min(1.0, visionResult.confidence + (if (event == "novel") 0.1 else 0.0))
        memory.encode(
            concept = visionResult.label,
            episode = "${visionResult.label} (session)",
            salience = salience,
        )
        // `await self._bus.publish_async(MEMORY_ENCODED, …)` has no translation here —
        // see this class's kdoc, "EventBus -> SharedFlow, continued".

        // 11. Grounding exposure
        grounding.recordExposure(visionResult.label)

        // 12. Learning log (CIAER record)
        learningLog.recordEvent(
            CIAERRecord(
                timestamp = now,
                cause = visionResult.label,
                intuition = visionResult.label,
                confidenceInVoice = "SKILL",
                action = "tap-to-look",
                effect = response.text,
                reaction = "observed",
                salience = salience,
                result = "none",
                preEnvTimeOfDay = tHours,
                preEnvCompanionMood = moodLineText.take(80),
                preEnvChildRecentConcepts = prevConcepts.take(3),
            )
        )
        stores?.session?.incrementEvents()

        // 13. UI update
        _presentation.value = CompanionPresentationCompiler.compile(state, persona, mode)
    }

    // ─── Background loops — direct ports of the four `_..._loop` coroutines ───────

    private suspend fun innerLifeTickLoop() {
        while (running) {
            try {
                val now = nowEpochSeconds()
                val state = innerLife.tick(now)
                val persona = activePersona()
                val mode = if (awakening.isAwakened) PresentationMode.AWAKENED else PresentationMode.MECHANICAL
                _presentation.value = CompanionPresentationCompiler.compile(state, persona, mode)
                // `await self._bus.publish_async(INNER_LIFE_TICKED, …)` — see kdoc.
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Inner life tick error", e)
            }
            delay((config.tickIntervalS * 1000).toLong())
        }
    }

    private suspend fun memoryDecayLoop() {
        while (running) {
            delay((config.decayIntervalS * 1000).toLong())
            try {
                val day = nowEpochSeconds() / 86_400.0
                val pruned = memory.advanceTime(day)
                if (pruned.isNotEmpty()) Log.d(TAG, "Memory pruned ${pruned.size} nodes")
                innerLife.applyAffectionBoost(memory.getAffectionMass())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Memory decay error", e)
            }
        }
    }

    private suspend fun saveStateLoop() {
        while (running) {
            delay((config.saveIntervalS * 1000).toLong())
            saveAll()
        }
    }

    private suspend fun idleWatchLoop() {
        while (running) {
            delay(30_000)
            val idleSeconds = nowMonotonicSeconds() - lastInteractionMonotonic
            if (idleSeconds >= config.idleTimeoutS) innerLife.fireEvent("idle")
        }
    }

    private suspend fun saveAll() {
        try {
            val persona = activePersona()
            stores?.innerLife?.save(persona.id, innerLife.getState().snapshot())
            stores?.memory?.let { store ->
                val (nodes, codebook) = memory.getSnapshot()
                store.save(nodes, codebook)
            }
            stores?.grounding?.save(grounding.getSnapshot())
            Log.d(TAG, "State saved")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Save failed", e)
        }
    }

    // ─── Awakening — the `AWAKENING_TRIGGERED` subscriber translation ─────────────

    /**
     * Direct port of the `_on_awakening_triggered` / `_run_awakening_sequence` pair —
     * collapsed into one collector because [AwakeningMachine.trigger] *is* the
     * subscription (no separate `bus.subscribe` registration step needed) and because
     * the original's three-way `asyncio.gather(play_awakening_sequence, awakening_bloom,
     * heartbeat_pulse)` is now the single [AwakeningChoreographer.runSequence] (see
     * that class's kdoc for why one clock replaced three). [AwakeningMachine.trigger]
     * is one-shot per device-reset by construction, so collecting sequentially here —
     * rather than `launch`-ing each emission — changes nothing observable; it does
     * keep this collector's lifetime legible as "runs the sequence, then waits for a
     * reset that (on this device, this boot) will never come," exactly like the
     * original's state machine guarantee promises.
     *
     * Also folds in the one line `_run_awakening_sequence` computed and never used
     * (`state = self._inner_life.get_state()`) — dropped, not ported: carrying forward
     * a value nothing reads would be translating a typo, not the design.
     */
    private suspend fun collectAwakeningTrigger() {
        awakening.trigger.collect {
            val persona = activePersona()

            val forwarder = scope.launch {
                choreographer.presentation.collect { frame -> _presentation.value = frame }
            }
            try {
                choreographer.runSequence(config.childName, persona)
            } finally {
                forwarder.cancel()
            }

            awakening.completeAwakening()
            innerLife.fireEvent("reunion")
        }
    }

    // ─── Small helpers — ported pieces that had no other natural home ────────────

    /**
     * Direct port of `UIController.thinking_cue` — three alternations between the two
     * [CompanionPresentationCompiler.thinkingCueFrames], 200ms apiece (the original's
     * `for _ in range(3): set_state(A); sleep(0.2); set_state(B); sleep(0.2)`, with
     * [PresentationState] standing in for `LedState` exactly as it does everywhere else).
     */
    private suspend fun playThinkingCue() {
        val (active, dim) = CompanionPresentationCompiler.thinkingCueFrames()
        repeat(3) {
            _presentation.value = active
            delay(200)
            _presentation.value = dim
            delay(200)
        }
    }

    /**
     * Direct port of `AudioFeedback.speak` — "speak text with per-character voice
     * profile modulated by inner life state." Both modulations are verbatim:
     * energy shifts the *rate* by up to ±30 wpm around the persona's baseline
     * (clamped to `[80, 220]`), mood shifts the *volume* linearly from a quiet
     * 0.6 floor to a warm 1.0 ceiling. A grumpy companion is quieter; a joyful
     * one, louder — the felt logic of a voice that means what it's feeling,
     * not just narrates it in the words it picks.
     */
    private suspend fun speakWithFeltVoice(text: String, persona: CompanionPersona, state: InnerLifeState) {
        val delta = ((state.e - 0.5) * 60.0).toInt()
        val rate = (persona.voiceRate + delta).coerceIn(80, 220)
        val volume = (0.6 + state.m * 0.4).toFloat()
        hal.speaker.speak(text = text, rate = rate, volume = volume)
    }
}
