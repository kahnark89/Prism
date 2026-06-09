package com.cappsconsulting.prism.engine.moodline

import com.cappsconsulting.prism.engine.config.PrismConfig
import com.cappsconsulting.prism.engine.innerlife.InnerLifeState
import com.cappsconsulting.prism.engine.memory.MemoryNode
import com.cappsconsulting.prism.engine.personas.CompanionPersona

/**
 * Direct port of `prism/engines/mood_line.py`.
 *
 * Compiles the inner-life state into the natural-language "mood line" that
 * conditions the cloud smart-brain's persona voice each turn (Architecture
 * invariant: "companion personas + conversation, conditioned by inner-life
 * state + memory + CIAER context"). This is also, narratively, the glass-box
 * line lived from the *companion's* side — "thought apple, almost said tomato"
 * is the same act as "you feel sleepy and a little slow... you are especially
 * charmed by spirals today": stating an internal, uncertain state out loud.
 */
internal fun band(value: Double, lo: String, mid: String, hi: String): String = when {
    value < 0.34 -> lo
    value < 0.66 -> mid
    else -> hi
}

class MoodLineCompiler(private val config: PrismConfig) {

    fun compile(
        state: InnerLifeState,
        persona: CompanionPersona,
        childName: String,
        topMemories: List<MemoryNode>,
        timeOfDay: Double,
    ): String {
        val cfg = config

        val energyW = band(state.e, persona.energyLo, persona.energyMid, persona.energyHi)
        val moodW = band(state.m, persona.moodLo, persona.moodMid, persona.moodHi)
        val curW = band(state.c, persona.curiosityLo, persona.curiosityMid, persona.curiosityHi)
        val affW = band(
            state.a,
            "friendly toward $childName",
            "warm toward $childName",
            "deeply fond of $childName",
        )

        val timeCtx = timeContext(timeOfDay, cfg)
        val memorySentence = memorySentence(topMemories)

        var moodLine = "You are ${persona.name}. " +
            timeCtx +
            "Right now you feel $energyW and $moodW; you are $curW. " +
            "You feel $affW. " +
            "You are especially charmed by ${state.whim} today."

        if (memorySentence.isNotEmpty()) moodLine += " $memorySentence"

        return moodLine
    }

    companion object {
        internal fun timeContext(t: Double, cfg: PrismConfig): String = when {
            t < cfg.wakeHour -> "It's very early morning, so you're just waking up. "
            t >= cfg.bedHour -> "It's near bedtime, so you're dreamy and soft-spoken. "
            t >= cfg.napHour && t < cfg.napHour + 2 -> "It's afternoon quiet-time, so you're a bit mellow. "
            else -> ""
        }

        /** Public (not just internal-to-orchestrator, like the Python `_memory_sentence`) — the
         * orchestrator needs it for the CIAER `memory_summary` field, exactly as `_mood_line._memory_sentence`
         * was reached into directly in `orchestrator.py`. Exposed properly here instead of reaching
         * into a "private" method — the Kotlin-idiomatic fix for a Python wart noted while porting. */
        fun memorySentence(memories: List<MemoryNode>): String {
            if (memories.isEmpty()) return ""
            val concepts = mutableListOf<String>()
            for (m in memories.take(3)) {
                if (m.concept !in concepts) concepts.add(m.concept)
            }
            return when (concepts.size) {
                1 -> "You remember ${concepts[0]} fondly."
                else -> {
                    val parts = concepts.dropLast(1).joinToString(", ") + " and ${concepts.last()}"
                    "You remember $parts from before."
                }
            }
        }
    }
}
