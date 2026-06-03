from __future__ import annotations
import asyncio
import logging
from prism.hal.base import LedHAL, LedState
from prism.config import PrismConfig

log = logging.getLogger(__name__)


class MockLedHAL(LedHAL):
    def __init__(self, config: PrismConfig) -> None:
        self.current_state: LedState | None = None

    async def set_state(self, state: LedState) -> None:
        self.current_state = state
        log.debug("[LED hue=%.0f sat=%.0f lit=%.0f pulse=%.2fs pat=%s]",
                  state.hue, state.saturation, state.lightness,
                  state.pulse_speed, state.pattern)

    async def off(self) -> None:
        self.current_state = None
        log.debug("[LED off]")


class PiNeoPixelLedHAL(LedHAL):
    def __init__(self, config: PrismConfig) -> None:
        self._pixels = None
        try:
            import board  # type: ignore
            import neopixel  # type: ignore
            self._pixels = neopixel.NeoPixel(board.D18, 12, brightness=0.5, auto_write=False)
        except Exception:
            pass

    def _hsl_to_rgb(self, h: float, s: float, l: float) -> tuple[int, int, int]:
        import colorsys
        r, g, b = colorsys.hls_to_rgb(h / 360.0, l / 100.0, s / 100.0)
        return int(r * 255), int(g * 255), int(b * 255)

    async def set_state(self, state: LedState) -> None:
        if not self._pixels:
            return
        rgb = self._hsl_to_rgb(state.hue, state.saturation, state.lightness)
        for i in range(len(self._pixels)):
            self._pixels[i] = rgb
        self._pixels.show()

    async def off(self) -> None:
        if self._pixels:
            self._pixels.fill((0, 0, 0))
            self._pixels.show()
