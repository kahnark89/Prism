package com.cappsconsulting.prism.engine.grounding

import com.cappsconsulting.prism.engine.config.PrismConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The OGC hard rule — "confidence only accumulates on LEARNING EVIDENCE... engagement
 * alone NEVER increments confidence" — is the structural form of Hard Line 10
 * ("optimize the teaching, never the engagement"). These tests exist to keep that
 * rule provable, not just stated: [GroundingAccumulator.recordExposure] alone must
 * never move [GroundingRecord.confidence].
 */
class GroundingAccumulatorTest {

    private val cfg = PrismConfig()

    @Test
    fun `mere exposure never moves confidence — only reappearance with evidence does`() {
        val g = GroundingAccumulator(cfg)
        repeat(50) { g.recordExposure("spiral", nowEpochSeconds = 1000.0 + it) }

        val rec = g.getRecord("spiral")
        assertThat(rec).isNotNull()
        assertThat(rec!!.confidence).isEqualTo(0.0)
        assertThat(rec.status).isEqualTo(GroundingStatus.EXPLORING)
    }

    @Test
    fun `same-context reappearance is treated as an echo, not evidence — near-zero gain`() {
        val g = GroundingAccumulator(cfg)
        g.recordExposure("spiral")
        g.recordReappearance("spiral", newContext = "snail shell")
        val afterFirst = g.getRecord("spiral")!!.confidence

        g.recordReappearance("spiral", newContext = "snail shell") // identical context — an echo
        val afterEcho = g.getRecord("spiral")!!.confidence

        assertThat(afterEcho - afterFirst).isWithin(1e-9).of(0.02)
    }

    @Test
    fun `genuinely novel-context reappearance counts as real transfer — substantial gain`() {
        val g = GroundingAccumulator(cfg)
        g.recordReappearance("spiral", newContext = "snail shell")
        val afterFirst = g.getRecord("spiral")!!.confidence

        g.recordReappearance("spiral", newContext = "spinning top") // genuinely new context
        val afterNovel = g.getRecord("spiral")!!.confidence

        assertThat(afterNovel - afterFirst).isWithin(1e-9).of(0.35)
    }

    @Test
    fun `model revision is strong evidence regardless of context distance`() {
        val g = GroundingAccumulator(cfg)
        g.recordReappearance("spiral", newContext = "snail shell") // first reappearance: 0.2
        val before = g.getRecord("spiral")!!.confidence

        // Same context again, but this time she revised her own model — that's strong
        // evidence even though the *context* alone would only earn an echo (0.02).
        g.recordReappearance("spiral", newContext = "snail shell", hasModelRevision = true)
        val after = g.getRecord("spiral")!!.confidence

        assertThat(after - before).isWithin(1e-9).of(0.5)
    }

    @Test
    fun `confidence is monotonic, capped at one, and classifies into the three plain-language bands`() {
        val g = GroundingAccumulator(cfg)
        // exploring
        g.recordReappearance("idea", newContext = "ctx-a")
        assertThat(g.getRecord("idea")!!.status).isEqualTo(GroundingStatus.EXPLORING)
        assertThat(g.getRecord("idea")!!.confidence).isLessThan(0.3)

        // getting_it — push past 0.3 with a genuinely new context
        g.recordReappearance("idea", newContext = "ctx-b")
        assertThat(g.getRecord("idea")!!.confidence).isAtLeast(0.3)
        assertThat(g.getRecord("idea")!!.status).isEqualTo(GroundingStatus.GETTING_IT)

        // owns_it — push past 0.7
        g.recordReappearance("idea", newContext = "ctx-c", hasModelRevision = true)
        assertThat(g.getRecord("idea")!!.confidence).isAtLeast(0.7)
        assertThat(g.getRecord("idea")!!.status).isEqualTo(GroundingStatus.OWNS_IT)

        // never exceeds 1.0 however much evidence keeps arriving
        repeat(20) { g.recordReappearance("idea", newContext = "ctx-$it", hasModelRevision = true) }
        assertThat(g.getRecord("idea")!!.confidence).isAtMost(1.0)
    }

    @Test
    fun `parent confirmation is a strong deliberate boost and is counted`() {
        val g = GroundingAccumulator(cfg)
        g.parentConfirm("spiral")
        val rec = g.getRecord("spiral")!!
        assertThat(rec.parentConfirmations).isEqualTo(1)
        assertThat(rec.confidence).isWithin(1e-9).of(0.4)
    }

    @Test
    fun `snapshot round-trips status and history faithfully`() {
        val g = GroundingAccumulator(cfg)
        g.recordReappearance("spiral", newContext = "snail shell")
        g.recordReappearance("spiral", newContext = "spinning top")
        g.parentConfirm("spiral")

        val restored = GroundingAccumulator(cfg)
        restored.loadSnapshot(g.getSnapshot())

        val original = g.getRecord("spiral")!!
        val copy = restored.getRecord("spiral")!!
        assertThat(copy.confidence).isEqualTo(original.confidence)
        assertThat(copy.status).isEqualTo(original.status)
        assertThat(copy.seenContexts).isEqualTo(original.seenContexts)
        assertThat(copy.parentConfirmations).isEqualTo(original.parentConfirmations)
    }
}
