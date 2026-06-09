package com.cappsconsulting.prism.engine.learninglog

import com.cappsconsulting.prism.engine.grounding.GroundingAccumulator
import kotlinx.serialization.Serializable

/**
 * Direct port of `prism/modules/learning_log.py::CIAERRecord` (a `TypedDict` in the
 * original; a sealed, serializable data class here — Kotlin has no structural typing,
 * and this record crosses the sync boundary, Doc 3.0 §3.1, so it needs a stable wire
 * shape with `kotlinx.serialization` rather than an open bag of `Any?`).
 *
 * This is CIAER+ written twice over (Genotype "Schema & memory commitments"): once
 * for the child as the subject of `cause`/`intuition`/`effect`, and a second time in
 * the `pre_env_*` fields, which capture the *companion's* own pre-environment state —
 * the second CIAER agent the schema was built to support.
 */
@Serializable
data class CIAERRecord(
    val timestamp: Double,
    val cause: String = "",
    val intuition: String = "",
    val confidenceInVoice: String = "",
    val action: String = "",
    val effect: String = "",
    val reaction: String = "",
    val salience: Double = 0.0,
    val result: String = "",
    val shadowActions: List<String> = emptyList(),
    /** Optional; precious when present — strong learning-evidence signal (see [LearningLog.recordEvent]). */
    val modelRevision: String? = null,
    val preEnvTimeOfDay: Double = 0.0,
    val preEnvCompanionMood: String = "",
    val preEnvChildRecentConcepts: List<String> = emptyList(),
)

/**
 * Direct port of `prism/modules/learning_log.py::LearningLog`.
 *
 * Writes CIAER+ records immediately, append-only — and is the single call site that
 * enforces the OGC hard rule: [GroundingAccumulator.recordReappearance] fires *only*
 * when [recordEvent] sees actual learning evidence (a model revision, or a non-trivial
 * result), never on engagement alone. Centralizing the rule here — rather than letting
 * every call site decide for itself whether to bump grounding — is what makes "optimize
 * the teaching, never the engagement" (Hard Line 10) structural rather than a
 * convention someone could forget at one of many call sites.
 *
 * @property onPersist Called for every record (replaces the Python `_store` /
 * `set_store` seam — the persistence layer is injected by whichever app wires this up;
 * see `:companion-app` `data/LearningLogStore`). Exceptions are caught and logged by
 * the caller's wiring, exactly as the Python wrapped `self._store.append` in try/except —
 * a persistence failure must never interrupt the live session.
 */
class LearningLog(
    private val grounding: GroundingAccumulator,
    private val onPersist: ((CIAERRecord) -> Unit)? = null,
) {
    private val records: MutableList<CIAERRecord> = mutableListOf()

    fun recordEvent(record: CIAERRecord) {
        records.add(record)
        onPersist?.invoke(record)

        // OGC enforcement — the load-bearing logic of this whole class.
        val concept = record.cause
        val hasModelRevision = !record.modelRevision.isNullOrEmpty()
        val result = record.result
        val newContext = record.action

        if (concept.isNotEmpty()) {
            grounding.recordExposure(concept)
        }

        val hasLearningEvidence = hasModelRevision ||
            (result.isNotEmpty() && result != "none" && result != "no_change")

        if (hasLearningEvidence && concept.isNotEmpty()) {
            grounding.recordReappearance(
                concept = concept,
                newContext = newContext,
                hasModelRevision = hasModelRevision,
            )
        }
    }

    fun getRecent(n: Int = 50): List<CIAERRecord> = records.takeLast(n)
    fun getByConcept(concept: String): List<CIAERRecord> = records.filter { it.cause == concept }
    fun getAll(): List<CIAERRecord> = records.toList()
}
