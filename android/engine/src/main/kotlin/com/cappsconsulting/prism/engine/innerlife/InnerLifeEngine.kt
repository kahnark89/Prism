package com.cappsconsulting.prism.engine.innerlife

import com.cappsconsulting.prism.engine.config.PrismConfig
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Direct port of `prism/engines/inner_life.py`.
 *
 * This is the dynamical system the genotype calls "aliveness is local" (Architecture
 * invariant — two brains): five coupled, drifting variables — Mood, Energy, Curiosity,
 * Affection, Shyness — that give the companion its "fast brain"-resident inner life,
 * independent of any cloud connection. Epigenome 025 named this file (with memory,
 * mood_line, grounding, personas, safety, learning_log) as the part of the Python
 * stack that "ports verbatim" — the actual differentiating IP, not a hardware-coupled
 * layer needing redesign. Every constant below is unchanged from the Python original;
 * changing any of them is a tuning decision for the architect, not a porting one.
 */
val VARS: List<String> = listOf("M", "E", "C", "A", "S")

val KAPPA: Map<String, Map<String, Double>> = mapOf(
    "M" to mapOf("E" to 0.10, "C" to 0.05, "A" to 0.04, "S" to 0.00),
    "E" to mapOf("M" to 0.06, "C" to 0.08, "A" to 0.00, "S" to 0.03),
    "C" to mapOf("M" to 0.05, "E" to 0.10, "A" to 0.00, "S" to 0.04),
    "A" to mapOf("M" to 0.03, "E" to 0.00, "C" to 0.02, "S" to 0.03),
    "S" to mapOf("M" to 0.05, "E" to 0.12, "C" to 0.06, "A" to 0.04),
)

val RHYTHM_SHIFT: Map<String, Double> = mapOf(
    "M" to 0.10, "E" to 0.45, "C" to 0.18, "A" to 0.05, "S" to 0.15,
)

val EVENT_KICKS: Map<String, Map<String, Double>> = mapOf(
    "novel" to mapOf("M" to 0.05, "E" to 0.08, "C" to 0.20, "A" to 0.03, "S" to 0.05),
    "delight" to mapOf("M" to 0.15, "E" to 0.10, "C" to 0.05, "A" to 0.08, "S" to 0.08),
    "whim" to mapOf("M" to 0.06, "E" to 0.05, "C" to 0.18, "A" to 0.00, "S" to 0.04),
    "reunion" to mapOf("M" to 0.10, "E" to 0.06, "C" to 0.05, "A" to 0.15, "S" to 0.10),
    "idle" to mapOf("M" to -0.03, "E" to -0.05, "C" to -0.06, "A" to 0.00, "S" to -0.08),
    "repeat" to mapOf("M" to 0.00, "E" to -0.04, "C" to -0.10, "A" to 0.00, "S" to -0.03),
)

val WHIMS: List<String> = listOf(
    "round things", "the color blue", "spirals", "soft things", "shiny things", "tiny things",
)

internal fun clamp01(x: Double): Double = max(0.0, min(1.0, x))

/**
 * Snapshot-friendly mirror of `InnerLifeState` for persistence (Room/DataStore on
 * Android, replacing `inner_life_store.py`'s SQLite JSON blob).
 */
@Serializable
data class InnerLifeSnapshot(
    val m: Double,
    val e: Double,
    val c: Double,
    val a: Double,
    val s: Double,
    val m0: Double,
    val e0: Double,
    val c0: Double,
    val a0: Double,
    val s0: Double,
    val a0Seed: Double,
    val eps: Map<String, Double>,
    val pendingKicks: Map<String, Double>,
    val whim: String,
    val whimDate: String,
    val daysTogether: Double,
    val lastTickTime: Double,
)

/**
 * Mutable engine state. `M0..S0` are the L0 trait baselines — mutable, because
 * affection (`A0`) grows slowly with `daysTogether` (the co-evolution mechanism,
 * Genotype §"organizing principle": the companion is also still becoming).
 */
class InnerLifeState(
    var m: Double,
    var e: Double,
    var c: Double,
    var a: Double,
    var s: Double,
    var m0: Double,
    var e0: Double,
    var c0: Double,
    var a0: Double,
    var s0: Double,
    val a0Seed: Double,
    val eps: MutableMap<String, Double> = VARS.associateWithTo(LinkedHashMap()) { 0.0 },
    val pendingKicks: MutableMap<String, Double> = VARS.associateWithTo(LinkedHashMap()) { 0.0 },
    var whim: String = "round things",
    var whimDate: String = "",
    var daysTogether: Double = 0.0,
    var lastTickTime: Double = 0.0,
) {
    fun snapshot(): InnerLifeSnapshot = InnerLifeSnapshot(
        m = m, e = e, c = c, a = a, s = s,
        m0 = m0, e0 = e0, c0 = c0, a0 = a0, s0 = s0,
        a0Seed = a0Seed,
        eps = eps.toMap(),
        pendingKicks = pendingKicks.toMap(),
        whim = whim,
        whimDate = whimDate,
        daysTogether = daysTogether,
        lastTickTime = lastTickTime,
    )

    /** Read-only view of the five drifting variables, keyed exactly as [VARS]. */
    fun valuesByVar(): Map<String, Double> = mapOf("M" to m, "E" to e, "C" to c, "A" to a, "S" to s)

    private fun baselinesByVar(): Map<String, Double> =
        mapOf("M" to m0, "E" to e0, "C" to c0, "A" to a0, "S" to s0)

    private fun applyVar(key: String, value: Double) {
        when (key) {
            "M" -> m = value
            "E" -> e = value
            "C" -> c = value
            "A" -> a = value
            "S" -> s = value
        }
    }

    internal fun baselineFor(key: String): Double = baselinesByVar().getValue(key)
    internal fun setBaseline(key: String, value: Double) {
        when (key) {
            "M" -> m0 = value
            "E" -> e0 = value
            "C" -> c0 = value
            "A" -> a0 = value
            "S" -> s0 = value
        }
    }
    internal fun setVar(key: String, value: Double) = applyVar(key, value)

    companion object {
        fun fromSnapshot(d: InnerLifeSnapshot): InnerLifeState = InnerLifeState(
            m = d.m, e = d.e, c = d.c, a = d.a, s = d.s,
            m0 = d.m0, e0 = d.e0, c0 = d.c0, a0 = d.a0, s0 = d.s0,
            a0Seed = d.a0Seed,
            eps = d.eps.toMutableMap(),
            pendingKicks = d.pendingKicks.toMutableMap(),
            whim = d.whim,
            whimDate = d.whimDate,
            daysTogether = d.daysTogether,
            lastTickTime = d.lastTickTime,
        )
    }
}

/**
 * The five-variable coupled drift. One [tick] = one step of the lived-in dynamical
 * system; [fireEvent] queues a kick that decays in over subsequent ticks (so events
 * land as a felt shift, not an instant snap — "aliveness over consistency", Genotype
 * Principle 7).
 */
class InnerLifeEngine(
    private val config: PrismConfig,
    baseM: Double,
    baseE: Double,
    baseC: Double,
    baseA: Double,
    baseS: Double,
    private val random: Random = Random.Default,
    private val nowEpochSeconds: () -> Double = { System.currentTimeMillis() / 1000.0 },
) {
    private var state = InnerLifeState(
        m = baseM, e = baseE, c = baseC, a = baseA, s = baseS,
        m0 = baseM, e0 = baseE, c0 = baseC, a0 = baseA, s0 = baseS,
        a0Seed = baseA,
        lastTickTime = nowEpochSeconds(),
    )

    /**
     * Composite circadian curve: morning peak, afternoon shoulder, evening/deep-night
     * troughs. Returns a value in [-1, 1] used to shift each variable's effective
     * baseline through the day — the substrate beneath any single tick's noise/events.
     */
    fun circadian(tHours: Double): Double {
        val morning = exp(-((tHours - 10.0) / 3.0).let { it * it })
        val afternoon = exp(-((tHours - 15.5) / 2.6).let { it * it }) * 0.85
        val night = -exp(-((tHours - 22.0) / 3.2).let { it * it }) * 1.1
        val deepNight = -exp(-((tHours - 2.0) / 4.0).let { it * it }) * 1.0
        return max(-1.0, min(1.0, morning + afternoon + night + deepNight))
    }

    /** Advance one tick. Returns the updated (mutated in place) state. */
    fun tick(now: Double = nowEpochSeconds()): InnerLifeState {
        val cfg = config
        val instant = Instant.fromEpochMilliseconds((now * 1000).toLong())
        val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val tHours = dt.hour + dt.minute / 60.0 + dt.second / 3600.0
        val todayStr = dt.date.toString()

        // Reseed whim at dawn
        if (todayStr != state.whimDate && tHours >= cfg.wakeHour) {
            state.whim = WHIMS.random(random)
            state.whimDate = todayStr
        }

        val r = circadian(tHours) * cfg.rhythmDepth
        val stateDict = state.valuesByVar()

        val newVals = LinkedHashMap<String, Double>(VARS.size)
        for (x in VARS) {
            // Noise update (autocorrelated, clamped)
            var epsNew = cfg.noiseAutocorr * state.eps.getValue(x) +
                (random.nextDouble() - 0.5) * cfg.noiseMagnitude
            epsNew = max(-cfg.noiseMagnitude, min(cfg.noiseMagnitude, epsNew))
            state.eps[x] = epsNew

            // Effective baseline (circadian-shifted)
            val x0Eff = state.baselineFor(x) + RHYTHM_SHIFT.getValue(x) * r

            // Coupling: every other variable pulls this one toward/away from its
            // own midpoint, scaled by the KAPPA matrix — the felt sense that mood,
            // energy, curiosity, affection and shyness aren't independent dials.
            val coupling = VARS.filter { it != x }.sumOf { j ->
                (KAPPA.getValue(x)[j] ?: 0.0) * cfg.couplingScale * (stateDict.getValue(j) - 0.5)
            }

            newVals[x] = clamp01(
                stateDict.getValue(x) +
                    cfg.lambdaHomeostasis * (x0Eff - stateDict.getValue(x)) +
                    coupling +
                    (state.pendingKicks[x] ?: 0.0) +
                    epsNew
            )

            // Decay the pending kick toward zero (events land gradually, not as a snap)
            var kick = (state.pendingKicks[x] ?: 0.0) * cfg.eventKickDecay
            if (kotlin.math.abs(kick) < 0.001) kick = 0.0
            state.pendingKicks[x] = kick
        }

        for (x in VARS) state.setVar(x, newVals.getValue(x))

        // Growth: accumulate days_together; affection baseline grows toward a capped ceiling.
        // This is the co-evolution mechanism made literal — the companion is also still
        // becoming (Genotype "organizing principle").
        if (state.lastTickTime > 0) {
            val elapsedDays = (now - state.lastTickTime) / 86400.0
            state.daysTogether += elapsedDays
            val growth = min(cfg.growthCap, state.daysTogether * cfg.growthRatePerDay)
            state.a0 = clamp01(state.a0Seed + growth)
        }

        state.lastTickTime = now
        return state
    }

    /**
     * Queue an event kick. It does not snap the state immediately — it adds to
     * [InnerLifeState.pendingKicks], which [tick] applies and decays over subsequent
     * ticks, so a "delight" or "reunion" lands as a gradually-settling shift.
     */
    fun fireEvent(eventName: String) {
        val kicks = EVENT_KICKS[eventName]
            ?: throw IllegalArgumentException("Unknown event '$eventName'. Valid: ${EVENT_KICKS.keys}")
        for ((v, k) in kicks) {
            state.pendingKicks[v] = (state.pendingKicks[v] ?: 0.0) + k
        }
    }

    fun getState(): InnerLifeState = state

    fun loadState(loaded: InnerLifeState) {
        state = loaded
    }

    /**
     * Called by the memory engine on consolidation: a small extra nudge to the
     * affection baseline from accumulated bonding (memory mass), capped at
     * `a0Seed + growthCap` — growth has a ceiling, the same as the daily-rate path.
     */
    fun applyAffectionBoost(memoryMass: Double) {
        val boost = memoryMass * 0.005
        state.a0 = min(state.a0Seed + config.growthCap, state.a0 + boost)
    }
}
