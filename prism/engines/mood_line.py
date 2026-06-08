from __future__ import annotations
from prism.config import PrismConfig
from prism.engines.inner_life import InnerLifeState
from prism.engines.memory import MemoryNode
from prism.personas.base import CompanionPersona


def _band(value: float, lo: str, mid: str, hi: str) -> str:
    if value < 0.34:
        return lo
    if value < 0.66:
        return mid
    return hi


class MoodLineCompiler:
    def __init__(self, config: PrismConfig) -> None:
        self._cfg = config

    def compile(
        self,
        state: InnerLifeState,
        persona: CompanionPersona,
        child_name: str,
        top_memories: list[MemoryNode],
        time_of_day: float,  # hours [0,24)
    ) -> str:
        cfg = self._cfg

        energy_w = _band(state.E, persona.energy_lo, persona.energy_mid, persona.energy_hi)
        mood_w   = _band(state.M, persona.mood_lo, persona.mood_mid, persona.mood_hi)
        cur_w    = _band(state.C, persona.curiosity_lo, persona.curiosity_mid, persona.curiosity_hi)
        aff_w    = _band(state.A,
                         f"friendly toward {child_name}",
                         f"warm toward {child_name}",
                         f"deeply fond of {child_name}")

        time_ctx = self._time_context(time_of_day, cfg)
        memory_sentence = self._memory_sentence(top_memories)

        mood_line = (
            f"You are {persona.name}. "
            f"{time_ctx}"
            f"Right now you feel {energy_w} and {mood_w}; you are {cur_w}. "
            f"You feel {aff_w}. "
            f"You are especially charmed by {state.whim} today."
        )
        if memory_sentence:
            mood_line += f" {memory_sentence}"

        return mood_line

    @staticmethod
    def _time_context(t: float, cfg: PrismConfig) -> str:
        if t < cfg.wake_hour:
            return "It's very early morning, so you're just waking up. "
        if t >= cfg.bed_hour:
            return "It's near bedtime, so you're dreamy and soft-spoken. "
        if cfg.nap_hour <= t < cfg.nap_hour + 2:
            return "It's afternoon quiet-time, so you're a bit mellow. "
        return ""

    @staticmethod
    def _memory_sentence(memories: list[MemoryNode]) -> str:
        if not memories:
            return ""
        concepts = []
        for m in memories[:3]:
            if m.concept not in concepts:
                concepts.append(m.concept)
        if len(concepts) == 1:
            return f"You remember {concepts[0]} fondly."
        parts = ", ".join(concepts[:-1]) + f" and {concepts[-1]}"
        return f"You remember {parts} from before."
