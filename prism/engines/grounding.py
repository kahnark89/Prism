from __future__ import annotations
import time
from dataclasses import dataclass, field
from prism.config import PrismConfig


@dataclass
class GroundingRecord:
    concept: str
    confidence: float = 0.0          # [0,1], OGC-accumulated
    first_exposed: float = 0.0       # unix timestamp
    context_distances: list[float] = field(default_factory=list)  # distance values per reappearance
    seen_contexts: list[str] = field(default_factory=list)         # actual context strings (for distance calc)
    parent_confirmations: int = 0
    status: str = "exploring"        # exploring | getting_it | owns_it

    def to_dict(self) -> dict:
        return {
            'concept': self.concept,
            'confidence': self.confidence,
            'first_exposed': self.first_exposed,
            'context_distances': self.context_distances,
            'seen_contexts': self.seen_contexts,
            'parent_confirmations': self.parent_confirmations,
            'status': self.status,
        }

    @classmethod
    def from_dict(cls, d: dict) -> 'GroundingRecord':
        # Handle records saved before seen_contexts was added
        d.setdefault('seen_contexts', [])
        return cls(**d)


def _compute_context_distance(seen_contexts: list[str], new_context: str) -> float:
    """
    Simple context-distance heuristic.
    Same episode context = near zero (echo, not evidence).
    Novel episode = substantial gain (genuine transfer).
    (Full semantic distance is future work.)
    """
    if not seen_contexts:
        return 0.2   # first reappearance: small gain
    if new_context in seen_contexts:
        return 0.02  # same context: near-zero
    return 0.35      # genuinely new context: substantial gain


class GroundingAccumulator:
    """
    Tracks invisible learning signal per concept.
    OGC HARD RULE: confidence only accumulates on LEARNING EVIDENCE
    (context-distant reappearance or parent confirmation).
    Engagement alone NEVER increments confidence.
    """

    def __init__(self, config: PrismConfig) -> None:
        self._cfg = config
        self._records: dict[str, GroundingRecord] = {}

    def record_exposure(self, concept: str) -> None:
        """First exposure: hold confidence in suspension."""
        if concept not in self._records:
            self._records[concept] = GroundingRecord(
                concept=concept,
                first_exposed=time.time(),
            )

    def record_reappearance(self, concept: str, new_context: str,
                             has_model_revision: bool = False) -> None:
        """
        Called only when there is LEARNING EVIDENCE:
        - context-distant reappearance (new_context differs from prior contexts), OR
        - model_revision was present in the CIAER record.
        Never called on engagement-only events.
        """
        rec = self._records.get(concept)
        if rec is None:
            rec = GroundingRecord(concept=concept, first_exposed=time.time())
            self._records[concept] = rec

        distance = _compute_context_distance(rec.seen_contexts, new_context)
        if has_model_revision:
            distance = max(distance, 0.5)  # model revision is strong evidence

        rec.confidence = min(1.0, rec.confidence + distance)
        rec.context_distances.append(distance)
        if new_context and new_context not in rec.seen_contexts:
            rec.seen_contexts.append(new_context)

        rec.status = self._classify(rec.confidence)

    def parent_confirm(self, concept: str) -> None:
        """Parent volunteers outside-world independent instance. Strong boost."""
        rec = self._records.get(concept)
        if rec is None:
            rec = GroundingRecord(concept=concept, first_exposed=time.time())
            self._records[concept] = rec
        rec.parent_confirmations += 1
        rec.confidence = min(1.0, rec.confidence + 0.4)
        rec.status = self._classify(rec.confidence)

    @staticmethod
    def _classify(confidence: float) -> str:
        if confidence < 0.3:
            return "exploring"
        if confidence < 0.7:
            return "getting_it"
        return "owns_it"

    def get_all_records(self) -> list[GroundingRecord]:
        return list(self._records.values())

    def get_record(self, concept: str) -> GroundingRecord | None:
        return self._records.get(concept)

    def get_snapshot(self) -> list[dict]:
        return [r.to_dict() for r in self._records.values()]

    def load_snapshot(self, records: list[dict]) -> None:
        self._records = {d['concept']: GroundingRecord.from_dict(d) for d in records}
