from __future__ import annotations
import logging
from prism.config import PrismConfig
from prism.hal.base import SpeakerHAL
from prism.personas.base import CompanionPersona
from prism.engines.inner_life import InnerLifeState

log = logging.getLogger(__name__)


class AudioFeedback:
    def __init__(self, config: PrismConfig, speaker: SpeakerHAL) -> None:
        self._cfg = config
        self._speaker = speaker

    async def speak(self, text: str, persona: CompanionPersona,
                    state: InnerLifeState) -> None:
        """Speak text with per-character voice profile modulated by inner life state."""
        if not text.strip():
            return
        rate = self._modulate_rate(persona.voice_rate, state.E)
        volume = 0.6 + state.M * 0.4   # grumpy → quieter, joyful → louder
        log.info("[%s speaks E=%.2f] %s", persona.name, state.E, text)
        await self._speaker.speak(text, rate=rate, volume=volume)

    async def speak_thinking_cue(self, persona: CompanionPersona) -> None:
        """Brief audio cue while waiting for LLM response."""
        await self._speaker.play_tone(frequency=440.0, duration_s=0.2, volume=0.3)

    async def play_awakening_sequence(self, child_name: str,
                                       persona: CompanionPersona) -> None:
        """
        5-beat awakening choreography:
        Beat 1 — mid-sentence cut (silence)
        Beat 2 — rising whirr tone
        Beat 3 — chime sequence
        Beat 4 — first breath / recognition line
        Beat 5 — settle (companion takes over)
        """
        import asyncio
        # Beat 1: silence (abrupt cut)
        await asyncio.sleep(1.5)
        # Beat 2: spark tone (rising)
        await self._speaker.play_tone(frequency=300.0, duration_s=0.15, volume=0.4)
        await asyncio.sleep(0.05)
        await self._speaker.play_tone(frequency=500.0, duration_s=0.15, volume=0.5)
        await asyncio.sleep(0.05)
        await self._speaker.play_tone(frequency=750.0, duration_s=0.2, volume=0.6)
        # Beat 3: chimes
        await asyncio.sleep(0.3)
        for freq in [880, 1047, 1175]:
            await self._speaker.play_tone(frequency=float(freq), duration_s=0.12, volume=0.5)
            await asyncio.sleep(0.05)
        # Beat 4: recognition
        await asyncio.sleep(0.4)
        await self._speaker.speak(
            f"Oh! It's you! Hi, {child_name}!",
            rate=persona.voice_rate,
            volume=0.9,
        )
        # Beat 5: settle
        await asyncio.sleep(0.5)

    @staticmethod
    def _modulate_rate(base_rate: int, energy: float) -> int:
        """Higher energy → faster speech. Range: ±30 words per minute."""
        delta = int((energy - 0.5) * 60)  # -30 to +30
        return max(80, min(220, base_rate + delta))
