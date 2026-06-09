package com.cappsconsulting.prism.engine.grounding

import com.cappsconsulting.prism.engine.config.PrismConfig
import kotlinx.serialization.Serializable
import kotlin.math.min

/**
 * Direct port of `prism/engines/grounding.py` — the OGC (outcome-grounded confidence)
 * accumulator. This is the invisible learning signal at the heart of Doc 1.8 and the
 * Parent Suite's Map (Doc 2.2): per-concept confidence that accumulates *only* on
 * learning evidence, never on engagement.
 *
 * OGC HARD RULE, preserved verbatim from the Python docstring: confidence only
 * accumulates on LEARNING EVIDENCE (context-distant reappearance or parent
 * confirmation). Engagement alone NEVER increments confidence. This is the
 * structural form of "optimize the teaching, never the engagement" (Genotype
 * Hard Line 10) — the rule lives in the accumulator's call contract, not in a
 * UI layer that could be bypassed.
 *
 * [status] never renders as a number anywhere — Hard Line 6 (broadened, Epigenome
 * 022): banded language only ("exploring" / "getting it" / "owns it"), on every
 * surface, including the parent's. The numeric [confidence] exists only as the
 * accumulator's internal bookkeeping; no UI may read it directly (see
 * `:parent-suite-app` Map — it renders [status], never [confidence]).
 */
@Serializable
data class GroundingRecordSnapshot(
    val concept: String,
    val confidence: Double = 0.0,
    val firstExposed: Double = 0.0,
    val contextDistances: List<Double> = emptyList(),
    val seenContexts: List<String> = emptyList(),
    val parentConfirmations: Int = 0,
    val status: String = "exploring",
)

enum class GroundingStatus(val wireValue: String) {
    EXPLORING("exploring"),
    GETTING_IT("getting_it"),
    OWNS_IT("owns_it");

    companion object {
        fun fromWireValue(value: String): GroundingStatus =
            entries.firstOrNull { it.wireValue == value } ?: EXPLORING
    }
}

class GroundingRecord(
    val concept: String,
    var confidence: Double = 0.0,
    var firstExposed: Double = 0.0,
    val contextDistances: MutableList<Double> = mutableListOf(),
    val seenContexts: MutableList<String> = mutableListOf(),
    var parentConfirmations: Int = 0,
    var status: GroundingStatus = GroundingStatus.EXPLORING,
) {
    fun toSnapshot(): GroundingRecordSnapshot = GroundingRecordSnapshot(
        concept = concept,
        confidence = confidence,
        firstExposed = firstExposed,
        contextDistances = contextDistances.toList(),
        seenContexts = seenContexts.toList(),
        parentConfirmations = parentConfirmations,
        status = status.wireValue,
    )

    companion object {
        fun fromSnapshot(d: GroundingRecordSnapshot): GroundingRecord = GroundingRecord(
            concept = d.concept,
            confidence = d.confidence,
            firstExposed = d.firstExposed,
            contextDistances = d.contextDistances.toMutableList(),
            seenContexts = d.seenContexts.toMutableList(),
            parentConfirmations = d.parentConfirmations,
            status = GroundingStatus.fromWireValue(d.status),
        )
    }
}

/**
 * Simple context-distance heuristic — direct port, including the original's
 * "full semantic distance is future work" caveat (heuristic #7: name the limitation,
 * don't dress it up as more than it is).
 *   - Same episode context  -> near zero (an echo, not evidence)
 *   - First reappearance    -> small gain
 *   - Genuinely new context -> substantial gain (real transfer)
 */
internal fun computeContextDistance(seenContexts: List<String>, newContext: String): Double = when {
    seenContexts.isEmpty() -> 0.2
    newContext in seenContexts -> 0.02
    else -> 0.35
}

class GroundingAccumulator(private val config: PrismConfig) {
    private val records: LinkedHashMap<String, GroundingRecord> = LinkedHashMap()

    /** First exposure: open a record and hold confidence in suspension. */
    fun recordExposure(concept: String, nowEpochSeconds: Double = System.currentTimeMillis() / 1000.0) {
        records.getOrPut(concept) { GroundingRecord(concept = concept, firstExposed = nowEpochSeconds) }
    }

    /**
     * Call ONLY when there is learning evidence — a context-distant reappearance,
     * or a `model_revision` was present in the CIAER record. Never on
     * engagement-only events (the OGC hard rule, enforced at the call site in
     * [com.cappsconsulting.prism.engine.learninglog.LearningLog]).
     */
    fun recordReappearance(
        concept: String,
        newContext: String,
        hasModelRevision: Boolean = false,
        nowEpochSeconds: Double = System.currentTimeMillis() / 1000.0,
    ) {
        val rec = records.getOrPut(concept) { GroundingRecord(concept = concept, firstExposed = nowEpochSeconds) }

        var distance = computeContextDistance(rec.seenContexts, newContext)
        if (hasModelRevision) distance = kotlin.math.max(distance, 0.5)

        rec.confidence = min(1.0, rec.confidence + distance)
        rec.contextDistances.add(distance)
        if (newContext.isNotEmpty() && newContext !in rec.seenContexts) rec.seenContexts.add(newContext)

        rec.status = classify(rec.confidence)
    }

    /** Parent volunteers an outside-world independent instance. Strong, deliberate boost. */
    fun parentConfirm(concept: String, nowEpochSeconds: Double = System.currentTimeMillis() / 1000.0) {
        val rec = records.getOrPut(concept) { GroundingRecord(concept = concept, firstExposed = nowEpochSeconds) }
        rec.parentConfirmations += 1
        rec.confidence = min(1.0, rec.confidence + 0.4)
        rec.status = classify(rec.confidence)
    }

    private fun classify(confidence: Double): GroundingStatus = when {
        confidence < 0.3 -> GroundingStatus.EXPLORING
        confidence < 0.7 -> GroundingStatus.GETTING_IT
        else -> GroundingStatus.OWNS_IT
    }

    fun getAllRecords(): List<GroundingRecord> = records.values.toList()
    fun getRecord(concept: String): GroundingRecord? = records[concept]

    fun getSnapshot(): List<GroundingRecordSnapshot> = records.values.map { it.toSnapshot() }

    fun loadSnapshot(snapshot: List<GroundingRecordSnapshot>) {
        records.clear()
        for (d in snapshot) records[d.concept] = GroundingRecord.fromSnapshot(d)
    }
}
