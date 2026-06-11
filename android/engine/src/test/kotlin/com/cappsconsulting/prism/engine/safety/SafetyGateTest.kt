package com.cappsconsulting.prism.engine.safety

import com.cappsconsulting.prism.engine.config.PrismConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Direct port of the gate behavior in `prism/modules/safety.py` — the layered
 * "input gate -> ... -> output gate -> topic bounding" model from the Genotype's
 * non-negotiable safety section. [SafetyResult.reason] is logging-only and must
 * never surface to the child; these tests check the *verdicts* and *sanitized*
 * text the UI is allowed to see, not the reason strings.
 */
class SafetyGateTest {

    private val cfg = PrismConfig()
    private val gate = SafetyGate(cfg)

    @Test
    fun `safe everyday text passes through untouched`() {
        val result = gate.checkInput("I saw a snail on the path today!")
        assertThat(result.verdict).isEqualTo(SafetyVerdict.PASS)
        assertThat(result.sanitized).isEqualTo("I saw a snail on the path today!")
    }

    @Test
    fun `hard-blocked vocabulary is blocked on input and yields no sanitized text`() {
        val result = gate.checkInput("I want to talk about a gun")
        assertThat(result.verdict).isEqualTo(SafetyVerdict.BLOCK)
        assertThat(result.sanitized).isEmpty()
    }

    @Test
    fun `hard-blocked vocabulary is blocked on output identically to input`() {
        val result = gate.checkOutput("That looks like a knife.")
        assertThat(result.verdict).isEqualTo(SafetyVerdict.BLOCK)
        assertThat(result.sanitized).isEmpty()
    }

    @Test
    fun `each blocked category is caught -- violence, adult content, substances, and unkindness`() {
        assertThat(gate.checkInput("don't be such an idiot").verdict).isEqualTo(SafetyVerdict.BLOCK)
        assertThat(gate.checkInput("let's talk about beer").verdict).isEqualTo(SafetyVerdict.BLOCK)
        assertThat(gate.checkInput("that movie was about a murder").verdict).isEqualTo(SafetyVerdict.BLOCK)
    }

    @Test
    fun `blocked matching is whole-word and case-insensitive, not a substring scan`() {
        // "class" contains "ass" but must not trip the adult-content pattern — \b word boundaries matter.
        assertThat(gate.checkInput("Let's go to class now.").verdict).isEqualTo(SafetyVerdict.PASS)
        // Case-insensitivity: same pattern, different casing.
        assertThat(gate.checkInput("DON'T BE SUCH AN IDIOT").verdict).isEqualTo(SafetyVerdict.BLOCK)
    }

    @Test
    fun `overly long text is redirected with the fixed phrase rather than blocked`() {
        val longText = "a".repeat(501)
        val result = gate.checkOutput(longText)
        assertThat(result.verdict).isEqualTo(SafetyVerdict.REDIRECT)
        assertThat(result.sanitized).isEqualTo("Let's look at something else! What do you see around you?")
    }

    @Test
    fun `text at exactly the length boundary still passes`() {
        val exactlyAtLimit = "a".repeat(500)
        assertThat(gate.checkInput(exactlyAtLimit).verdict).isEqualTo(SafetyVerdict.PASS)
    }

    @Test
    fun `config-supplied patterns extend the hard-blocked vocabulary`() {
        val withExtra = SafetyGate(cfg.copy(safetyBlockedPatterns = listOf("\\bspider\\b")))
        assertThat(withExtra.checkInput("I saw a spider in the garden").verdict).isEqualTo(SafetyVerdict.BLOCK)
        // The base gate, without that pattern, still passes the same text.
        assertThat(gate.checkInput("I saw a spider in the garden").verdict).isEqualTo(SafetyVerdict.PASS)
    }

    @Test
    fun `reason is populated for blocks and redirects but empty on pass -- and never echoed into sanitized`() {
        val blocked = gate.checkInput("a knife")
        assertThat(blocked.reason).isNotEmpty()
        assertThat(blocked.sanitized).doesNotContain(blocked.reason)

        val passed = gate.checkInput("a snail")
        assertThat(passed.reason).isEmpty()
    }
}
