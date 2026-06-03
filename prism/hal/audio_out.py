from __future__ import annotations
import asyncio
import logging
from prism.hal.base import SpeakerHAL
from prism.config import PrismConfig

log = logging.getLogger(__name__)


class MockSpeakerHAL(SpeakerHAL):
    def __init__(self, config: PrismConfig) -> None:
        self._log: list[dict] = []

    async def speak(self, text: str, rate: int = 150, volume: float = 1.0) -> None:
        log.info("[SPEAK rate=%d vol=%.1f] %s", rate, volume, text)
        self._log.append({"type": "speak", "text": text, "rate": rate})
        await asyncio.sleep(0.05)

    async def play_tone(self, frequency: float, duration_s: float, volume: float = 0.5) -> None:
        log.info("[TONE %.1fHz %.2fs]", frequency, duration_s)
        await asyncio.sleep(duration_s)

    async def play_sound(self, sound_id: str) -> None:
        log.info("[SOUND %s]", sound_id)
        await asyncio.sleep(0.1)

    @property
    def spoken_lines(self) -> list[str]:
        return [e["text"] for e in self._log if e["type"] == "speak"]


class PiSpeakerHAL(SpeakerHAL):
    def __init__(self, config: PrismConfig) -> None:
        self._engine = None
        try:
            import pyttsx3  # type: ignore
            self._engine = pyttsx3.init()
        except Exception:
            pass

    async def speak(self, text: str, rate: int = 150, volume: float = 1.0) -> None:
        if not self._engine:
            log.info("[SPEAK] %s", text)
            return
        loop = asyncio.get_event_loop()
        def _speak():
            self._engine.setProperty("rate", rate)
            self._engine.setProperty("volume", volume)
            self._engine.say(text)
            self._engine.runAndWait()
        await loop.run_in_executor(None, _speak)

    async def play_tone(self, frequency: float, duration_s: float, volume: float = 0.5) -> None:
        await asyncio.sleep(duration_s)

    async def play_sound(self, sound_id: str) -> None:
        await asyncio.sleep(0.1)
