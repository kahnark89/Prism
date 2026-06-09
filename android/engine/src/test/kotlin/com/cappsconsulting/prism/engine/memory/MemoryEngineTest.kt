package com.cappsconsulting.prism.engine.memory

import com.cappsconsulting.prism.engine.config.PrismConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MemoryEngineTest {

    private val cfg = PrismConfig()

    @Test
    fun `encode below the salience gate is discarded — beats, not a transcript`() {
        val m = MemoryEngine(cfg)
        val node = m.encode(concept = "puddle", episode = "stepped in a puddle", salience = 0.1)
        assertThat(node).isNull()
        assertThat(m.getAllNodes()).isEmpty()
        assertThat(m.getCodebook()).isEmpty()
    }

    @Test
    fun `encode above the gate creates a node and a codebook entry`() {
        val m = MemoryEngine(cfg)
        val node = m.encode(concept = "snail", episode = "snail on the path", salience = 0.8)
        assertThat(node).isNotNull()
        assertThat(m.getAllNodes()).hasSize(1)
        assertThat(m.getCodebook()).containsKey("snail")
        assertThat(m.getCodebook().getValue("snail").count).isEqualTo(1)
    }

    @Test
    fun `re-encoding the same concept runs spreading activation on the existing node`() {
        val m = MemoryEngine(cfg)
        m.encode(concept = "snail", episode = "on the path", salience = 0.8)
        val first = m.getAllNodes().single()
        val sBefore = first.s
        val tauBefore = first.tau

        m.encode(concept = "snail", episode = "in the garden", salience = 0.8)

        // The original node should have strengthened and slowed its decay...
        assertThat(first.s).isGreaterThan(sBefore)
        assertThat(first.tau).isGreaterThan(tauBefore)
        assertThat(first.contexts).contains("in the garden")
        // ...and a *second* episodic node now also exists (spreading activation
        // strengthens what's already known AND records the new instance).
        assertThat(m.getAllNodes()).hasSize(2)
        assertThat(m.getCodebook().getValue("snail").count).isEqualTo(2)
    }

    @Test
    fun `advanceTime decays strength exponentially and prunes below the floor — discard by default`() {
        val m = MemoryEngine(cfg)
        m.setDay(0.0)
        val node = m.encode(concept = "spiral", episode = "seashell spiral", salience = 1.0)!!
        val initialStrength = node.s
        val tau = node.tau

        // Advance by exactly one tau: s should fall to ~ s0 * e^-1.
        val pruned1 = m.advanceTime(tau)
        assertThat(pruned1).isEmpty()
        assertThat(node.s).isWithin(1e-6).of(initialStrength * kotlin.math.exp(-1.0))

        // Advance far enough that s drops below the prune floor.
        val prunedLater = m.advanceTime(tau * 50)
        assertThat(prunedLater).containsExactly(node.id)
        assertThat(m.getAllNodes()).isEmpty()
    }

    @Test
    fun `getTopActivated biases toward the concept hint via spreading activation`() {
        val m = MemoryEngine(cfg)
        m.setDay(0.0)
        m.encode(concept = "apple", episode = "red apple", salience = 0.5)
        m.encode(concept = "banana", episode = "yellow banana", salience = 0.9)

        val noHint = m.getTopActivated(k = 1)
        assertThat(noHint.single().concept).isEqualTo("banana") // higher raw strength wins

        val withHint = m.getTopActivated(conceptHint = "apple", k = 1)
        assertThat(withHint.single().concept).isEqualTo("apple") // hint bias flips the ranking
    }

    @Test
    fun `getContextsForConcept collects distinct contexts in first-seen order`() {
        val m = MemoryEngine(cfg)
        m.encode(concept = "snail", episode = "on the path", salience = 0.8)
        m.encode(concept = "snail", episode = "in the garden", salience = 0.8)
        m.encode(concept = "snail", episode = "on the path", salience = 0.8) // duplicate

        assertThat(m.getContextsForConcept("snail")).containsExactly("on the path", "in the garden").inOrder()
    }

    @Test
    fun `affection mass is zero with no memories and bounded at one with many`() {
        val empty = MemoryEngine(cfg)
        assertThat(empty.getAffectionMass()).isEqualTo(0.0)

        val full = MemoryEngine(cfg)
        repeat(20) { full.encode(concept = "thing$it", episode = "ep$it", salience = 1.0) }
        assertThat(full.getAffectionMass()).isAtMost(1.0)
        assertThat(full.getAffectionMass()).isGreaterThan(0.0)
    }

    @Test
    fun `snapshot round-trips nodes and codebook`() {
        val m = MemoryEngine(cfg)
        m.setDay(2.0)
        m.encode(concept = "spiral", episode = "seashell", salience = 0.9)
        val (nodes, codebook) = m.getSnapshot()

        val restored = MemoryEngine(cfg)
        restored.loadSnapshot(nodes, codebook)

        assertThat(restored.getAllNodes().map { it.concept }).isEqualTo(m.getAllNodes().map { it.concept })
        assertThat(restored.getCodebook().keys).isEqualTo(m.getCodebook().keys)
    }
}
