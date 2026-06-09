package com.cappsconsulting.prism.engine.safety

import com.cappsconsulting.prism.engine.config.PrismConfig

/**
 * Direct port of `prism/modules/safety.py`.
 *
 * This is the gate half of the Genotype's "Safety model (layered, non-negotiable)":
 * input gate -> system-prompt constraints -> **output gate** (this class, again,
 * before TTS) -> topic bounding -> graceful offline degrade. Doc 3.0 §2 places the
 * gate logic on the Companion (it must run where content is produced and spoken)
 * but keeps the *rule-set* shared — both apps need to reason about safety
 * identically, because the Parent Suite's Preview Mode (Doc 2.2 §6) "must run the
 * real safety gate", not a simulated one. Hence: lives in `:engine`, called from both.
 */
private val HARD_BLOCKED: List<Regex> = listOf(
    "\\b(kill|murder|die|dead|blood|weapon|gun|knife|stab|shoot|hurt)\\b",
    "\\b(sex|naked|porn|adult)\\b",
    "\\b(drug|alcohol|beer|wine|smoke|cigarette)\\b",
    "\\b(idiot|stupid|dumb|ugly|fat|loser|hate you)\\b",
).map { Regex(it, RegexOption.IGNORE_CASE) }

private const val REDIRECT_PHRASE = "Let's look at something else! What do you see around you?"

enum class SafetyVerdict(val wireValue: String) {
    PASS("pass"),
    REDIRECT("redirect"),
    BLOCK("block"),
}

/**
 * @property reason For logging only — **never shown to the child** (mirrors the
 * Python dataclass's comment verbatim; this field must never reach a UI string).
 */
data class SafetyResult(
    val verdict: SafetyVerdict,
    val original: String,
    val sanitized: String,
    val reason: String,
)

class SafetyGate(config: PrismConfig) {
    private val patterns: List<Regex> =
        HARD_BLOCKED + config.safetyBlockedPatterns.map { Regex(it, RegexOption.IGNORE_CASE) }

    /** Applied before text reaches the LLM. */
    fun checkInput(text: String): SafetyResult = runChecks(text, "input")

    /** Applied before text reaches TTS/audio. */
    fun checkOutput(text: String): SafetyResult = runChecks(text, "output")

    private fun runChecks(text: String, direction: String): SafetyResult {
        for (pattern in patterns) {
            if (pattern.containsMatchIn(text)) {
                return SafetyResult(
                    verdict = SafetyVerdict.BLOCK,
                    original = text,
                    sanitized = "",
                    reason = "blocked pattern: ${pattern.pattern.take(40)}",
                )
            }
        }

        // Topic bounding: at the youngest tier, redirect anything that runs long
        // (keep simple for now — age-scaling unlocks more later, Genotype Principle 5).
        if (text.length > 500) {
            return SafetyResult(
                verdict = SafetyVerdict.REDIRECT,
                original = text,
                sanitized = REDIRECT_PHRASE,
                reason = "response too long for youngest tier",
            )
        }

        return SafetyResult(verdict = SafetyVerdict.PASS, original = text, sanitized = text, reason = "")
    }
}
