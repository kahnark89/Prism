package com.cappsconsulting.prism.parentsuite.data

import com.cappsconsulting.prism.engine.grounding.GroundingStatus

/**
 * The four states a concept tile can occupy — Doc 2.2 §2, all four rendered
 * equally legibly: "absence shown as clearly as presence" (Principle 12).
 *
 * [ACTIVE]: on the learning menu; Companion may present it.
 * [DORMANT]: parent paused it; Companion won't present it until the parent re-enables.
 * [ABSENT_BY_PARENT]: parent marked it hard off-limits — not a temporary pause.
 * [NOT_YET_REACHED]: the child hasn't encountered this concept yet.
 */
enum class ConceptTileState {
    ACTIVE,
    DORMANT,
    ABSENT_BY_PARENT,
    NOT_YET_REACHED,
}

/**
 * How quickly the Companion paces this concept — Doc 2.2 §3's three gears:
 * [SLOW] for concepts the child needs more time with; [BALANCED] the default;
 * [FAST] for concepts the child has clearly outpaced.
 */
enum class Pacing(val displayLabel: String) {
    SLOW("more time"),
    BALANCED("balanced"),
    FAST("move faster"),
}

/**
 * One concept in the parent's mental map of the child's learning space — the
 * record Doc 2.2 §1 says the Map and Trajectory screens render.
 *
 * No numeric scores: [groundingStatus] comes from [GroundingStatus] in :engine and
 * renders as banded plain language only (Hard Line 6 — "no visible numeric score
 * anywhere, to anyone"). The banded labels are "exploring", "getting it", "owns it" —
 * never a percentage, never a decimal.
 *
 * [counterBalanceWeight] drives the counter-balance signal Doc 2.2 §4 names —
 * a "permanent glass-box control," not an advanced-users-only feature. It is always
 * visible in the concept detail sheet and indicated on the tile when non-zero,
 * so the parent can see at a glance which concepts carry extra weight.
 */
data class ConceptRecord(
    val id: String,
    val label: String,
    val tileState: ConceptTileState,
    val groundingStatus: GroundingStatus = GroundingStatus.EXPLORING,
    val pacing: Pacing = Pacing.BALANCED,
    val counterBalanceWeight: Double = 0.0,
    val firstExposedAtEpochSeconds: Double? = null,
    val lastSeenAtEpochSeconds: Double? = null,
    val sessionCount: Int = 0,
)
