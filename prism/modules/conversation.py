from __future__ import annotations
import logging
from dataclasses import dataclass
from typing import Optional
from prism.config import PrismConfig
from prism.hal.base import MicHAL

log = logging.getLogger(__name__)


@dataclass
class ConversationResult:
    transcript: str
    confidence: float
    is_question: bool


class ConversationModule:
    """
    STT + dialogue state. Online only.
    Offline: disabled entirely (returns None).
    """

    def __init__(self, config: PrismConfig, mic: MicHAL) -> None:
        self._cfg = config
        self._mic = mic
        self._client = None
        self._init_client()

    def _init_client(self) -> None:
        try:
            import anthropic  # type: ignore
            self._client = anthropic.AsyncAnthropic()
        except ImportError:
            pass

    async def listen_for_question(self, is_online: bool) -> Optional[ConversationResult]:
        """Record mic audio and transcribe. Returns None if offline or no speech."""
        if not is_online:
            return None
        audio = await self._mic.record_utterance(max_seconds=8.0)
        if not audio:
            return None
        return await self._transcribe(audio)

    async def _transcribe(self, audio: bytes) -> Optional[ConversationResult]:
        # Placeholder: real implementation uses Whisper API or on-device VAD
        # For now return mock transcript
        return ConversationResult(
            transcript="What is this?",
            confidence=0.9,
            is_question=True,
        )
