package com.cappsconsulting.prism.engine.learninglog

import com.cappsconsulting.prism.engine.config.PrismConfig
import com.cappsconsulting.prism.engine.grounding.GroundingAccumulator
import com.cappsconsulting.prism.engine.grounding.GroundingStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Direct port of the call-site behavior in `prism/modules/learning_log.py::LearningLog`.
 *
 * [LearningLog] is the single enforcement point for the OGC hard rule (see
 * [GroundingAccumulator] kdoc and [GroundingAccumulatorTest]) — these tests verify
 * that *this* call site honors the contract: exposure always, but reappearance
 * (the only thing that can move confidence) fires only when [CIAERRecord] carries
 * actual learning evidence — a `modelRevision`, or a non-trivial `result`.
 */
class LearningLogTest {

    private val cfg = PrismConfig()

    private fun recordOf(
        cause: String = "spiral",
        action: String = "pointed at the shell",
        result: String = "",
        modelRevision: String? = null,
    ) = CIAERRecord(
        timestamp = 1000.0,
        cause = cause,
        action = action,
        result = result,
        modelRevision = modelRevision,
    )

    @Test
    fun `every recorded event is appended and persisted, queryable by recency and by concept`() {
        val persisted = mutableListOf<CIAERRecord>()
        val log = LearningLog(GroundingAccumulator(cfg), onPersist = { persisted.add(it) })

        log.recordEvent(recordOf(cause = "spiral"))
        log.recordEvent(recordOf(cause = "snail"))

        assertThat(log.getAll()).hasSize(2)
        assertThat(log.getRecent(1).single().cause).isEqualTo("snail")
        assertThat(log.getByConcept("spiral")).hasSize(1)
        assertThat(persisted).hasSize(2)
    }

    @Test
    fun `engagement-only event opens exposure but never increments confidence — the OGC hard rule`() {
        val grounding = GroundingAccumulator(cfg)
        val log = LearningLog(grounding)

        // No model revision, and "result" is empty — pure engagement, no learning evidence.
        log.recordEvent(recordOf(cause = "spiral", result = ""))

        val rec = grounding.getRecord("spiral")
        assertThat(rec).isNotNull() // exposure WAS recorded — the record exists...
        assertThat(rec!!.confidence).isEqualTo(0.0) // ...but confidence did not move.
        assertThat(rec.status).isEqualTo(GroundingStatus.EXPLORING)
    }

    @Test
    fun `'none' and 'no_change' results are explicitly treated as non-evidence, just like empty`() {
        val grounding = GroundingAccumulator(cfg)
        val log = LearningLog(grounding)

        log.recordEvent(recordOf(cause = "spiral", result = "none"))
        log.recordEvent(recordOf(cause = "spiral", result = "no_change"))

        val rec = grounding.getRecord("spiral")!!
        assertThat(rec.confidence).isEqualTo(0.0)
        assertThat(rec.status).isEqualTo(GroundingStatus.EXPLORING)
    }

    @Test
    fun `a non-trivial result counts as learning evidence and triggers reappearance`() {
        val grounding = GroundingAccumulator(cfg)
        val log = LearningLog(grounding)

        log.recordEvent(recordOf(cause = "spiral", action = "snail shell", result = "named_the_shape"))

        val rec = grounding.getRecord("spiral")!!
        assertThat(rec.confidence).isGreaterThan(0.0)
        assertThat(rec.seenContexts).contains("snail shell")
    }

    @Test
    fun `a model revision counts as strong learning evidence even with a trivial result`() {
        val grounding = GroundingAccumulator(cfg)
        val log = LearningLog(grounding)

        log.recordEvent(recordOf(cause = "spiral", result = "none", modelRevision = "thought it was a snake; it's a shell"))

        val rec = grounding.getRecord("spiral")!!
        assertThat(rec.confidence).isGreaterThan(0.0)
    }

    @Test
    fun `events with no cause never touch grounding at all`() {
        val grounding = GroundingAccumulator(cfg)
        val log = LearningLog(grounding)

        log.recordEvent(recordOf(cause = "", result = "named_the_shape", modelRevision = "revised a model"))

        assertThat(grounding.getAllRecords()).isEmpty()
    }
}
