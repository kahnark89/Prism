from __future__ import annotations
import time
import logging
from typing import Optional, TypedDict
from prism.config import PrismConfig
from prism.engines.grounding import GroundingAccumulator

log = logging.getLogger(__name__)


class CIAERRecord(TypedDict, total=False):
    timestamp: float
    cause: str
    intuition: str
    confidence_in_voice: str   # SKILL | RULE | KNOWLEDGE
    action: str
    effect: str
    reaction: str
    salience: float
    result: str
    shadow_actions: list
    model_revision: str        # optional; precious when present
    # Pre-ENV fields
    pre_env_time_of_day: float
    pre_env_companion_mood: str
    pre_env_child_recent_concepts: list


class LearningLog:
    """
    Writes CIAER+ records immediately (append-only).
    OGC HARD RULE: grounding_accumulator.record_reappearance() is only called
    when learning evidence exists — never on engagement-only events.
    """

    def __init__(self, config: PrismConfig,
                 grounding: GroundingAccumulator) -> None:
        self._cfg = config
        self._grounding = grounding
        self._records: list[CIAERRecord] = []
        self._store = None   # set by persistence layer after init

    def set_store(self, store) -> None:
        self._store = store

    def record_event(self, record: CIAERRecord) -> None:
        record['timestamp'] = record.get('timestamp', time.time())
        self._records.append(record)

        if self._store:
            try:
                self._store.append(record)
            except Exception as e:
                log.error("Failed to persist CIAER record: %s", e)

        # OGC enforcement: only update grounding on learning evidence
        concept = record.get('cause', '')
        has_model_revision = bool(record.get('model_revision'))
        result = record.get('result', '')
        new_context = record.get('action', '')

        # First exposure
        if concept:
            self._grounding.record_exposure(concept)

        # Reappearance with learning evidence: model revision OR non-trivial result
        has_learning_evidence = has_model_revision or (
            result and result not in ('', 'none', 'no_change')
        )
        if has_learning_evidence and concept:
            self._grounding.record_reappearance(
                concept=concept,
                new_context=new_context,
                has_model_revision=has_model_revision,
            )

    def get_recent(self, n: int = 50) -> list[CIAERRecord]:
        return self._records[-n:]

    def get_by_concept(self, concept: str) -> list[CIAERRecord]:
        return [r for r in self._records if r.get('cause') == concept]

    def get_all(self) -> list[CIAERRecord]:
        return list(self._records)
