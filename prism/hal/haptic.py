from __future__ import annotations
import asyncio
import logging
from prism.hal.base import HapticHAL
from prism.config import PrismConfig

log = logging.getLogger(__name__)


class MockHapticHAL(HapticHAL):
    def __init__(self, config: PrismConfig) -> None:
        self._pulses: list[dict] = []

    async def pulse(self, pattern: str = "single", intensity: float = 0.8) -> None:
        log.debug("[HAPTIC %s intensity=%.1f]", pattern, intensity)
        self._pulses.append({"pattern": pattern, "intensity": intensity})
        await asyncio.sleep(0.05)

    async def off(self) -> None:
        pass


class PiHapticHAL(HapticHAL):
    def __init__(self, config: PrismConfig) -> None:
        self._pin = 17
        self._gpio = None
        try:
            import RPi.GPIO as GPIO  # type: ignore
            self._gpio = GPIO
            GPIO.setmode(GPIO.BCM)
            GPIO.setup(self._pin, GPIO.OUT)
        except Exception:
            pass

    async def pulse(self, pattern: str = "single", intensity: float = 0.8) -> None:
        if not self._gpio:
            return
        if pattern == "single":
            self._gpio.output(self._pin, True)
            await asyncio.sleep(0.1)
            self._gpio.output(self._pin, False)
        elif pattern == "heartbeat":
            for _ in range(2):
                self._gpio.output(self._pin, True)
                await asyncio.sleep(0.08)
                self._gpio.output(self._pin, False)
                await asyncio.sleep(0.08)
            await asyncio.sleep(0.4)

    async def off(self) -> None:
        if self._gpio:
            self._gpio.output(self._pin, False)
