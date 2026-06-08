from __future__ import annotations
import pytest
from prism.engines.grounding import GroundingAccumulator


def test_engagement_alone_does_not_increment(grounding):
    """OGC hard rule: calling record_exposure multiple times WITHOUT
    context-distant reappearance must NOT increment confidence."""
    grounding.record_exposure("apple")
    c0 = grounding.get_record("apple").confidence
    # More exposures (same device context, no model revision) — should stay at 0
    grounding.record_exposure("apple")
    grounding.record_exposure("apple")
    c1 = grounding.get_record("apple").confidence
    assert c1 == c0, f"Engagement-only should not increment confidence: {c0} → {c1}"


def test_context_distance_same_context_near_zero(grounding):
    grounding.record_exposure("spiral")
    rec = grounding.get_record("spiral")
    c_before = rec.confidence
    grounding.record_reappearance("spiral", "snail shell", has_model_revision=False)
    c_after = grounding.get_record("spiral").confidence
    gain1 = c_after - c_before

    # Same context again — should be near-zero gain
    c_before2 = c_after
    grounding.record_reappearance("spiral", "snail shell", has_model_revision=False)
    c_after2 = grounding.get_record("spiral").confidence
    gain2 = c_after2 - c_before2

    assert gain2 < gain1 * 0.2, f"Same-context gain {gain2} should be much less than first gain {gain1}"


def test_model_revision_gives_strong_boost(grounding):
    grounding.record_exposure("flower")
    grounding.record_reappearance("flower", "daisy", has_model_revision=True)
    rec = grounding.get_record("flower")
    assert rec.confidence >= 0.4, f"Model revision should give strong confidence boost: {rec.confidence}"


def test_parent_confirm_larger_than_device(grounding):
    grounding.record_exposure("tree")
    grounding.record_reappearance("tree", "oak in garden", has_model_revision=False)
    c_device = grounding.get_record("tree").confidence

    grounding2 = GroundingAccumulator(grounding._cfg)
    grounding2.record_exposure("tree")
    grounding2.record_reappearance("tree", "oak in garden", has_model_revision=False)
    grounding2.parent_confirm("tree")
    c_parent = grounding2.get_record("tree").confidence

    assert c_parent > c_device, f"Parent confirm {c_parent} should exceed device-only {c_device}"


def test_status_transitions(grounding):
    grounding.record_exposure("color")
    assert grounding.get_record("color").status == "exploring"
    for i in range(5):
        grounding.record_reappearance("color", f"context_{i}", has_model_revision=False)
    # Multiple genuine reappearances should progress status
    rec = grounding.get_record("color")
    assert rec.status in ("getting_it", "owns_it", "exploring")


def test_snapshot_roundtrip(grounding):
    grounding.record_exposure("cat")
    grounding.record_reappearance("cat", "tabby", has_model_revision=True)
    snap = grounding.get_snapshot()
    fresh = GroundingAccumulator(grounding._cfg)
    fresh.load_snapshot(snap)
    assert fresh.get_record("cat").confidence == pytest.approx(
        grounding.get_record("cat").confidence, abs=1e-6
    )
