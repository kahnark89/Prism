package com.cappsconsulting.prism.engine.perspective

import com.cappsconsulting.prism.engine.config.PrismConfig
import com.cappsconsulting.prism.engine.personas.CompanionPersona
import com.cappsconsulting.prism.engine.personas.Personas
import com.cappsconsulting.prism.engine.safety.SafetyGate
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test

private class FakeLlmClient(
    private val response: String? = null,
    private val error: Throwable? = null,
    private val delayMs: Long = 0,
) : LlmClient {
    var callCount: Int = 0
        private set

    override suspend fun complete(model: String, maxTokens: Int, systemPrompt: String, userMessage: String): String {
        callCount++
        if (delayMs > 0) delay(delayMs)
        error?.let { throw it }
        return response ?: ""
    }
}

/** A persona with no `fallbackPhrases` — every shipped persona has them (see `PersonasTest`), so this is purpose-built to reach `_fallback`'s other branch. */
private val BLANK_PERSONA = CompanionPersona(
    id = "blank", name = "Blank", displayName = "Blank", lens = "", strength = "",
    dilemma = "", lifeLesson = "", baseM = 0.5, baseE = 0.5, baseC = 0.5, baseA = 0.5, baseS = 0.5,
    fallbackPhrases = emptyMap(),
)

private fun requestFor(
    persona: CompanionPersona = Personas.PIP,
    visionLabel: String = "apple",
    visionConfidence: Double = 0.8,
    childQuestion: String? = null,
    isOnline: Boolean = true,
): PerspectiveRequest = PerspectiveRequest(
    visionLabel = visionLabel,
    visionConfidence = visionConfidence,
    moodLine = "You are ${persona.name}. You feel curious.",
    memorySummary = "",
    childQuestion = childQuestion,
    persona = persona,
    isOnline = isOnline,
)

/**
 * Direct-port behavior tests for `prism/modules/perspective_engine.py::PerspectiveEngine.generate`
 * — the pipeline order (input gate -> online/client check -> timed call -> output gate ->
 * fallback) is what's under test here, the same way [com.cappsconsulting.prism.engine.safety.SafetyGateTest]
 * tests `SafetyGate` and `LearningLogTest` tests `LearningLog`'s OGC enforcement: this is
 * differentiating logic the port has to prove behaves identically, not glue to wave through.
 */
class PerspectiveEngineTest {

    private val cfg = PrismConfig()
    private val safety = SafetyGate(cfg)

    @Test
    fun `input that trips the safety gate short-circuits to a neutral fallback before any client is consulted`() = runTest {
        val client = FakeLlmClient(response = "should never be reached")
        val engine = PerspectiveEngine(cfg, safety, client)

        val response = engine.generate(requestFor(visionLabel = "a gun"))

        assertThat(response.isFallback).isTrue()
        assertThat(response.text).isEqualTo("Oh, that looks like something interesting! I wonder about it.")
        assertThat(client.callCount).isEqualTo(0)
    }

    @Test
    fun `offline requests go straight to the persona's fallback voice without ever touching the client`() = runTest {
        val client = FakeLlmClient(response = "should never be reached")
        val engine = PerspectiveEngine(cfg, safety, client)

        val response = engine.generate(requestFor(isOnline = false, visionConfidence = 0.9))

        assertThat(response.isFallback).isTrue()
        assertThat(response.text).isIn(Personas.PIP.fallbackPhrases.getValue("hi"))
        assertThat(client.callCount).isEqualTo(0)
    }

    @Test
    fun `a missing client is treated exactly like being offline`() = runTest {
        val engine = PerspectiveEngine(cfg, safety, llmClient = null)

        val response = engine.generate(requestFor(visionConfidence = 0.5))

        assertThat(response.isFallback).isTrue()
        assertThat(response.text).isIn(Personas.PIP.fallbackPhrases.getValue("mid"))
    }

    @Test
    fun `a successful call returns the model's voice, sanitized and trimmed — not the fallback`() = runTest {
        val client = FakeLlmClient(response = "  Look at that apple! Want to count its seeds together?  ")
        val engine = PerspectiveEngine(cfg, safety, client)

        val response = engine.generate(requestFor())

        assertThat(response.isFallback).isFalse()
        assertThat(response.text).isEqualTo("Look at that apple! Want to count its seeds together?")
        assertThat(response.personaName).isEqualTo("Pip")
    }

    @Test
    fun `a call that runs past the configured timeout falls back exactly like a failed call`() = runTest {
        val slowClient = FakeLlmClient(response = "too slow", delayMs = 500)
        val engine = PerspectiveEngine(cfg.copy(llmTimeoutS = 0.05), safety, slowClient)

        val response = engine.generate(requestFor())

        assertThat(response.isFallback).isTrue()
        assertThat(response.text).isNotEqualTo("too slow")
    }

    @Test
    fun `a call that throws falls back rather than propagating the failure to the caller`() = runTest {
        val brokenClient = FakeLlmClient(error = RuntimeException("network exploded"))
        val engine = PerspectiveEngine(cfg, safety, brokenClient)

        val response = engine.generate(requestFor())

        assertThat(response.isFallback).isTrue()
    }

    @Test
    fun `output that trips the safety gate on the way out is replaced by the fallback voice, never spoken`() = runTest {
        val client = FakeLlmClient(response = "That looks like a knife!")
        val engine = PerspectiveEngine(cfg, safety, client)

        val response = engine.generate(requestFor())

        assertThat(response.isFallback).isTrue()
        assertThat(response.text).doesNotContain("knife")
    }

    @Test
    fun `fallback bands by confidence across a persona's lo, mid, and hi phrases`() = runTest {
        val engine = PerspectiveEngine(cfg, safety, llmClient = null)

        assertThat(engine.generate(requestFor(visionConfidence = 0.90)).text).isIn(Personas.PIP.fallbackPhrases.getValue("hi"))
        assertThat(engine.generate(requestFor(visionConfidence = 0.50)).text).isIn(Personas.PIP.fallbackPhrases.getValue("mid"))
        assertThat(engine.generate(requestFor(visionConfidence = 0.10)).text).isIn(Personas.PIP.fallbackPhrases.getValue("lo"))
    }

    @Test
    fun `a persona with no fallback phrases falls through to the offline templates, which only ever resolve to mid or hi`() = runTest {
        val engine = PerspectiveEngine(cfg, safety, llmClient = null)

        val high = engine.generate(requestFor(persona = BLANK_PERSONA, visionLabel = "a kite", visionConfidence = 0.90))
        assertThat(high.text).isEqualTo("Oh wow! That's a a kite! How exciting!")

        // Even a low-confidence read resolves to "mid" here — `_fallback` never reaches
        // a "lo" branch once it's past the persona-phrases check. Verbatim asymmetry,
        // preserved exactly (see PerspectiveEngine.fallback's kdoc).
        val low = engine.generate(requestFor(persona = BLANK_PERSONA, visionLabel = "a kite", visionConfidence = 0.10))
        assertThat(low.text).isEqualTo("Oh, that looks like a kite! I wonder about it.")
    }

    @Test
    fun `system prompt and user message assemble exactly as the Python helpers built them`() {
        val req = requestFor(visionLabel = "a butterfly", visionConfidence = 0.83, childQuestion = "why does it have spots?")

        assertThat(PerspectiveEngine.buildSystemPrompt(req)).isEqualTo(
            "${Personas.PIP.systemPreamble}\n\n" +
                "MOOD: ${req.moodLine}\n\n" +
                "MEMORY: No specific memories yet.\n\n" +
                "Rules: respond in 1–2 short warm sentences max. " +
                "Never say anything scary, violent, or sad. " +
                "End with a question or invitation to explore."
        )
        assertThat(PerspectiveEngine.buildUserMessage(req))
            .isEqualTo("I see: a butterfly (83% sure). Child asks: why does it have spots?")
    }
}
