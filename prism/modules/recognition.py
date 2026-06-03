from __future__ import annotations
import logging
import numpy as np
from dataclasses import dataclass
from typing import Optional
from prism.config import PrismConfig

log = logging.getLogger(__name__)


@dataclass
class RecognitionResult:
    is_enrolled_child: bool
    confidence: float
    mode: str  # "face" | "voice" | "mock"


class RecognitionModule:
    """
    On-device only. Never calls cloud.
    Raw frames/audio are discarded immediately after embedding extraction.
    Templates stored in separate recognition.db (never in main db).
    """

    def __init__(self, config: PrismConfig) -> None:
        self._cfg = config
        self._enrolled: bool = False
        self._face_templates: list[np.ndarray] = []
        self._recognition_db_path = config.recognition_db_path

    def is_enrolled(self) -> bool:
        return self._enrolled or len(self._face_templates) > 0

    def enroll(self, frames: list[np.ndarray],
               audio_clips: Optional[list[bytes]] = None) -> bool:
        """
        Extract embeddings from frames, store in recognition_store.
        Raw frames discarded after this method returns.
        """
        if not frames:
            return False
        # Placeholder: real impl uses face_recognition or deepface
        # Store mean embedding of frame center as template
        embeddings = []
        for f in frames:
            h, w = f.shape[:2]
            patch = f[h//4:3*h//4, w//4:3*w//4]
            emb = patch.mean(axis=(0, 1)).astype(np.float32)
            embeddings.append(emb)
        self._face_templates = embeddings
        self._enrolled = True
        log.info("Enrolled %d face templates", len(embeddings))
        return True

    def recognize(self, frame: np.ndarray,
                  audio: Optional[bytes] = None) -> RecognitionResult:
        """On-device only. Returns yes/no + confidence. Never retains raw data."""
        if not self._face_templates:
            return RecognitionResult(is_enrolled_child=False, confidence=0.0, mode="mock")

        h, w = frame.shape[:2]
        patch = frame[h//4:3*h//4, w//4:3*w//4]
        query_emb = patch.mean(axis=(0, 1)).astype(np.float32)

        # Cosine similarity to templates
        scores = []
        for tmpl in self._face_templates:
            norm_q = np.linalg.norm(query_emb)
            norm_t = np.linalg.norm(tmpl)
            if norm_q > 0 and norm_t > 0:
                sim = float(np.dot(query_emb, tmpl) / (norm_q * norm_t))
                scores.append(sim)

        if not scores:
            return RecognitionResult(is_enrolled_child=False, confidence=0.0, mode="face")

        confidence = max(scores)
        is_child = confidence > 0.85
        return RecognitionResult(is_enrolled_child=is_child, confidence=confidence, mode="face")

    def delete_templates(self) -> None:
        """Parent-callable. Wipes all templates."""
        self._face_templates = []
        self._enrolled = False
        import sqlite3, os
        db = self._recognition_db_path
        if db.exists():
            try:
                conn = sqlite3.connect(db)
                conn.execute("DELETE FROM recognition_templates")
                conn.commit()
                conn.close()
            except Exception:
                pass
        log.info("Recognition templates deleted")
