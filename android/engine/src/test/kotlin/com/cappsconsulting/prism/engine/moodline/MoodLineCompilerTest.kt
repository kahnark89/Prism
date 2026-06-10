package com.cappsconsulting.prism.engine.moodline

import com.cappsconsulting.prism.engine.config.PrismConfig
import com.cappsconsulting.prism.engine.innerlife.InnerLifeState
import com.cappsconsulting.prism.engine.memory.MemoryEngine
import com.cappsconsulting.prism.engine.personas.Personas
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MoodLineCompilerTest {

    private val cfg = PrismConfig()
    private val compiler = MoodLineCompiler(cfg)

    private fun stateOf(m: Double, e: Double, c: Double, a: Double, s: Double, whim: String = "spirals") =
        InnerLifeState(m = m, e = e, c = c, a = a, s = s, m0 = m, e0 = e, c0 = c, a0 = a, s0 = s, a0Seed = a, whim = whim)

    @Test
    fun `compile narrates band language only -- never a number, anywhere (Hard Line 6)`() {
        val state = stateOf(m = 0.8, e = 0.2, c = 0.9, a = 0.5, s = 0.5)
        val line = compiler.compile(state, Personas.PIP, "Naomi", emptyList(), timeOfDay = 12.0)

        // The whole point of Hard Line 6: no digits anywhere in what the companion says
        // about its own state (the banded vocabulary is the *only* legal representation).
        assertThat(line).doesNotContainMatch("[0-9]")
        assertThat(line).contains("Pip")
        assertThat(line).contains("Naomi")
        assertThat(line).contains("spirals")
    }

    @Test
    fun `band selection follows the lo-mid-hi thresholds exactly`() {
        assertThat(band(0.10, "lo", "mid", "hi")).isEqualTo("lo")
        assertThat(band(0.33, "lo", "mid", "hi")).isEqualTo("lo")
        assertThat(band(0.34, "lo", "mid", "hi")).isEqualTo("mid")
        assertThat(band(0.65, "lo", "mid", "hi")).isEqualTo("mid")
        assertThat(band(0.66, "lo", "mid", "hi")).isEqualTo("hi")
        assertThat(band(1.00, "lo", "mid", "hi")).isEqualTo("hi")
    }

    @Test
    fun `time context names early-morning, bedtime, and quiet-time windows`() {
        assertThat(MoodLineCompiler.timeContext(5.0, cfg)).contains("just waking up")
        assertThat(MoodLineCompiler.timeContext(20.0, cfg)).contains("bedtime")
        assertThat(MoodLineCompiler.timeContext(13.5, cfg)).contains("quiet-time")
        assertThat(MoodLineCompiler.timeContext(11.0, cfg)).isEmpty()
    }

    @Test
    fun `memory sentence names one, two, or several remembered concepts in natural prose`() {
        assertThat(MoodLineCompiler.memorySentence(emptyList())).isEmpty()

        val mem = MemoryEngine(cfg)
        mem.encode("snail", "on the path", salience = 0.9)
        val one = mem.getTopActivated(k = 1)
        assertThat(MoodLineCompiler.memorySentence(one)).isEqualTo("You remember snail fondly.")

        mem.encode("spiral", "seashell", salience = 0.9)
        mem.encode("apple", "in the bowl", salience = 0.9)
        val many = listOf(mem.getAllNodes()[0], mem.getAllNodes()[1], mem.getAllNodes()[2])
        assertThat(MoodLineCompiler.memorySentence(many))
            .isEqualTo("You remember snail, spiral and apple from before.")
    }

    @Test
    fun `compile appends the memory sentence only when memories exist`() {
        val state = stateOf(m = 0.5, e = 0.5, c = 0.5, a = 0.5, s = 0.5)
        val withoutMemories = compiler.compile(state, Personas.LUMI, "Naomi", emptyList(), 12.0)
        assertThat(withoutMemories).doesNotContain("You remember")

        val mem = MemoryEngine(cfg)
        mem.encode("snail", "on the path", salience = 0.9)
        val withMemories = compiler.compile(state, Personas.LUMI, "Naomi", mem.getTopActivated(), 12.0)
        assertThat(withMemories).contains("You remember snail fondly.")
    }
}
