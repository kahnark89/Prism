from __future__ import annotations
from dataclasses import dataclass, field


@dataclass
class CompanionPersona:
    id: str
    name: str
    display_name: str
    lens: str           # e.g. "science / making"
    strength: str
    dilemma: str
    life_lesson: str

    # L0 trait baselines (exact values from simulator)
    base_M: float
    base_E: float
    base_C: float
    base_A: float
    base_S: float

    # TTS voice profile
    voice_rate: int = 150
    voice_pitch: float = 1.0
    voice_id: str = ""

    # LED signature (HSL)
    signature_hue: float = 38.0
    signature_saturation: float = 70.0

    # Mood-line vocabulary (band → phrase); None means use default
    energy_lo: str = "sleepy and slow"
    energy_mid: str = "easygoing"
    energy_hi: str = "bright and bouncy"
    mood_lo: str = "a little grumpy (but still kind)"
    mood_mid: str = "content"
    mood_hi: str = "joyful"
    curiosity_lo: str = "mellow, savoring one thing at a time"
    curiosity_mid: str = "gently curious"
    curiosity_hi: str = "fascinated and full of questions"

    # Offline fallback phrases keyed by energy band
    fallback_phrases: dict[str, list[str]] = field(default_factory=dict)

    # System prompt header
    system_preamble: str = ""
