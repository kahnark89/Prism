from __future__ import annotations
import time
import pytest
from prism.engines.inner_life import InnerLifeEngine, VARS, _clamp01


def make_engine(config):
    return InnerLifeEngine(config, base_M=0.62, base_E=0.60, base_C=0.80, base_A=0.55, base_S=0.62)


def test_state_stays_in_bounds(config):
    eng = make_engine(config)
    for _ in range(500):
        state = eng.tick()
        for v in VARS:
            val = getattr(state, v)
            assert 0.0 <= val <= 1.0, f"{v}={val} out of bounds"


def test_homeostasis_converges(config):
    """With no events and no noise (noise_magnitude=0), state converges toward baseline."""
    config.noise_magnitude = 0.0
    config.event_kick_decay = 0.0
    eng = make_engine(config)
    # Push state far from baseline
    eng.get_state().M = 0.0
    eng.get_state().E = 0.0
    for _ in range(200):
        eng.tick()
    state = eng.get_state()
    assert state.M > 0.3, f"M should converge toward baseline, got {state.M}"
    assert state.E > 0.3, f"E should converge toward baseline, got {state.E}"


def test_coupling_energy_drags_sociability(config):
    """High E should pull S upward via coupling (KAPPA[S][E] = 0.12)."""
    config.noise_magnitude = 0.0
    eng = make_engine(config)
    eng.get_state().E = 1.0
    eng.get_state().S = 0.0
    for _ in range(50):
        eng.tick()
    state = eng.get_state()
    assert state.S > 0.1, f"High E should drag S up via coupling, got S={state.S}"


def test_circadian_shape():
    """R(10) should be a local peak, R(22) should be negative."""
    assert InnerLifeEngine.circadian(10.0) > 0.5, "Morning peak should be > 0.5"
    assert InnerLifeEngine.circadian(22.0) < 0.0, "Night dip should be negative"
    assert InnerLifeEngine.circadian(15.5) > 0.0, "Afternoon peak should be positive"


def test_event_kick_fires_and_decays(config):
    config.noise_magnitude = 0.0
    eng = make_engine(config)
    before_C = eng.get_state().C
    eng.fire_event('novel')
    state = eng.tick()
    assert state.C > before_C, "Novel event should boost Curiosity"
    # After many ticks with no more events, kick decays
    for _ in range(20):
        state = eng.tick()
    assert eng.get_state().pending_kicks.get('C', 0.0) == pytest.approx(0.0, abs=0.01)


def test_noise_bounded(config):
    eng = make_engine(config)
    mag = config.noise_magnitude
    for _ in range(1000):
        eng.tick()
        for v in VARS:
            eps = eng.get_state().eps[v]
            assert abs(eps) <= mag + 1e-9, f"Noise {v}={eps} exceeds bound ±{mag}"


def test_affection_growth_cap(config):
    eng = make_engine(config)
    seed = eng.get_state().A0_seed
    cap = config.growth_cap
    # Force many days
    eng.get_state().days_together = 1000.0
    for _ in range(10):
        eng.tick()
    a0 = eng.get_state().A0
    assert a0 <= seed + cap + 0.001, f"A0={a0} exceeds cap of {seed + cap}"


def test_unknown_event_raises(config):
    eng = make_engine(config)
    with pytest.raises(ValueError):
        eng.fire_event('does_not_exist')


def test_state_snapshot_roundtrip(config):
    eng = make_engine(config)
    eng.fire_event('delight')
    for _ in range(5):
        eng.tick()
    snap = eng.get_state().snapshot()
    restored = eng.get_state().from_snapshot(snap)
    assert abs(restored.M - eng.get_state().M) < 1e-9
    assert abs(restored.A0 - eng.get_state().A0) < 1e-9
