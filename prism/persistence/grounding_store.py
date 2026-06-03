from __future__ import annotations
import json
import logging
from prism.config import PrismConfig

log = logging.getLogger(__name__)


class GroundingStore:
    def __init__(self, config: PrismConfig) -> None:
        self._cfg = config
        self._conn = None

    def set_conn(self, conn) -> None:
        self._conn = conn

    async def save(self, records: list[dict]) -> None:
        if not self._conn:
            return
        await self._conn.execute("DELETE FROM grounding")
        for rec in records:
            await self._conn.execute(
                "INSERT INTO grounding (concept, record_json) VALUES (?, ?)",
                (rec['concept'], json.dumps(rec)),
            )
        await self._conn.commit()

    async def load(self) -> list[dict]:
        if not self._conn:
            return []
        cursor = await self._conn.execute("SELECT record_json FROM grounding")
        rows = await cursor.fetchall()
        return [json.loads(r[0]) for r in rows]
