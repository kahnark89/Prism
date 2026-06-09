package com.cappsconsulting.prism.engine.config

/**
 * Tunable constants for the engine — direct port of `prism/config.py::PrismConfig`.
 *
 * Hardware/dashboard/persistence-path fields from the Python original are dropped
 * here on purpose: those describe the retired single-process design (Pi paths,
 * dashboard host/port, `use_mock_hal`). On Android each app supplies its own
 * platform config (data dir via `Context.filesDir`, etc. — see
 * `:companion-app`/`:parent-suite-app`); this class carries only the numbers that
 * shape the *behavior* of the differentiating engine, which is this module's job
 * to own verbatim.
 */
data class PrismConfig(
    // Inner Life Engine
    val lambdaHomeostasis: Double = 0.08,
    val noiseMagnitude: Double = 0.02,
    val noiseAutocorr: Double = 0.8,
    val rhythmDepth: Double = 1.0,
    val couplingScale: Double = 1.0,
    val eventKickDecay: Double = 0.6,
    val growthRatePerDay: Double = 0.012,
    val growthCap: Double = 0.18,

    // Memory Engine
    val tauBase: Double = 2.0,
    val tauSalienceScale: Double = 14.0,
    val salienceGate: Double = 0.35,
    val rehearsalKick: Double = 0.5,
    val consolidationFactor: Double = 0.35,
    val pruneFloor: Double = 0.05,
    val spreadingActivation: Double = 0.4,
    val codebookStrengthPerEncode: Double = 0.25,
    val codebookSalienceBonus: Double = 0.2,

    // Perspective / LLM
    val llmProvider: String = "anthropic",
    val llmModel: String = "claude-haiku-4-5-20251001",
    val llmTimeoutS: Double = 8.0,
    val offlineFallbackEnabled: Boolean = true,

    // Safety
    val safetyBlockedPatterns: List<String> = emptyList(),

    // Circadian
    val wakeHour: Double = 7.0,
    val napHour: Double = 13.0,
    val bedHour: Double = 19.5,

    // Companion / child
    val activeCompanion: String = "pip",
    val childName: String = "Naomi",

    // Orchestrator timing (seconds)
    val tickIntervalS: Double = 5.0,
    val decayIntervalS: Double = 1800.0,
    val saveIntervalS: Double = 120.0,
    val idleTimeoutS: Double = 180.0,
)
