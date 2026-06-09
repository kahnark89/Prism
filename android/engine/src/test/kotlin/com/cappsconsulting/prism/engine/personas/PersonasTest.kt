package com.cappsconsulting.prism.engine.personas

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Direct port of the lookup behavior in `prism/personas/__init__.py::get_persona`
 * (and the four `pip`/`lumi`/`tale`/`mechanical` modules it serves).
 */
class PersonasTest {

    @Test
    fun `get looks up by id and is case-insensitive, exactly like the python original's name_lower`() {
        assertThat(Personas.get("pip")).isEqualTo(Personas.PIP)
        assertThat(Personas.get("PIP")).isEqualTo(Personas.PIP)
        assertThat(Personas.get("PiP")).isEqualTo(Personas.PIP)
        assertThat(Personas.get("lumi")).isEqualTo(Personas.LUMI)
        assertThat(Personas.get("tale")).isEqualTo(Personas.TALE)
        assertThat(Personas.get("mechanical")).isEqualTo(Personas.MECHANICAL)
    }

    @Test
    fun `get raises on an unknown persona id, naming the bad id and listing the valid ones`() {
        val ex = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            Personas.get("nonexistent")
        }
        assertThat(ex.message).contains("Unknown persona 'nonexistent'")
        assertThat(ex.message).contains("pip")
        assertThat(ex.message).contains("mechanical")
    }

    @Test
    fun `all returns the four personas with distinct ids and non-empty trait baselines`() {
        val all = Personas.all()
        assertThat(all).hasSize(4)
        assertThat(all.map { it.id }).containsExactly("pip", "lumi", "tale", "mechanical")

        for (p in all) {
            for (trait in listOf(p.baseM, p.baseE, p.baseC, p.baseA, p.baseS)) {
                assertThat(trait).isAtLeast(0.0)
                assertThat(trait).isAtMost(1.0)
            }
        }
    }

    @Test
    fun `mechanical is the pre-awakening voice — factual lens, neutral baselines, no life lesson`() {
        val mechanical = Personas.MECHANICAL
        assertThat(mechanical.name).isEqualTo("Prism")
        assertThat(mechanical.lens).isEqualTo("factual")
        assertThat(mechanical.lifeLesson).isEmpty()
        assertThat(mechanical.dilemma).isEmpty()
        // Perfectly centered baselines — the "mechanical", not-yet-awakened persona has no lean.
        assertThat(listOf(mechanical.baseM, mechanical.baseE, mechanical.baseC, mechanical.baseA, mechanical.baseS))
            .containsExactly(0.50, 0.50, 0.50, 0.50, 0.50)
    }

    @Test
    fun `each awakened persona carries its own lens, life lesson, and banded fallback vocabulary`() {
        for (p in listOf(Personas.PIP, Personas.LUMI, Personas.TALE)) {
            assertThat(p.lifeLesson).isNotEmpty()
            assertThat(p.systemPreamble).contains(p.name)
            assertThat(p.fallbackPhrases.keys).containsAtLeast("lo", "mid", "hi")
            for ((_, phrases) in p.fallbackPhrases) {
                assertThat(phrases).isNotEmpty()
            }
        }
    }
}
