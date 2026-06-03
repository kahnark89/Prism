from __future__ import annotations
import logging
from dataclasses import dataclass
from typing import Optional
from prism.config import PrismConfig
from prism.hal.base import LedHAL, HapticHAL, LedState
from prism.engines.inner_life import InnerLifeState
from prism.personas.base import CompanionPersona

log = logging.getLogger(__name__)


@dataclass
class UIMode:
    MECHANICAL = "mechanical"
    AWAKENED = "awakened"
    THINKING = "thinking"
    SLEEPING = "sleeping"


class UIController:
    def __init__(self, config: PrismConfig, led: LedHAL, haptic: HapticHAL) -> None:
        self._cfg = config
        self._led = led
        self._haptic = haptic

    async def update_from_state(self, state: InnerLifeState,
                                persona: CompanionPersona, mode: str) -> None:
        led_state = self._compile_led(state, persona, mode)
        await self._led.set_state(led_state)

    async def thinking_cue(self) -> None:
        """Fast pulse while awaiting LLM."""
        import asyncio
        for _ in range(3):
            await self._led.set_state(LedState(hue=200, saturation=80, lightness=70,
                                                pulse_speed=0.2, pattern="solid"))
            await asyncio.sleep(0.2)
            await self._led.set_state(LedState(hue=200, saturation=40, lightness=30,
                                                pulse_speed=0.2, pattern="solid"))
            await asyncio.sleep(0.2)

    async def awakening_bloom(self) -> None:
        """LED bloom sequence for awakening beat 3."""
        import asyncio
        for brightness in [30, 50, 70, 90, 100, 80, 60]:
            await self._led.set_state(LedState(hue=45, saturation=90,
                                                lightness=float(brightness),
                                                pulse_speed=0.3, pattern="solid"))
            await asyncio.sleep(0.15)

    async def heartbeat_pulse(self) -> None:
        await self._haptic.pulse(pattern="heartbeat", intensity=0.7)

    @staticmethod
    def _compile_led(state: InnerLifeState, persona: CompanionPersona, mode: str) -> LedState:
        """
        Faithful port of inner_life_simulator.jsx ledStyle():
        if E < 0.35: blue-ish (sleepy)
        else: amber→gold from M (awake)
        pulse_speed from E: 0.6 + E * 1.8 seconds
        """
        if mode == UIMode.SLEEPING:
            return LedState(hue=205, saturation=30, lightness=15, pulse_speed=3.0, pattern="breathe")

        pulse_speed = 0.6 + state.E * 1.8

        if state.E < 0.35:
            hue = 205.0
            sat = 55.0
            lit = 45.0 + state.M * 12.0
        else:
            hue = persona.signature_hue + state.M * 14.0
            sat = persona.signature_saturation + state.M * 20.0
            lit = 42.0 + state.M * 20.0

        if mode == UIMode.THINKING:
            sat = min(100.0, sat + 20.0)

        return LedState(
            hue=hue,
            saturation=min(100.0, sat),
            lightness=min(100.0, lit),
            pulse_speed=pulse_speed,
            pattern="breathe",
        )
