package com.cappsconsulting.prism.engine.perspective

import com.cappsconsulting.prism.engine.config.PrismConfig
import com.cappsconsulting.prism.engine.personas.CompanionPersona
import com.cappsconsulting.prism.engine.safety.SafetyGate
import com.cappsconsulting.prism.engine.safety.SafetyVerdict
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.random.Random

/** Direct port of `prism/modules/perspective_engine.py::PerspectiveRequest`. */
data class PerspectiveRequest(
    val visionLabel: String,
    val visionConfidence: Double,
    val moodLine: String,
    val memorySummary: String,
    val childQuestion: String?,
    val persona: CompanionPersona,
    val isOnline: Boolean,
)

/** Direct port of `prism/modules/perspective_engine.py::PerspectiveResponse`. */
data class PerspectiveResponse(
    val text: String,
    val isFallback: Boolean,
    val personaName: String,
    val latencyMs: Double,
)

/**
 * The single network seam [PerspectiveEngine] needs — replacing the Python's direct
 * `import anthropic; self._client = anthropic.AsyncAnthropic()` construction in
 * `_init_client`/`_call_llm`. Everything *around* the call (system-prompt assembly,
 * timeout policy, safety gating both directions, fallback construction) is pure,
 * differentiating logic that ports verbatim into this module; only "send these two
 * strings to the configured model and hand back its reply" is platform/network-shaped
 * enough to need an interface — `:companion-app` supplies the real `AnthropicLlmClient`
 * (HTTP + API key from secure storage); tests supply fakes. Same seam-drawing
 * principle as [com.cappsconsulting.prism.engine.learninglog.LearningLog]'s `onPersist`.
 */
interface LlmClient {
    suspend fun complete(model: String, maxTokens: Int, systemPrompt: String, userMessage: String): String
}

private val OFFLINE_TEMPLATES: Map<String, String> = mapOf(
    "lo" to "Hmm... I'm feeling a bit slow right now. That looks like {label}!",
    "mid" to "Oh, that looks like {label}! I wonder about it.",
    "hi" to "Oh wow! That's a {label}! How exciting!",
)

private fun fillLabel(template: String, label: String): String = template.replace("{label}", label)

/**
 * Direct port of `prism/modules/perspective_engine.py::PerspectiveEngine`.
 *
 * Lives in `:engine`, not `:companion-app`, for the same reason [SafetyGate] does
 * (see that class's kdoc): Doc 2.2 §6 requires the Parent Suite's Preview/Test-Drive
 * Mode to "run the real system, not a demo" — including this exact pipeline's input
 * gate, fallback voice, and output gate, not a re-implementation of it. Splitting the
 * *thinking* (this class — pure, testable on any JVM) from the *calling* ([LlmClient]
 * — platform-shaped) is what makes that "the real thing, runnable in preview" promise
 * actually checkable rather than aspirational.
 *
 * Pipeline order is a verbatim port of `generate()`: input gate (BLOCK -> immediate
 * neutral fallback, before anything else runs) -> online/client check -> timed LLM
 * call -> output gate -> sanitized text. Every fallback path funnels through
 * [fallback], exactly as the original funneled through `_fallback`.
 */
class PerspectiveEngine(
    private val config: PrismConfig,
    private val safety: SafetyGate,
    private val llmClient: LlmClient?,
    private val random: Random = Random.Default,
    private val nowMonotonicSeconds: () -> Double = { System.nanoTime() / 1_000_000_000.0 },
) {
    suspend fun generate(req: PerspectiveRequest): PerspectiveResponse {
        val t0 = nowMonotonicSeconds()

        // 1. Input gate — synchronous, always runs, exactly as the original ran it
        // before even checking whether a client exists.
        val inputText = "${req.visionLabel} ${req.childQuestion ?: ""}"
        if (safety.checkInput(inputText).verdict == SafetyVerdict.BLOCK) {
            return PerspectiveResponse(
                text = fillLabel(OFFLINE_TEMPLATES.getValue("mid"), "something interesting"),
                isFallback = true,
                personaName = req.persona.name,
                latencyMs = elapsedMs(t0),
            )
        }

        // 2. Offline / no client -> straight to the persona's own offline voice.
        if (!req.isOnline || llmClient == null) return fallback(req, t0)

        // 3. Timed call. Kotlin deviation, named once: Python's bare
        // `except (asyncio.TimeoutError, Exception)` becomes three branches here —
        // `withTimeout` signals its own timeout via `TimeoutCancellationException`,
        // a `CancellationException` subtype. Catching `CancellationException` broadly
        // (the literal translation of "catch everything") would also swallow *real*
        // structured-concurrency cancellation (e.g. the orchestrator's scope tearing
        // down mid-call) — a cardinal coroutine sin the original's threading model has
        // no equivalent danger for. So: catch the timeout specifically (-> fallback,
        // same as the original), rethrow any other cancellation (let the scope win),
        // and treat every remaining exception as a call failure (-> fallback, same
        // as the original's catch-all).
        val text = try {
            withTimeout((config.llmTimeoutS * 1000).toLong()) { callLlm(req) }
        } catch (e: TimeoutCancellationException) {
            return fallback(req, t0)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return fallback(req, t0)
        }

        // 4. Output gate — the second half of the layered safety model (Genotype:
        // "input gate -> system-prompt constraints -> output gate -> ... ").
        val safetyOut = safety.checkOutput(text)
        if (safetyOut.verdict == SafetyVerdict.BLOCK) return fallback(req, t0)

        return PerspectiveResponse(
            text = safetyOut.sanitized,
            isFallback = false,
            personaName = req.persona.name,
            latencyMs = elapsedMs(t0),
        )
    }

    private suspend fun callLlm(req: PerspectiveRequest): String {
        val client = checkNotNull(llmClient) { "callLlm invoked without a client — generate() must gate this" }
        return client.complete(
            model = config.llmModel,
            maxTokens = 120,
            systemPrompt = buildSystemPrompt(req),
            userMessage = buildUserMessage(req),
        ).trim()
    }

    /**
     * Direct port of `_fallback` — including its asymmetry: a persona *with*
     * `fallback_phrases` bands across lo/mid/hi, but a persona *without* them falls
     * through to [OFFLINE_TEMPLATES], which only ever resolves to "hi" or "mid"
     * here ("lo" is reachable in the phrase-banded branch but not this one). That
     * lopsidedness is in the original verbatim — preserved exactly, not "fixed",
     * because every persona this project ships *does* define phrases (see
     * `PersonasTest`), making the templates a true last resort whose own internal
     * shape was never exercised enough to matter. Renaming or restructuring it would
     * be inventing a requirement no doc states.
     */
    private fun fallback(req: PerspectiveRequest, t0: Double): PerspectiveResponse {
        val text = if (req.persona.fallbackPhrases.isNotEmpty()) {
            val band = confidenceBand(req.visionConfidence)
            val phrases = req.persona.fallbackPhrases[band] ?: listOf("How interesting!")
            phrases.random(random)
        } else {
            val band = if (req.visionConfidence > 0.66) "hi" else "mid"
            fillLabel(OFFLINE_TEMPLATES.getValue(band), req.visionLabel)
        }
        return PerspectiveResponse(text = text, isFallback = true, personaName = req.persona.name, latencyMs = elapsedMs(t0))
    }

    private fun elapsedMs(t0: Double): Double = (nowMonotonicSeconds() - t0) * 1000.0

    companion object {
        /** Same three-way split as `_fallback`'s inline ternary chain — `> 0.66` hi, `> 0.34` mid, else lo. */
        internal fun confidenceBand(confidence: Double): String = when {
            confidence > 0.66 -> "hi"
            confidence > 0.34 -> "mid"
            else -> "lo"
        }

        /** Direct port of `_build_system_prompt` — same four sections, same literal rules paragraph. */
        internal fun buildSystemPrompt(req: PerspectiveRequest): String =
            "${req.persona.systemPreamble}\n\n" +
                "MOOD: ${req.moodLine}\n\n" +
                "MEMORY: ${req.memorySummary.ifEmpty { "No specific memories yet." }}\n\n" +
                "Rules: respond in 1–2 short warm sentences max. " +
                "Never say anything scary, violent, or sad. " +
                "End with a question or invitation to explore."

        /** Direct port of `_build_user_message` — same percentage rounding (truncating `int()`), same optional question clause. */
        internal fun buildUserMessage(req: PerspectiveRequest): String {
            val confidencePct = (req.visionConfidence * 100).toInt()
            var msg = "I see: ${req.visionLabel} ($confidencePct% sure)."
            if (!req.childQuestion.isNullOrEmpty()) msg += " Child asks: ${req.childQuestion}"
            return msg
        }
    }
}
