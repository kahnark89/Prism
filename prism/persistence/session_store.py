from __future__ import annotations
import time
import uuid
import logging
from prism.config import PrismConfig

log = logging.getLogger(__name__)


class SessionStore:
    def __init__(self, config: PrismConfig) -> None:
        self._cfg = config
        self._conn = None
        self._current_id: str | None = None

    def set_conn(self, conn) -> None:
        self._conn = conn

    async def start_session(self, companion_id: str) -> str:
        session_id = str(uuid.uuid4())
        self._current_id = session_id
        if self._conn:
            await self._conn.execute(
                "INSERT INTO sessions (id, started_at, companion_id) VALUES (?, ?, ?)",
                (session_id, time.time(), companion_id),
            )
            await self._conn.commit()
        return session_id

    async def end_session(self, event_count: int = 0) -> None:
        if not self._conn or not self._current_id:
            return
        await self._conn.execute(
            "UPDATE sessions SET ended_at = ?, event_count = ? WHERE id = ?",
            (time.time(), event_count, self._current_id),
        )
        await self._conn.commit()
        self._current_id = None

    async def increment_events(self) -> None:
        if not self._conn or not self._current_id:
            return
        await self._conn.execute(
            "UPDATE sessions SET event_count = event_count + 1 WHERE id = ?",
            (self._current_id,),
        )
        await self._conn.commit()
