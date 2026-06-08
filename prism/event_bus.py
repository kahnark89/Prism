from __future__ import annotations
import asyncio
from collections import defaultdict
from typing import Any, Callable

# Event type constants
SHUTTER_PRESSED = "shutter_pressed"
VISION_RESULT = "vision_result"
AWAKENING_TRIGGERED = "awakening_triggered"
AWAKENING_COMPLETE = "awakening_complete"
COMPANION_SWITCHED = "companion_switched"
INNER_LIFE_TICKED = "inner_life_ticked"
MEMORY_ENCODED = "memory_encoded"
PARENT_CONFIRMATION = "parent_confirmation"
IDLE_FIRED = "idle_fired"
SESSION_STARTED = "session_started"
SESSION_ENDED = "session_ended"


class EventBus:
    def __init__(self) -> None:
        self._handlers: dict[str, list[Callable]] = defaultdict(list)

    def subscribe(self, event_type: str, handler: Callable) -> None:
        self._handlers[event_type].append(handler)

    def unsubscribe(self, event_type: str, handler: Callable) -> None:
        self._handlers[event_type] = [
            h for h in self._handlers[event_type] if h is not handler
        ]

    def publish(self, event_type: str, payload: dict[str, Any] | None = None) -> None:
        for handler in self._handlers.get(event_type, []):
            handler(payload or {})

    async def publish_async(self, event_type: str, payload: dict[str, Any] | None = None) -> None:
        for handler in self._handlers.get(event_type, []):
            result = handler(payload or {})
            if asyncio.iscoroutine(result):
                await result
