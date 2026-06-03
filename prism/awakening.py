from __future__ import annotations
import asyncio
import logging
from enum import Enum
from prism.config import PrismConfig
from prism.event_bus import EventBus, AWAKENING_TRIGGERED, AWAKENING_COMPLETE

log = logging.getLogger(__name__)


class AwakeningState(Enum):
    MECHANICAL = "mechanical"
    AWAKENING = "awakening"
    AWAKENED = "awakened"


class AwakeningMachine:
    """
    Tracks whether the companion has awakened to this child.
    Awakening happens once per device reset: when the device recognizes
    the enrolled child and the parent steps back.

    The 5-beat choreography coordinates LED, haptic, and audio via callbacks
    rather than direct imports, so UIController and AudioFeedback don't need
    to be imported here.
    """

    def __init__(self, config: PrismConfig, event_bus: EventBus) -> None:
        self._cfg = config
        self._bus = event_bus
        self._state = AwakeningState.MECHANICAL

    @property
    def state(self) -> AwakeningState:
        return self._state

    @property
    def is_awakened(self) -> bool:
        return self._state == AwakeningState.AWAKENED

    @property
    def is_mechanical(self) -> bool:
        return self._state == AwakeningState.MECHANICAL

    async def check_and_trigger(
        self,
        is_enrolled_child: bool,
        recognition_confidence: float,
    ) -> AwakeningState:
        if self._state != AwakeningState.MECHANICAL:
            return self._state
        if not is_enrolled_child or recognition_confidence < 0.85:
            return self._state

        log.info("Awakening triggered (recognition confidence=%.2f)", recognition_confidence)
        self._state = AwakeningState.AWAKENING
        await self._bus.publish_async(AWAKENING_TRIGGERED, {
            "recognition_confidence": recognition_confidence,
        })
        return self._state

    def complete_awakening(self) -> None:
        """Called by orchestrator after the audio/LED sequence finishes."""
        self._state = AwakeningState.AWAKENED
        self._bus.publish(AWAKENING_COMPLETE, {})
        log.info("Awakening complete — companion is now alive to this child")

    def force_mechanical(self) -> None:
        """Parent-initiated reset (from dashboard)."""
        self._state = AwakeningState.MECHANICAL
        log.info("Awakening reset to mechanical mode")
