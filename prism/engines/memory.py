from __future__ import annotations
import math
import uuid
from dataclasses import dataclass, field
from typing import Optional
from prism.config import PrismConfig


@dataclass
class MemoryNode:
    id: str
    concept: str
    episode: str              # specific instance (e.g., "snail on the path")
    salience: float           # reaction magnitude at encoding [0,1]
    s: float                  # current strength [0,1]
    tau: float                # decay time-constant in days
    rehearsals: int
    last_day: float           # fractional day since epoch
    contexts: list[str]       # list of episodes where concept reappeared
    created_day: float

    def to_dict(self) -> dict:
        return {
            'id': self.id, 'concept': self.concept, 'episode': self.episode,
            'salience': self.salience, 's': self.s, 'tau': self.tau,
            'rehearsals': self.rehearsals, 'last_day': self.last_day,
            'contexts': self.contexts, 'created_day': self.created_day,
        }

    @classmethod
    def from_dict(cls, d: dict) -> 'MemoryNode':
        return cls(**d)


@dataclass
class CodebookEntry:
    concept: str
    strength: float  # [0,1] compressed gist strength
    count: int       # total encoding count

    def to_dict(self) -> dict:
        return {'concept': self.concept, 'strength': self.strength, 'count': self.count}


class MemoryEngine:
    def __init__(self, config: PrismConfig) -> None:
        self._cfg = config
        self._nodes: list[MemoryNode] = []
        self._codebook: dict[str, CodebookEntry] = {}
        self._current_day: float = 0.0

    @property
    def current_day(self) -> float:
        return self._current_day

    def set_day(self, day: float) -> None:
        self._current_day = day

    def encode(self, concept: str, episode: str, salience: float,
               context_tag: str = "") -> Optional[MemoryNode]:
        """
        Salience gate: if salience < gate, discard immediately, return None.
        Otherwise:
        - Compute tau = tau_base + tau_salience_scale * salience
        - s_init = clamp01(0.55 + salience * 0.45)
        - Spreading activation: existing same-concept nodes get s += rehearsal_kick, tau *= (1 + consolidation_factor), contexts appended
        - Add new MemoryNode
        - Update codebook
        """
        cfg = self._cfg
        if salience < cfg.salience_gate:
            return None

        tau = cfg.tau_base + cfg.tau_salience_scale * salience
        s_init = min(1.0, max(0.0, 0.55 + salience * 0.45))

        # Spreading activation on existing same-concept nodes
        for node in self._nodes:
            if node.concept == concept:
                node.s = min(1.0, node.s + cfg.rehearsal_kick)
                node.tau *= (1.0 + cfg.consolidation_factor)
                node.rehearsals += 1
                node.last_day = self._current_day
                tag = episode if episode else context_tag
                if tag and tag not in node.contexts:
                    node.contexts.append(tag)

        # New episodic node
        node = MemoryNode(
            id=str(uuid.uuid4()),
            concept=concept,
            episode=episode,
            salience=salience,
            s=s_init,
            tau=tau,
            rehearsals=0,
            last_day=self._current_day,
            contexts=[episode] if episode else [],
            created_day=self._current_day,
        )
        self._nodes.append(node)

        # Update codebook
        entry = self._codebook.get(concept)
        if entry is None:
            entry = CodebookEntry(concept=concept, strength=0.0, count=0)
            self._codebook[concept] = entry
        entry.strength = min(1.0, entry.strength + cfg.codebook_strength_per_encode + salience * cfg.codebook_salience_bonus)
        entry.count += 1

        return node

    def rehearse(self, node_id: str, context_tag: str = "") -> None:
        """Manually reactivate a node (companion references a memory)."""
        cfg = self._cfg
        for node in self._nodes:
            if node.id == node_id:
                node.s = min(1.0, node.s + cfg.rehearsal_kick)
                node.tau *= (1.0 + cfg.consolidation_factor)
                node.rehearsals += 1
                node.last_day = self._current_day
                if context_tag and context_tag not in node.contexts:
                    node.contexts.append(context_tag)
                return

    def advance_time(self, new_day: float) -> list[str]:
        """
        Decay all nodes: s *= exp(-dt/tau).
        Prune nodes where s < prune_floor.
        Returns IDs of pruned nodes.
        """
        cfg = self._cfg
        dt = new_day - self._current_day
        if dt <= 0:
            self._current_day = new_day
            return []

        pruned_ids: list[str] = []
        surviving: list[MemoryNode] = []
        for node in self._nodes:
            node.s *= math.exp(-dt / node.tau)
            if node.s < cfg.prune_floor:
                pruned_ids.append(node.id)
            else:
                surviving.append(node)
        self._nodes = surviving

        # Decay codebook (12x slower than episodes)
        cb_decay_tau = cfg.tau_base * 12.0
        for entry in self._codebook.values():
            entry.strength *= math.exp(-dt / cb_decay_tau)

        self._current_day = new_day
        return pruned_ids

    def get_top_activated(self, concept_hint: str = "", k: int = 3) -> list[MemoryNode]:
        """
        Return top-k nodes by current strength, optionally biased toward concept_hint
        via spreading activation.
        """
        if not self._nodes:
            return []

        cfg = self._cfg
        scored: list[tuple[float, MemoryNode]] = []
        for node in self._nodes:
            score = node.s
            if concept_hint and node.concept == concept_hint:
                score += cfg.spreading_activation  # bias toward hinted concept
            scored.append((score, node))

        scored.sort(key=lambda t: t[0], reverse=True)
        return [n for _, n in scored[:k]]

    def get_contexts_for_concept(self, concept: str) -> list[str]:
        """All distinct episode contexts ever recorded for this concept."""
        seen: set[str] = set()
        result: list[str] = []
        for node in self._nodes:
            if node.concept == concept:
                for ctx in node.contexts:
                    if ctx not in seen:
                        seen.add(ctx)
                        result.append(ctx)
        return result

    def get_affection_mass(self) -> float:
        """Sum of s*salience across all nodes, normalized to [0,1]."""
        if not self._nodes:
            return 0.0
        total = sum(n.s * n.salience for n in self._nodes)
        return min(1.0, total / max(1, len(self._nodes)))

    def get_codebook(self) -> dict[str, CodebookEntry]:
        return dict(self._codebook)

    def get_all_nodes(self) -> list[MemoryNode]:
        return list(self._nodes)

    def get_snapshot(self) -> tuple[list[dict], dict[str, dict]]:
        return (
            [n.to_dict() for n in self._nodes],
            {k: v.to_dict() for k, v in self._codebook.items()},
        )

    def load_snapshot(self, nodes: list[dict], codebook: dict[str, dict]) -> None:
        self._nodes = [MemoryNode.from_dict(d) for d in nodes]
        self._codebook = {k: CodebookEntry(**v) for k, v in codebook.items()}
