from __future__ import annotations
import math
import random
import time
from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional
from prism.config import PrismConfig

VARS = ['M', 'E', 'C', 'A', 'S']

KAPPA: dict[str, dict[str, float]] = {
    'M': {'E': 0.10, 'C': 0.05, 'A': 0.04, 'S': 0.00},
    'E': {'M': 0.06, 'C': 0.08, 'A': 0.00, 'S': 0.03},
    'C': {'M': 0.05, 'E': 0.10, 'A': 0.00, 'S': 0.04},
    'A': {'M': 0.03, 'E': 0.00, 'C': 0.02, 'S': 0.03},
    'S': {'M': 0.05, 'E': 0.12, 'C': 0.06, 'A': 0.04},
}

RHYTHM_SHIFT: dict[str, float] = {
    'M': 0.10, 'E': 0.45, 'C': 0.18, 'A': 0.05, 'S': 0.15,
}

EVENT_KICKS: dict[str, dict[str, float]] = {
    'novel':   {'M':  0.05, 'E':  0.08, 'C':  0.20, 'A':  0.03, 'S':  0.05},
    'delight': {'M':  0.15, 'E':  0.10, 'C':  0.05, 'A':  0.08, 'S':  0.08},
    'whim':    {'M':  0.06, 'E':  0.05, 'C':  0.18, 'A':  0.00, 'S':  0.04},
    'reunion': {'M':  0.10, 'E':  0.06, 'C':  0.05, 'A':  0.15, 'S':  0.10},
    'idle':    {'M': -0.03, 'E': -0.05, 'C': -0.06, 'A':  0.00, 'S': -0.08},
    'repeat':  {'M':  0.00, 'E': -0.04, 'C': -0.10, 'A':  0.00, 'S': -0.03},
}

WHIMS = ['round things', 'the color blue', 'spirals', 'soft things', 'shiny things', 'tiny things']


def _clamp01(x: float) -> float:
    return max(0.0, min(1.0, x))


@dataclass
class InnerLifeState:
    M: float
    E: float
    C: float
    A: float
    S: float
    M0: float   # L0 trait baselines (mutable: growth)
    E0: float
    C0: float
    A0: float
    S0: float
    A0_seed: float     # original A0 at birth, used for growth cap
    eps: dict[str, float] = field(default_factory=lambda: {v: 0.0 for v in ['M', 'E', 'C', 'A', 'S']})
    pending_kicks: dict[str, float] = field(default_factory=lambda: {v: 0.0 for v in ['M', 'E', 'C', 'A', 'S']})
    whim: str = 'round things'
    whim_date: str = ''
    days_together: float = 0.0
    last_tick_time: float = 0.0

    def snapshot(self) -> dict:
        return {
            'M': self.M, 'E': self.E, 'C': self.C, 'A': self.A, 'S': self.S,
            'M0': self.M0, 'E0': self.E0, 'C0': self.C0, 'A0': self.A0, 'S0': self.S0,
            'A0_seed': self.A0_seed,
            'eps': dict(self.eps),
            'pending_kicks': dict(self.pending_kicks),
            'whim': self.whim,
            'whim_date': self.whim_date,
            'days_together': self.days_together,
            'last_tick_time': self.last_tick_time,
        }

    @classmethod
    def from_snapshot(cls, d: dict) -> 'InnerLifeState':
        return cls(
            M=d['M'], E=d['E'], C=d['C'], A=d['A'], S=d['S'],
            M0=d['M0'], E0=d['E0'], C0=d['C0'], A0=d['A0'], S0=d['S0'],
            A0_seed=d.get('A0_seed', d['A0']),
            eps=d.get('eps', {v: 0.0 for v in VARS}),
            pending_kicks=d.get('pending_kicks', {v: 0.0 for v in VARS}),
            whim=d.get('whim', 'round things'),
            whim_date=d.get('whim_date', ''),
            days_together=d.get('days_together', 0.0),
            last_tick_time=d.get('last_tick_time', time.time()),
        )


class InnerLifeEngine:
    def __init__(self, config: PrismConfig, base_M: float, base_E: float,
                 base_C: float, base_A: float, base_S: float) -> None:
        self._cfg = config
        self._state = InnerLifeState(
            M=base_M, E=base_E, C=base_C, A=base_A, S=base_S,
            M0=base_M, E0=base_E, C0=base_C, A0=base_A, S0=base_S,
            A0_seed=base_A,
            last_tick_time=time.time(),
        )

    @staticmethod
    def circadian(t_hours: float) -> float:
        morning    = math.exp(-((t_hours - 10.0) / 3.0) ** 2)
        afternoon  = math.exp(-((t_hours - 15.5) / 2.6) ** 2) * 0.85
        night      = -math.exp(-((t_hours - 22.0) / 3.2) ** 2) * 1.1
        deep_night = -math.exp(-((t_hours - 2.0)  / 4.0) ** 2) * 1.0
        return max(-1.0, min(1.0, morning + afternoon + night + deep_night))

    def tick(self, now: Optional[float] = None) -> InnerLifeState:
        """Advance one tick. Returns updated state."""
        if now is None:
            now = time.time()
        state = self._state
        cfg = self._cfg

        # Wall-clock hour for circadian
        dt = datetime.fromtimestamp(now)
        t_hours = dt.hour + dt.minute / 60.0 + dt.second / 3600.0
        today_str = dt.date().isoformat()

        # Reseed whim at dawn
        if today_str != state.whim_date and t_hours >= cfg.wake_hour:
            state.whim = random.choice(WHIMS)
            state.whim_date = today_str

        R = self.circadian(t_hours) * cfg.rhythm_depth

        state_dict = {'M': state.M, 'E': state.E, 'C': state.C, 'A': state.A, 'S': state.S}
        base_dict  = {'M': state.M0, 'E': state.E0, 'C': state.C0, 'A': state.A0, 'S': state.S0}

        new_vals: dict[str, float] = {}
        for x in VARS:
            # Noise update
            eps_new = cfg.noise_autocorr * state.eps[x] + (random.random() - 0.5) * cfg.noise_magnitude
            eps_new = max(-cfg.noise_magnitude, min(cfg.noise_magnitude, eps_new))
            state.eps[x] = eps_new

            # Effective baseline (circadian shifted)
            x0_eff = base_dict[x] + RHYTHM_SHIFT[x] * R

            # Coupling
            coupling = sum(
                KAPPA[x].get(j, 0.0) * cfg.coupling_scale * (state_dict[j] - 0.5)
                for j in VARS if j != x
            )

            # Update
            new_vals[x] = _clamp01(
                state_dict[x]
                + cfg.lambda_homeostasis * (x0_eff - state_dict[x])
                + coupling
                + state.pending_kicks.get(x, 0.0)
                + eps_new
            )

            # Decay kick
            state.pending_kicks[x] = state.pending_kicks.get(x, 0.0) * cfg.event_kick_decay
            if abs(state.pending_kicks[x]) < 0.001:
                state.pending_kicks[x] = 0.0

        state.M, state.E, state.C, state.A, state.S = (
            new_vals['M'], new_vals['E'], new_vals['C'], new_vals['A'], new_vals['S']
        )

        # Growth: accumulate days_together
        if state.last_tick_time > 0:
            elapsed_days = (now - state.last_tick_time) / 86400.0
            state.days_together += elapsed_days
            # Apply affection growth
            growth = min(cfg.growth_cap, state.days_together * cfg.growth_rate_per_day)
            state.A0 = _clamp01(state.A0_seed + growth)

        state.last_tick_time = now
        return state

    def fire_event(self, event_name: str) -> None:
        kicks = EVENT_KICKS.get(event_name)
        if kicks is None:
            raise ValueError(f"Unknown event '{event_name}'. Valid: {list(EVENT_KICKS)}")
        for v, k in kicks.items():
            self._state.pending_kicks[v] = self._state.pending_kicks.get(v, 0.0) + k

    def get_state(self) -> InnerLifeState:
        return self._state

    def load_state(self, state: InnerLifeState) -> None:
        self._state = state

    def apply_affection_boost(self, memory_mass: float) -> None:
        """Called by memory engine consolidation. Small extra A0 nudge from bonding."""
        boost = memory_mass * 0.005
        self._state.A0 = min(
            self._state.A0_seed + self._cfg.growth_cap,
            self._state.A0 + boost
        )
