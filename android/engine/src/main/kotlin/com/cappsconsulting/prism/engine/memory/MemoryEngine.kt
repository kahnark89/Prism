package com.cappsconsulting.prism.engine.memory

import com.cappsconsulting.prism.engine.config.PrismConfig
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.math.exp
import kotlin.math.min

/**
 * Direct port of `prism/engines/memory.py`.
 *
 * Implements mp4Real's *philosophy* — discard-by-default, rate-distortion decay,
 * a compressed behavioral "codebook" — over a privacy-safe track set (Genotype
 * §"Schema & memory commitments"). This is "beats, not a transcript of a childhood"
 * (Hard Line 4) made into an actual exponential-decay mechanism: salient moments are
 * encoded with a strength and a per-node time-constant; everything below the prune
 * floor is forgotten, by construction, not by policy.
 */
@Serializable
data class MemoryNodeSnapshot(
    val id: String,
    val concept: String,
    val episode: String,
    val salience: Double,
    val s: Double,
    val tau: Double,
    val rehearsals: Int,
    val lastDay: Double,
    val contexts: List<String>,
    val createdDay: Double,
)

/** A single encoded episodic memory — "snail on the path", not a transcript. */
class MemoryNode(
    val id: String,
    val concept: String,
    val episode: String,
    val salience: Double,
    var s: Double,
    var tau: Double,
    var rehearsals: Int,
    var lastDay: Double,
    val contexts: MutableList<String>,
    val createdDay: Double,
) {
    fun toSnapshot(): MemoryNodeSnapshot = MemoryNodeSnapshot(
        id = id, concept = concept, episode = episode, salience = salience,
        s = s, tau = tau, rehearsals = rehearsals, lastDay = lastDay,
        contexts = contexts.toList(), createdDay = createdDay,
    )

    companion object {
        fun fromSnapshot(d: MemoryNodeSnapshot): MemoryNode = MemoryNode(
            id = d.id, concept = d.concept, episode = d.episode, salience = d.salience,
            s = d.s, tau = d.tau, rehearsals = d.rehearsals, lastDay = d.lastDay,
            contexts = d.contexts.toMutableList(), createdDay = d.createdDay,
        )
    }
}

@Serializable
data class CodebookEntrySnapshot(val concept: String, val strength: Double, val count: Int)

/** Compressed gist — "the behavioral codebook" (mp4Real philosophy), survives longer than episodes. */
class CodebookEntry(val concept: String, var strength: Double, var count: Int) {
    fun toSnapshot() = CodebookEntrySnapshot(concept, strength, count)
}

class MemoryEngine(private val config: PrismConfig) {
    private val nodes: MutableList<MemoryNode> = mutableListOf()
    private val codebook: LinkedHashMap<String, CodebookEntry> = LinkedHashMap()
    private var currentDay: Double = 0.0

    fun currentDay(): Double = currentDay
    fun setDay(day: Double) { currentDay = day }

    /**
     * Salience gate: below [PrismConfig.salienceGate], discard immediately — return
     * null. Otherwise encode a new episodic node and run spreading activation across
     * existing same-concept nodes (each reappearance strengthens and slows the decay
     * of everything already known about that concept, plus the new instance).
     */
    fun encode(concept: String, episode: String, salience: Double, contextTag: String = ""): MemoryNode? {
        val cfg = config
        if (salience < cfg.salienceGate) return null

        val tau = cfg.tauBase + cfg.tauSalienceScale * salience
        val sInit = min(1.0, kotlin.math.max(0.0, 0.55 + salience * 0.45))

        for (node in nodes) {
            if (node.concept == concept) {
                node.s = min(1.0, node.s + cfg.rehearsalKick)
                node.tau *= (1.0 + cfg.consolidationFactor)
                node.rehearsals += 1
                node.lastDay = currentDay
                val tag = episode.ifEmpty { contextTag }
                if (tag.isNotEmpty() && tag !in node.contexts) node.contexts.add(tag)
            }
        }

        val node = MemoryNode(
            id = UUID.randomUUID().toString(),
            concept = concept,
            episode = episode,
            salience = salience,
            s = sInit,
            tau = tau,
            rehearsals = 0,
            lastDay = currentDay,
            contexts = if (episode.isNotEmpty()) mutableListOf(episode) else mutableListOf(),
            createdDay = currentDay,
        )
        nodes.add(node)

        val entry = codebook.getOrPut(concept) { CodebookEntry(concept = concept, strength = 0.0, count = 0) }
        entry.strength = min(1.0, entry.strength + cfg.codebookStrengthPerEncode + salience * cfg.codebookSalienceBonus)
        entry.count += 1

        return node
    }

    /** Manually reactivate a node (the companion references a memory aloud). */
    fun rehearse(nodeId: String, contextTag: String = "") {
        val cfg = config
        for (node in nodes) {
            if (node.id == nodeId) {
                node.s = min(1.0, node.s + cfg.rehearsalKick)
                node.tau *= (1.0 + cfg.consolidationFactor)
                node.rehearsals += 1
                node.lastDay = currentDay
                if (contextTag.isNotEmpty() && contextTag !in node.contexts) node.contexts.add(contextTag)
                return
            }
        }
    }

    /**
     * Decay every node by `s *= exp(-dt/tau)`; prune anything below the floor.
     * Returns the IDs of pruned nodes — "discard-by-default", the mechanism Hard
     * Line 4 requires, not a retention policy bolted on after the fact.
     */
    fun advanceTime(newDay: Double): List<String> {
        val cfg = config
        val dt = newDay - currentDay
        if (dt <= 0) {
            currentDay = newDay
            return emptyList()
        }

        val prunedIds = mutableListOf<String>()
        val surviving = mutableListOf<MemoryNode>()
        for (node in nodes) {
            node.s *= exp(-dt / node.tau)
            if (node.s < cfg.pruneFloor) prunedIds.add(node.id) else surviving.add(node)
        }
        nodes.clear()
        nodes.addAll(surviving)

        // Codebook (compressed gist) decays 12x slower than episodes — the
        // mp4Real-philosophy split between "what happened" and "what she's like".
        val cbDecayTau = cfg.tauBase * 12.0
        for (entry in codebook.values) {
            entry.strength *= exp(-dt / cbDecayTau)
        }

        currentDay = newDay
        return prunedIds
    }

    /** Top-k currently-activated nodes, optionally biased toward [conceptHint] via spreading activation. */
    fun getTopActivated(conceptHint: String = "", k: Int = 3): List<MemoryNode> {
        if (nodes.isEmpty()) return emptyList()
        val cfg = config
        return nodes
            .map { node ->
                var score = node.s
                if (conceptHint.isNotEmpty() && node.concept == conceptHint) score += cfg.spreadingActivation
                score to node
            }
            .sortedByDescending { it.first }
            .take(k)
            .map { it.second }
    }

    /** All distinct episode contexts ever recorded for this concept (order of first appearance). */
    fun getContextsForConcept(concept: String): List<String> {
        val seen = LinkedHashSet<String>()
        for (node in nodes) {
            if (node.concept == concept) seen.addAll(node.contexts)
        }
        return seen.toList()
    }

    /** Sum of `s * salience` across all nodes, normalized to [0,1] — feeds [InnerLifeEngine.applyAffectionBoost]. */
    fun getAffectionMass(): Double {
        if (nodes.isEmpty()) return 0.0
        val total = nodes.sumOf { it.s * it.salience }
        return min(1.0, total / kotlin.math.max(1, nodes.size))
    }

    fun getCodebook(): Map<String, CodebookEntry> = codebook.toMap()
    fun getAllNodes(): List<MemoryNode> = nodes.toList()

    fun getSnapshot(): Pair<List<MemoryNodeSnapshot>, Map<String, CodebookEntrySnapshot>> =
        nodes.map { it.toSnapshot() } to codebook.mapValues { it.value.toSnapshot() }

    fun loadSnapshot(nodeSnapshots: List<MemoryNodeSnapshot>, codebookSnapshot: Map<String, CodebookEntrySnapshot>) {
        nodes.clear()
        nodes.addAll(nodeSnapshots.map { MemoryNode.fromSnapshot(it) })
        codebook.clear()
        for ((k, v) in codebookSnapshot) {
            codebook[k] = CodebookEntry(v.concept, v.strength, v.count)
        }
    }
}
