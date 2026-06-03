from __future__ import annotations
import json
import logging
from prism.config import PrismConfig

log = logging.getLogger(__name__)


class MemoryStore:
    def __init__(self, config: PrismConfig) -> None:
        self._cfg = config
        self._conn = None

    def set_conn(self, conn) -> None:
        self._conn = conn

    async def save(self, nodes: list[dict], codebook: dict[str, dict]) -> None:
        if not self._conn:
            return
        await self._conn.execute("DELETE FROM memory_nodes")
        await self._conn.execute("DELETE FROM codebook")
        for node_dict in nodes:
            await self._conn.execute(
                "INSERT INTO memory_nodes (id, node_json) VALUES (?, ?)",
                (node_dict['id'], json.dumps(node_dict)),
            )
        for concept, entry_dict in codebook.items():
            await self._conn.execute(
                "INSERT OR REPLACE INTO codebook (concept, entry_json) VALUES (?, ?)",
                (concept, json.dumps(entry_dict)),
            )
        await self._conn.commit()

    async def load(self) -> tuple[list[dict], dict[str, dict]]:
        if not self._conn:
            return [], {}
        cursor = await self._conn.execute("SELECT node_json FROM memory_nodes")
        rows = await cursor.fetchall()
        nodes = [json.loads(r[0]) for r in rows]
        cursor = await self._conn.execute("SELECT concept, entry_json FROM codebook")
        rows = await cursor.fetchall()
        codebook = {r[0]: json.loads(r[1]) for r in rows}
        return nodes, codebook
