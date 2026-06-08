from __future__ import annotations
import json
import logging
from prism.config import PrismConfig

log = logging.getLogger(__name__)


class LearningLogStore:
    def __init__(self, config: PrismConfig) -> None:
        self._cfg = config
        self._conn = None

    def set_conn(self, conn) -> None:
        self._conn = conn

    async def append(self, record: dict) -> None:
        if not self._conn:
            return
        await self._conn.execute(
            "INSERT INTO ciaer_log (timestamp, concept, salience, record_json) VALUES (?, ?, ?, ?)",
            (
                record.get('timestamp', 0.0),
                record.get('cause', ''),
                record.get('salience', 0.0),
                json.dumps(record),
            ),
        )
        await self._conn.commit()

    async def load_recent(self, n: int = 100) -> list[dict]:
        if not self._conn:
            return []
        cursor = await self._conn.execute(
            "SELECT record_json FROM ciaer_log ORDER BY timestamp DESC LIMIT ?", (n,)
        )
        rows = await cursor.fetchall()
        return [json.loads(r[0]) for r in rows]

    async def load_by_concept(self, concept: str) -> list[dict]:
        if not self._conn:
            return []
        cursor = await self._conn.execute(
            "SELECT record_json FROM ciaer_log WHERE concept = ? ORDER BY timestamp",
            (concept,),
        )
        rows = await cursor.fetchall()
        return [json.loads(r[0]) for r in rows]
