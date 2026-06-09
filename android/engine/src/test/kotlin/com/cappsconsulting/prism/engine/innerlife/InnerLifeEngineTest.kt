package com.cappsconsulting.prism.engine.innerlife

import com.cappsconsulting.prism.engine.config.PrismConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

private const val DAY_SECONDS = 86_400.0
// 2026-06-08 12:00:00 UTC — a stable, mid-day instant so circadian-driven assertions
// (which key off local wall-clock hour) land predictably regardless of CI timezone bias
// near midnight. Any fixed instant works; this one just keeps `t_hours` away from edges.
private const val NOON_EPOCH = 1_780_920_000.0

class InnerLifeEngineTest {

    private fun engine(seed: Long = 42L, cfg: PrismConfig = PrismConfig()) = InnerLifeEngine(
        config = cfg,
        baseM = 0.6, baseE = 0.6, baseC = 0.6, baseA = 0.6, baseS = 0.6,
        random = Random(seed),
        nowEpochSeconds = { NOON_EPOCH },
    )

    @Test
    fun `tick keeps every variable inside the unit interval`() {
        val e = engine()
        var now = NOON_EPOCH
        repeat(500) {
            now += 60.0
            val s = e.tick(now)
            for (v in VARS) {
                val value = s.valuesByVar().getValue(v)
                assertThat(value).isAtLeast(0.0)
                assertThat(value).isAtMost(1.0)
            }
        }
    }

    @Test
    fun `circadian curve peaks near mid-morning and troughs at night`() {
        val e = engine()
        val morning = e.circadian(10.0)
        val deepNight = e.circadian(2.0)
        val noon = e.circadian(12.0)

        assertThat(morning).isGreaterThan(noon)
        assertThat(morning).isGreaterThan(deepNight)
        assertThat(deepNight).isLessThan(0.0)
        assertThat(morning).isAtMost(1.0)
        assertThat(deepNight).isAtLeast(-1.0)
    }

    @Test
    fun `fireEvent queues a kick that nudges state over subsequent ticks rather than snapping it`() {
        val cfg = PrismConfig(noiseMagnitude = 0.0) // isolate the kick from noise
        val e = InnerLifeEngine(
            config = cfg, baseM = 0.5, baseE = 0.5, baseC = 0.5, baseA = 0.5, baseS = 0.5,
            random = Random(7), nowEpochSeconds = { NOON_EPOCH },
        )
        val before = e.getState().valuesByVar().toMap()
        e.fireEvent("delight")

        // Immediately after firing, state hasn't moved yet — the kick is *pending*.
        assertThat(e.getState().valuesByVar()).isEqualTo(before)

        val afterFirstTick = e.tick(NOON_EPOCH + 1.0).valuesByVar().toMap()
        // 'delight' kicks every variable positively (see EVENT_KICKS) — all should rise.
        for (v in VARS) {
            assertThat(afterFirstTick.getValue(v)).isGreaterThan(before.getValue(v))
        }

        // The kick decays geometrically — it should keep contributing (decreasingly)
        // across several ticks rather than vanishing after one, i.e. a felt shift,
        // not an instant snap (Genotype Principle 7, "aliveness over consistency").
        var lastPendingC = e.getState().pendingKicks.getValue("C")
        assertThat(lastPendingC).isGreaterThan(0.0)
        repeat(5) {
            e.tick(NOON_EPOCH + 1.0 + it)
            val pending = e.getState().pendingKicks.getValue("C")
            assertThat(pending).isAtMost(lastPendingC)
            lastPendingC = pending
        }
    }

    @Test
    fun `fireEvent rejects unknown event names exactly like the python original`() {
        val e = engine()
        val ex = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            e.fireEvent("does-not-exist")
        }
        assertThat(ex.message).contains("Unknown event")
    }

    @Test
    fun `affection baseline grows toward the cap as days_together accumulates, never beyond it`() {
        val cfg = PrismConfig(growthRatePerDay = 0.012, growthCap = 0.18)
        val e = InnerLifeEngine(
            config = cfg, baseM = 0.5, baseE = 0.5, baseC = 0.5, baseA = 0.40, baseS = 0.5,
            random = Random(3), nowEpochSeconds = { NOON_EPOCH },
        )
        val seedA0 = e.getState().a0

        // Simulate ~400 days passing tick by tick (one tick per day) — far past the
        // point where growth would exceed the cap if it weren't clamped.
        var now = NOON_EPOCH
        var lastA0 = seedA0
        repeat(400) {
            now += DAY_SECONDS
            val s = e.tick(now)
            assertThat(s.a0).isAtLeast(lastA0 - 1e-9) // monotonically non-decreasing
            assertThat(s.a0).isAtMost(seedA0 + cfg.growthCap + 1e-9)
            lastA0 = s.a0
        }
        // After ~400 days the growth should have saturated at the cap.
        assertThat(lastA0).isWithin(1e-6).of(seedA0 + cfg.growthCap)
    }

    @Test
    fun `applyAffectionBoost nudges baseline up but respects the same growth cap`() {
        val cfg = PrismConfig(growthCap = 0.18)
        val e = InnerLifeEngine(
            config = cfg, baseM = 0.5, baseE = 0.5, baseC = 0.5, baseA = 0.40, baseS = 0.5,
            random = Random(11), nowEpochSeconds = { NOON_EPOCH },
        )
        val seedA0 = e.getState().a0Seed
        repeat(1000) { e.applyAffectionBoost(memoryMass = 1.0) }
        assertThat(e.getState().a0).isAtMost(seedA0 + cfg.growthCap + 1e-9)
        assertThat(e.getState().a0).isGreaterThan(seedA0) // it did move
    }

    @Test
    fun `snapshot round-trips through fromSnapshot without losing state`() {
        val e = engine()
        e.fireEvent("novel")
        val ticked = e.tick(NOON_EPOCH + 5.0)
        val snap = ticked.snapshot()

        val restored = InnerLifeState.fromSnapshot(snap)
        assertThat(restored.valuesByVar()).isEqualTo(ticked.valuesByVar())
        assertThat(restored.whim).isEqualTo(ticked.whim)
        assertThat(restored.daysTogether).isEqualTo(ticked.daysTogether)
        assertThat(restored.pendingKicks).isEqualTo(ticked.pendingKicks)
    }
}
