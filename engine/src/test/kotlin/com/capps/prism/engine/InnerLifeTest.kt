package com.capps.prism.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

/**
 * Direct port of tests/test_inner_life.py — same engine, same behavioral
 * claims, same construction (Pip's baselines: M=0.62 E=0.60 C=0.80 A=0.55
 * S=0.62 — prism/personas/pip.py). Kept one-to-one with the Python suite so
 * "does the port behave like the spec" stays a single, checkable question.
 */
class InnerLifeTest {

    private fun makeEngine(config: InnerLifeConfig = InnerLifeConfig()): InnerLifeEngine =
        InnerLifeEngine(config, baseM = 0.62, baseE = 0.60, baseC = 0.80, baseA = 0.55, baseS = 0.62)

    @Test
    fun `state stays in bounds`() {
        val eng = makeEngine()
        repeat(500) {
            val state = eng.tick()
            for (v in VARS) {
                val value = axisValue(state, v)
                assertTrue(value in 0.0..1.0, "$v=$value out of bounds")
            }
        }
    }

    @Test
    fun `homeostasis converges`() {
        // With no events and no noise, state converges toward baseline.
        val config = InnerLifeConfig(noiseMagnitude = 0.0, eventKickDecay = 0.0)
        val eng = makeEngine(config)
        // Push state far from baseline
        eng.getState().M = 0.0
        eng.getState().E = 0.0
        repeat(200) { eng.tick() }
        val state = eng.getState()
        assertTrue(state.M > 0.3, "M should converge toward baseline, got ${state.M}")
        assertTrue(state.E > 0.3, "E should converge toward baseline, got ${state.E}")
    }

    @Test
    fun `coupling energy drags sociability`() {
        // High E should pull S upward via coupling (KAPPA[S][E] = 0.12).
        val config = InnerLifeConfig(noiseMagnitude = 0.0)
        val eng = makeEngine(config)
        eng.getState().E = 1.0
        eng.getState().S = 0.0
        repeat(50) { eng.tick() }
        val state = eng.getState()
        assertTrue(state.S > 0.1, "High E should drag S up via coupling, got S=${state.S}")
    }

    @Test
    fun `circadian shape`() {
        assertTrue(InnerLifeEngine.circadian(10.0) > 0.5, "Morning peak should be > 0.5")
        assertTrue(InnerLifeEngine.circadian(22.0) < 0.0, "Night dip should be negative")
        assertTrue(InnerLifeEngine.circadian(15.5) > 0.0, "Afternoon peak should be positive")
    }

    @Test
    fun `event kick fires and decays`() {
        val config = InnerLifeConfig(noiseMagnitude = 0.0)
        val eng = makeEngine(config)
        val beforeC = eng.getState().C
        eng.fireEvent("novel")
        val state = eng.tick()
        assertTrue(state.C > beforeC, "Novel event should boost Curiosity")
        // After many ticks with no more events, the kick decays toward zero
        repeat(20) { eng.tick() }
        assertTrue(
            abs(eng.getState().pendingKicks.getValue("C")) <= 0.01,
            "pending C kick should have decayed to ~0, got ${eng.getState().pendingKicks.getValue("C")}",
        )
    }

    @Test
    fun `noise bounded`() {
        val eng = makeEngine()
        val mag = InnerLifeConfig().noiseMagnitude
        repeat(1000) {
            eng.tick()
            for (v in VARS) {
                val eps = eng.getState().eps.getValue(v)
                assertTrue(abs(eps) <= mag + 1e-9, "Noise $v=$eps exceeds bound ±$mag")
            }
        }
    }

    @Test
    fun `affection growth cap`() {
        val eng = makeEngine()
        val seed = eng.getState().a0Seed
        val cap = InnerLifeConfig().growthCap
        // Force many days
        eng.getState().daysTogether = 1000.0
        repeat(10) { eng.tick() }
        val a0 = eng.getState().A0
        assertTrue(a0 <= seed + cap + 0.001, "A0=$a0 exceeds cap of ${seed + cap}")
    }

    @Test
    fun `unknown event raises`() {
        val eng = makeEngine()
        assertThrows(IllegalArgumentException::class.java) { eng.fireEvent("does_not_exist") }
    }

    @Test
    fun `state snapshot roundtrip`() {
        val eng = makeEngine()
        eng.fireEvent("delight")
        repeat(5) { eng.tick() }
        val snap = eng.getState().snapshot()
        val restored = InnerLifeState.fromSnapshot(snap)
        assertEquals(eng.getState().M, restored.M, 1e-9)
        assertEquals(eng.getState().A0, restored.A0, 1e-9)
    }

    private fun axisValue(state: InnerLifeState, axis: String): Double = when (axis) {
        "M" -> state.M
        "E" -> state.E
        "C" -> state.C
        "A" -> state.A
        "S" -> state.S
        else -> error("unknown axis $axis")
    }
}
