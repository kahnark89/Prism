from __future__ import annotations
import pytest
from prism.engines.inner_life import InnerLifeState, VARS
from prism.engines.memory import MemoryNode
from prism.engines.mood_line import MoodLineCompiler


def make_state(M=0.5, E=0.5, C=0.5, A=0.5, S=0.5):
    return InnerLifeState(
        M=M, E=E, C=C, A=A, S=S,
        M0=M, E0=E, C0=C, A0=A, S0=S,
        A0_seed=A,
        whim="spirals",
    )


def test_low_energy_phrase(mood_compiler, pip_persona):
    state = make_state(E=0.1)
    line = mood_compiler.compile(state, pip_persona, "Naomi", [], 10.0)
    assert pip_persona.energy_lo in line


def test_high_energy_phrase(mood_compiler, pip_persona):
    state = make_state(E=0.9)
    line = mood_compiler.compile(state, pip_persona, "Naomi", [], 10.0)
    assert pip_persona.energy_hi in line


def test_child_name_in_affection(mood_compiler, pip_persona):
    state = make_state(A=0.9)
    line = mood_compiler.compile(state, pip_persona, "Naomi", [], 10.0)
    assert "Naomi" in line


def test_memory_sentence_injected(mood_compiler, pip_persona):
    import uuid
    node = MemoryNode(
        id=str(uuid.uuid4()), concept="spiral", episode="snail shell",
        salience=0.8, s=0.9, tau=10.0, rehearsals=2, last_day=1.0,
        contexts=["snail shell"], created_day=0.0,
    )
    state = make_state()
    line = mood_compiler.compile(state, pip_persona, "Naomi", [node], 10.0)
    assert "spiral" in line


def test_whim_in_output(mood_compiler, pip_persona):
    state = make_state()
    state.whim = "tiny things"
    line = mood_compiler.compile(state, pip_persona, "Naomi", [], 10.0)
    assert "tiny things" in line


def test_bedtime_context(mood_compiler, pip_persona):
    state = make_state()
    line = mood_compiler.compile(state, pip_persona, "Naomi", [], 21.0)
    assert "bedtime" in line.lower() or "dreamy" in line.lower()
