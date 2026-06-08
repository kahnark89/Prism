package com.capps.prism.engine

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Translation of prism/engines/inner_life.py (the reference spec — Epigenome
 * 025: this engine "ports verbatim," nothing in it is hardware-specific).
 * It is the dynamical system behind the companion's half of co-evolution:
 * five drifting axes, nudged by a circadian rhythm, cross-coupled, kicked by
 * named events, and slowly growing a trait baseline (A0) the longer the
 * companion and child are together.
 *
 * Kept dict-keyed by the same single-letter axis names as the Python spec
 * (rather than e.g. an enum) so the coupling/shift/kick tables below can be
 * checked line-for-line against prism/engines/inner_life.py during review.
 */

/** Mood, Energy, Curiosity, Affection, Sociability — the five drifting axes. */
val VARS: List<String> = listOf("M", "E", "C", "A", "S")

/** Cross-axis coupling: KAPPA[x][j] pulls axis x toward axis j's deviation from 0.5. */
val KAPPA: Map<String, Map<String, Double>> = mapOf(
    "M" to mapOf("E" to 0.10, "C" to 0.05, "A" to 0.04, "S" to 0.00),
    "E" to mapOf("M" to 0.06, "C" to 0.08, "A" to 0.00, "S" to 0.03),
    "C" to mapOf("M" to 0.05, "E" to 0.10, "A" to 0.00, "S" to 0.04),
    "A" to mapOf("M" to 0.03, "E" to 0.00, "C" to 0.02, "S" to 0.03),
    "S" to mapOf("M" to 0.05, "E" to 0.12, "C" to 0.06, "A" to 0.04),
)

/** How far each axis's effective baseline shifts with the circadian rhythm R. */
val RHYTHM_SHIFT: Map<String, Double> = mapOf(
    "M" to 0.10, "E" to 0.45, "C" to 0.18, "A" to 0.05, "S" to 0.15,
)

/** Per-axis one-shot nudges fired by named events; each tick they decay by `eventKickDecay`. */
val EVENT_KICKS: Map<String, Map<String, Double>> = mapOf(
    "novel"   to mapOf("M" to  0.05, "E" to  0.08, "C" to  0.20, "A" to  0.03, "S" to  0.05),
    "delight" to mapOf("M" to  0.15, "E" to  0.10, "C" to  0.05, "A" to  0.08, "S" to  0.08),
    "whim"    to mapOf("M" to  0.06, "E" to  0.05, "C" to  0.18, "A" to  0.00, "S" to  0.04),
    "reunion" to mapOf("M" to  0.10, "E" to  0.06, "C" to  0.05, "A" to  0.15, "S" to  0.10),
    "idle"    to mapOf("M" to -0.03, "E" to -0.05, "C" to -0.06, "A" to  0.00, "S" to -0.08),
    "repeat"  to mapOf("M" to  0.00, "E" to -0.04, "C" to -0.10, "A" to  0.00, "S" to -0.03),
)

val WHIMS: List<String> = listOf(
    "round things", "the color blue", "spirals", "soft things", "shiny things", "tiny things",
)

fun clamp01(x: Double): Double = max(0.0, min(1.0, x))

/**
 * The inner-life tuning knobs this engine reads, with defaults matching
 * prism/config.py's PrismConfig. Scoped to this engine rather than mirrored
 * as one monolithic config object — Python's PrismConfig mixes hardware/LLM/
 * dashboard concerns that have no place in a pure-logic shared-engine module
 * (Doc 3.0 §2). Each ported engine takes the slice it needs; whether/how to
 * unify them is a later call, once more engines are ported and the seams
 * between their knobs are visible.
 */
data class InnerLifeConfig(
    val lambdaHomeostasis: Double = 0.08,
    val noiseMagnitude: Double = 0.02,
    val noiseAutocorr: Double = 0.8,
    val rhythmDepth: Double = 1.0,
    val couplingScale: Double = 1.0,
    val eventKickDecay: Double = 0.6,
    val growthRatePerDay: Double = 0.012,
    val growthCap: Double = 0.18,
    val wakeHour: Double = 7.0,
)

private fun zeroedAxisMap(): MutableMap<String, Double> =
    VARS.associateWithTo(LinkedHashMap()) { 0.0 }

/**
 * Mutable snapshot of the dynamical system: the five live axes (M/E/C/A/S),
 * their L0 trait baselines — which themselves slowly grow via `A0` (the
 * "affection growth" half of co-evolution, Doc 2.1) — per-axis OU noise
 * (`eps`), pending event kicks, and the day's whim. `snapshot`/`fromSnapshot`
 * round-trip this exact shape for persistence, field names matching the
 * Python spec's so stored state is legible against it.
 *
 * Direct port of InnerLifeState (prism/engines/inner_life.py) — including
 * which fields mutate in place (the engine advances this object tick over
 * tick rather than replacing it).
 */
data class InnerLifeState(
    var M: Double,
    var E: Double,
    var C: Double,
    var A: Double,
    var S: Double,
    var M0: Double,
    var E0: Double,
    var C0: Double,
    var A0: Double,
    var S0: Double,
    val a0Seed: Double,
    val eps: MutableMap<String, Double> = zeroedAxisMap(),
    val pendingKicks: MutableMap<String, Double> = zeroedAxisMap(),
    var whim: String = "round things",
    var whimDate: String = "",
    var daysTogether: Double = 0.0,
    var lastTickTime: Double = 0.0,
) {
    fun snapshot(): Map<String, Any> = mapOf(
        "M" to M, "E" to E, "C" to C, "A" to A, "S" to S,
        "M0" to M0, "E0" to E0, "C0" to C0, "A0" to A0, "S0" to S0,
        "A0_seed" to a0Seed,
        "eps" to eps.toMap(),
        "pending_kicks" to pendingKicks.toMap(),
        "whim" to whim,
        "whim_date" to whimDate,
        "days_together" to daysTogether,
        "last_tick_time" to lastTickTime,
    )

    companion object {
        /** Inverse of `snapshot()` — same key names (snake_case) so stored rows need no translation layer. */
        fun fromSnapshot(d: Map<String, Any?>): InnerLifeState {
            fun num(key: String): Double = (d.getValue(key) as Number).toDouble()
            fun numOrDefault(key: String, default: Double): Double =
                (d[key] as? Number)?.toDouble() ?: default

            @Suppress("UNCHECKED_CAST")
            fun axisMapOrZeroed(key: String): MutableMap<String, Double> {
                val raw = d[key] as? Map<String, Any?>
                return VARS.associateWithTo(LinkedHashMap()) { axis ->
                    (raw?.get(axis) as? Number)?.toDouble() ?: 0.0
                }
            }

            return InnerLifeState(
                M = num("M"), E = num("E"), C = num("C"), A = num("A"), S = num("S"),
                M0 = num("M0"), E0 = num("E0"), C0 = num("C0"), A0 = num("A0"), S0 = num("S0"),
                a0Seed = numOrDefault("A0_seed", num("A0")),
                eps = axisMapOrZeroed("eps"),
                pendingKicks = axisMapOrZeroed("pending_kicks"),
                whim = (d["whim"] as? String) ?: "round things",
                whimDate = (d["whim_date"] as? String) ?: "",
                daysTogether = numOrDefault("days_together", 0.0),
                lastTickTime = numOrDefault("last_tick_time", System.currentTimeMillis() / 1000.0),
            )
        }
    }
}

/**
 * Advances one [InnerLifeState] forward in discrete ticks: homeostatic pull
 * toward a circadian-shifted baseline, cross-axis coupling, decaying event
 * kicks, and bounded autocorrelated noise — plus the slow A0 growth that is
 * this companion's half of the co-evolution mechanism (Doc 2.1).
 *
 * Direct translation of InnerLifeEngine (prism/engines/inner_life.py).
 */
class InnerLifeEngine(
    private val config: InnerLifeConfig,
    baseM: Double,
    baseE: Double,
    baseC: Double,
    baseA: Double,
    baseS: Double,
) {
    private var state = InnerLifeState(
        M = baseM, E = baseE, C = baseC, A = baseA, S = baseS,
        M0 = baseM, E0 = baseE, C0 = baseC, A0 = baseA, S0 = baseS,
        a0Seed = baseA,
        lastTickTime = nowEpochSeconds(),
    )

    /** Advance one tick at `now` (epoch seconds; defaults to current time). Mutates and returns the live state. */
    fun tick(now: Double = nowEpochSeconds()): InnerLifeState {
        val s = state
        val cfg = config

        val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli((now * 1000.0).toLong()), ZoneId.systemDefault())
        val tHours = dt.hour + dt.minute / 60.0 + dt.second / 3600.0
        val todayStr = dt.toLocalDate().toString() // ISO-8601 yyyy-MM-dd, matches Python's date().isoformat()

        // Reseed whim at dawn
        if (todayStr != s.whimDate && tHours >= cfg.wakeHour) {
            s.whim = WHIMS.random()
            s.whimDate = todayStr
        }

        val r = circadian(tHours) * cfg.rhythmDepth

        val stateDict = mapOf("M" to s.M, "E" to s.E, "C" to s.C, "A" to s.A, "S" to s.S)
        val baseDict = mapOf("M" to s.M0, "E" to s.E0, "C" to s.C0, "A" to s.A0, "S" to s.S0)

        val newVals = LinkedHashMap<String, Double>()
        for (x in VARS) {
            // Noise update (bounded Ornstein-Uhlenbeck-ish autocorrelated noise)
            var epsNew = cfg.noiseAutocorr * s.eps.getValue(x) + (Random.nextDouble() - 0.5) * cfg.noiseMagnitude
            epsNew = max(-cfg.noiseMagnitude, min(cfg.noiseMagnitude, epsNew))
            s.eps[x] = epsNew

            // Effective baseline (circadian-shifted)
            val x0Eff = baseDict.getValue(x) + RHYTHM_SHIFT.getValue(x) * r

            // Coupling — each other axis's deviation from the neutral midpoint pulls this one
            val coupling = VARS.filter { it != x }.sumOf { j ->
                (KAPPA.getValue(x)[j] ?: 0.0) * cfg.couplingScale * (stateDict.getValue(j) - 0.5)
            }

            newVals[x] = clamp01(
                stateDict.getValue(x) +
                    cfg.lambdaHomeostasis * (x0Eff - stateDict.getValue(x)) +
                    coupling +
                    (s.pendingKicks[x] ?: 0.0) +
                    epsNew
            )

            // Decay this axis's pending kick
            var kick = (s.pendingKicks[x] ?: 0.0) * cfg.eventKickDecay
            if (abs(kick) < 0.001) kick = 0.0
            s.pendingKicks[x] = kick
        }

        s.M = newVals.getValue("M")
        s.E = newVals.getValue("E")
        s.C = newVals.getValue("C")
        s.A = newVals.getValue("A")
        s.S = newVals.getValue("S")

        // Growth: accumulate days_together, apply affection growth toward the cap
        if (s.lastTickTime > 0) {
            val elapsedDays = (now - s.lastTickTime) / 86400.0
            s.daysTogether += elapsedDays
            val growth = min(cfg.growthCap, s.daysTogether * cfg.growthRatePerDay)
            s.A0 = clamp01(s.a0Seed + growth)
        }

        s.lastTickTime = now
        return s
    }

    /** Queue a named event's per-axis kicks onto pendingKicks (additive — concurrent events stack). */
    fun fireEvent(eventName: String) {
        val kicks = EVENT_KICKS[eventName]
            ?: throw IllegalArgumentException("Unknown event '$eventName'. Valid: ${EVENT_KICKS.keys}")
        for ((axis, kick) in kicks) {
            state.pendingKicks[axis] = (state.pendingKicks[axis] ?: 0.0) + kick
        }
    }

    fun getState(): InnerLifeState = state

    fun loadState(newState: InnerLifeState) {
        state = newState
    }

    /** Called by the memory engine on consolidation — a small extra A0 nudge from bonding (the co-evolution coupling, Doc 2.1). */
    fun applyAffectionBoost(memoryMass: Double) {
        val boost = memoryMass * 0.005
        state.A0 = min(state.a0Seed + config.growthCap, state.A0 + boost)
    }

    companion object {
        /**
         * Composite circadian curve at hour-of-day `tHours`: a morning peak,
         * a smaller afternoon peak, and two night dips (evening + small
         * hours), summed and clamped to [-1, 1]. A `staticmethod` in the
         * Python spec — kept here as a companion function so callers (and
         * the ported test) can reach it the same way: `InnerLifeEngine.circadian(...)`.
         */
        fun circadian(tHours: Double): Double {
            val morning = exp(-square((tHours - 10.0) / 3.0))
            val afternoon = exp(-square((tHours - 15.5) / 2.6)) * 0.85
            val night = -exp(-square((tHours - 22.0) / 3.2)) * 1.1
            val deepNight = -exp(-square((tHours - 2.0) / 4.0)) * 1.0
            return max(-1.0, min(1.0, morning + afternoon + night + deepNight))
        }

        private fun square(x: Double): Double = x * x
    }
}

private fun nowEpochSeconds(): Double = System.currentTimeMillis() / 1000.0
