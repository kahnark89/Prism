package com.cappsconsulting.prism.engine.personas

/**
 * Direct port of `prism/personas/base.py`.
 *
 * One persona = one of the "recurring characters, each a different lens" (Genotype
 * Design Principle 2 — multiple points of view). `signatureHue`/`signatureSaturation`
 * feed the awakening choreography and the Companion's resting presentation
 * (Doc 2.3 §3 "the screen eases down... in the companion's signature color");
 * `voiceRate`/`voicePitch` feed Android `TextToSpeech` parameters; `fallbackPhrases`
 * are what the offline "fast brain" speaks when the cloud "smart brain" is
 * unreachable (Architecture invariant — "degrades gracefully offline").
 */
data class CompanionPersona(
    val id: String,
    val name: String,
    val displayName: String,
    val lens: String,
    val strength: String,
    val dilemma: String,
    val lifeLesson: String,

    // L0 trait baselines — exact values from the simulator / Python persona modules
    val baseM: Double,
    val baseE: Double,
    val baseC: Double,
    val baseA: Double,
    val baseS: Double,

    // TTS voice profile (Android TextToSpeech: setSpeechRate / setPitch)
    val voiceRate: Int = 150,
    val voicePitch: Double = 1.0,
    val voiceId: String = "",

    // Signature color (HSL) — drives the awakening bloom and resting presentation
    val signatureHue: Double = 38.0,
    val signatureSaturation: Double = 70.0,

    // Mood-line vocabulary (band -> phrase)
    val energyLo: String = "sleepy and slow",
    val energyMid: String = "easygoing",
    val energyHi: String = "bright and bouncy",
    val moodLo: String = "a little grumpy (but still kind)",
    val moodMid: String = "content",
    val moodHi: String = "joyful",
    val curiosityLo: String = "mellow, savoring one thing at a time",
    val curiosityMid: String = "gently curious",
    val curiosityHi: String = "fascinated and full of questions",

    // Offline fallback phrases keyed by energy band ("lo" | "mid" | "hi")
    val fallbackPhrases: Map<String, List<String>> = emptyMap(),

    // System prompt header for the cloud "smart brain"
    val systemPreamble: String = "",
)
