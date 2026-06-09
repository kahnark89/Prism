package com.cappsconsulting.prism.sync.payload

import kotlinx.serialization.Serializable

/**
 * Companion -> Parent Suite — Doc 3.0 §3.1, first bullet: "session summaries / CIAER
 * records — the data the Map and Trajectory render (exposure, reappearance,
 * grounding band, never raw transcripts; Doc 2.2 §1)."
 *
 * Notice what's *absent*, on purpose: no transcript text, no audio/video reference
 * or pointer, no raw [Double] confidence — [groundingStatus] carries the banded
 * wire-value string from `GroundingStatus` in `:engine` (Hard Line 6: "no visible
 * numeric score anywhere, to anyone," and that includes the wire format two apps
 * use to talk about a child's mind to each other). Doc 3.0 §3.1 calls this list of
 * fields "the privacy surface" and says explicitly to "keep this list short" —
 * widening it is a decision for the architect, not something to slip in while
 * porting, which is why every field here traces to a named bullet in that section.
 */
@Serializable
data class SessionSummary(
    val sessionId: String,
    val startedAtEpochSeconds: Double,
    val endedAtEpochSeconds: Double,
    val concept: String,
    val groundingStatus: String,
    val isNewExposure: Boolean,
    val isReappearance: Boolean,
    val companionMoodBand: String,
)

/**
 * Parent Suite -> Companion — Doc 3.0 §3.1, second bullet: "the current menu state
 * (on/off-menu, pacing, boundaries, counter-balance weights — Doc 2.2 §4), which
 * feeds the suggested-topics signal to the LLM."
 *
 * This is *the* shape of parental influence on a live session, full stop — it
 * reaches the companion only as a reweighting of what gets *offered* next, never
 * as a lever on the companion's internal state. That boundary is Doc 2.2 §4's
 * counter-balance contract ("a permanent glass-box control... never targets
 * internal state") and this payload is what makes it structural on the wire: there
 * is no field here *to* target internal state with, even if someone wanted to.
 */
@Serializable
data class MenuState(
    val onMenuConcepts: List<String> = emptyList(),
    val offMenuConcepts: List<String> = emptyList(),
    val pacing: String = "balanced",
    val dailyBoundaryMinutes: Int? = null,
    val counterBalanceWeights: Map<String, Double> = emptyMap(),
    val updatedAtEpochSeconds: Double = 0.0,
)
