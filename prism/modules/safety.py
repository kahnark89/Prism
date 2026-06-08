from __future__ import annotations
import re
import logging
from dataclasses import dataclass
from enum import Enum
from prism.config import PrismConfig

log = logging.getLogger(__name__)

# Hard-coded blocked patterns (in addition to config.safety_blocked_patterns)
_HARD_BLOCKED = [
    r'\b(kill|murder|die|dead|blood|weapon|gun|knife|stab|shoot|hurt)\b',
    r'\b(sex|naked|porn|adult)\b',
    r'\b(drug|alcohol|beer|wine|smoke|cigarette)\b',
    r'\b(idiot|stupid|dumb|ugly|fat|loser|hate you)\b',
]

_REDIRECT_PHRASE = "Let's look at something else! What do you see around you?"


class SafetyVerdict(Enum):
    PASS = "pass"
    REDIRECT = "redirect"
    BLOCK = "block"


@dataclass
class SafetyResult:
    verdict: SafetyVerdict
    original: str
    sanitized: str   # empty if BLOCK; redirect phrase if REDIRECT; original if PASS
    reason: str      # for logging only, never shown to child


class SafetyModule:
    def __init__(self, config: PrismConfig) -> None:
        self._cfg = config
        self._patterns = [re.compile(p, re.IGNORECASE) for p in _HARD_BLOCKED]
        for p in config.safety_blocked_patterns:
            self._patterns.append(re.compile(p, re.IGNORECASE))

    def check_input(self, text: str) -> SafetyResult:
        """Applied before text reaches the LLM."""
        return self._run_checks(text, "input")

    def check_output(self, text: str) -> SafetyResult:
        """Applied before text reaches TTS/audio."""
        return self._run_checks(text, "output")

    def _run_checks(self, text: str, direction: str) -> SafetyResult:
        for pattern in self._patterns:
            if pattern.search(text):
                log.warning("[SAFETY %s] BLOCKED pattern=%s", direction, pattern.pattern[:40])
                return SafetyResult(
                    verdict=SafetyVerdict.BLOCK,
                    original=text,
                    sanitized="",
                    reason=f"blocked pattern: {pattern.pattern[:40]}",
                )

        # Topic bounding: at youngest tier, redirect anything that seems off-topic
        # (keep simple for now; age-scaling unlocks more later)
        if len(text) > 500:
            return SafetyResult(
                verdict=SafetyVerdict.REDIRECT,
                original=text,
                sanitized=_REDIRECT_PHRASE,
                reason="response too long for youngest tier",
            )

        return SafetyResult(
            verdict=SafetyVerdict.PASS,
            original=text,
            sanitized=text,
            reason="",
        )
