from __future__ import annotations
import json
import time
import logging
from prism.config import PrismConfig
from prism.engines.inner_life import InnerLifeState

log = logging.getLogger(__name__)


class InnerLifeStore:
    def __init__(self, config: PrismConfig) -> None:
        self._cfg = config
        self._conn = None

    def set_conn(self, conn) -> None:
        self._conn = conn

    async def save(self, companion_id: str, state: InnerLifeState) -> None:
        if not self._conn:
            return
        snapshot = json.dumps(state.snapshot())
        await self._conn.execute(
            "INSERT OR REPLACE INTO inner_life_state (id, companion_id, state_json, updated_at) VALUES (1, ?, ?, ?)",
            (companion_id, snapshot, time.time()),
        )
        await self._conn.commit()

    async def load(self, companion_id: str) -> InnerLifeState | None:
        if not self._conn:
            return None
        cursor = await self._conn.execute(
            "SELECT state_json FROM inner_life_state WHERE companion_id = ?",
            (companion_id,),
        )
        row = await cursor.fetchone()
        if not row:
            return None
        try:
            d = json.loads(row[0])
            return InnerLifeState.from_snapshot(d)
        except Exception as e:
            log.error("Failed to load inner life state: %s", e)
            return None
