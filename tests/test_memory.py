from __future__ import annotations
import math
import pytest
from prism.engines.memory import MemoryEngine


def test_salience_gate_blocks_low_salience(memory_engine):
    result = memory_engine.encode("apple", "red apple on table", salience=0.1)
    assert result is None, "Low salience should be gated out"


def test_salience_gate_allows_high_salience(memory_engine):
    result = memory_engine.encode("apple", "red apple on table", salience=0.8)
    assert result is not None


def test_decay_curve(memory_engine):
    node = memory_engine.encode("spiral", "snail shell", salience=0.8)
    assert node is not None
    s0 = node.s
    tau = node.tau
    memory_engine.advance_time(new_day=5.0)
    # Find the node (may still exist)
    nodes = memory_engine.get_all_nodes()
    surviving = [n for n in nodes if n.concept == "spiral"]
    if surviving:
        n = surviving[0]
        expected = s0 * math.exp(-5.0 / tau)
        assert abs(n.s - expected) < 0.01, f"s={n.s} expected ~{expected}"


def test_rehearsal_strengthens_and_extends_tau(memory_engine):
    memory_engine.encode("spiral", "snail", salience=0.6)
    nodes_before = memory_engine.get_all_nodes()
    tau_before = nodes_before[0].tau
    s_before = nodes_before[0].s

    # Second encoding of same concept triggers spreading
    memory_engine.encode("spiral", "pinecone", salience=0.6)
    nodes_after = memory_engine.get_all_nodes()
    # Original node should have been rehearsed
    same_concept = [n for n in nodes_after if n.episode == "snail"]
    assert same_concept[0].s >= s_before
    assert same_concept[0].tau > tau_before


def test_prune_floor(memory_engine, config):
    node = memory_engine.encode("flower", "daisy", salience=0.4)
    assert node is not None
    # Advance far enough to decay below prune floor
    memory_engine.advance_time(new_day=100.0)
    nodes = memory_engine.get_all_nodes()
    assert all(n.s >= config.prune_floor for n in nodes)


def test_codebook_accumulates(memory_engine):
    memory_engine.encode("spiral", "snail", salience=0.7)
    memory_engine.encode("spiral", "pinecone", salience=0.7)
    memory_engine.encode("spiral", "galaxies", salience=0.7)
    cb = memory_engine.get_codebook()
    assert "spiral" in cb
    assert cb["spiral"].strength > 0.5
    assert cb["spiral"].count == 3


def test_affection_mass_increases(memory_engine):
    mass0 = memory_engine.get_affection_mass()
    memory_engine.encode("smile", "Naomi's laugh", salience=0.9)
    mass1 = memory_engine.get_affection_mass()
    assert mass1 > mass0


def test_snapshot_roundtrip(memory_engine):
    memory_engine.encode("apple", "green apple", salience=0.8)
    memory_engine.encode("flower", "daisy", salience=0.6)
    nodes, codebook = memory_engine.get_snapshot()
    fresh = MemoryEngine(memory_engine._cfg)
    fresh.load_snapshot(nodes, codebook)
    assert len(fresh.get_all_nodes()) == len(memory_engine.get_all_nodes())
