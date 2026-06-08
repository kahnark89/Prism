from __future__ import annotations
import logging
import time
import numpy as np
from prism.config import PrismConfig

log = logging.getLogger(__name__)


class RecognitionStore:
    """Separate DB file — parent can delete independently."""

    def __init__(self, config: PrismConfig) -> None:
        self._cfg = config
        self._conn = None

    def set_conn(self, conn) -> None:
        self._conn = conn

    async def save_embeddings(self, embeddings: list[np.ndarray],
                               template_type: str = "face") -> None:
        if not self._conn:
            return
        now = time.time()
        for emb in embeddings:
            blob = emb.astype(np.float32).tobytes()
            await self._conn.execute(
                "INSERT INTO recognition_templates (type, embedding_blob, enrolled_at) VALUES (?, ?, ?)",
                (template_type, blob, now),
            )
        await self._conn.commit()

    async def load_embeddings(self, template_type: str = "face") -> list[np.ndarray]:
        if not self._conn:
            return []
        cursor = await self._conn.execute(
            "SELECT embedding_blob FROM recognition_templates WHERE type = ?",
            (template_type,),
        )
        rows = await cursor.fetchall()
        return [np.frombuffer(r[0], dtype=np.float32) for r in rows]

    async def delete_all(self) -> None:
        if not self._conn:
            return
        await self._conn.execute("DELETE FROM recognition_templates")
        await self._conn.commit()
        log.info("All recognition templates deleted")
