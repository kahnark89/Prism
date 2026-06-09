package com.cappsconsulting.prism.companion.presentation

import com.cappsconsulting.prism.engine.innerlife.InnerLifeState
import com.cappsconsulting.prism.engine.personas.CompanionPersona
import kotlin.math.min

/** Direct port of `ui_controller.py::UIMode` — the same four presentation modes, same names, renamed only because there's no more LED to drive. */
enum class PresentationMode { MECHANICAL, AWAKENED, THINKING, SLEEPING }

/**
 * Redesign of `ui_controller.py::UIController._compile_led` — the pure
 * `(state, persona, mode) -> renderable description` function — now compiling to
 * [PresentationState] instead of `LedState`, plus the Beat 1/2/3 frames the
 * original produced procedurally inside `awakening_bloom()`/by direct LED writes
 * (Doc 2.3 needs these *named and reusable* because a screen bloom is a continuous
 * animation Compose drives frame-by-frame, not a seven-step `for` loop of discrete
 * brightness values).
 *
 * **The coloring math below is ported verbatim** — same branch on `E < 0.35`, same
 * coefficients on `M`, same thinking-mode saturation boost, same caps at 100. Per
 * Epigenome 025 these numbers — not their packaging — are "the actual differentiating
 * IP," lifted from `inner_life_simulator.jsx::ledStyle()` originally; the redesign
 * changes only the shape of what they get packed into (a canvas description, built
 * to be *animated toward*, rather than a ring description meant to be set instantly).
 */
object CompanionPresentationCompiler {

    /** Steady-state presentation — the direct replacement for `_compile_led`, called every tick exactly as the original was. */
    fun compile(state: InnerLifeState, persona: CompanionPersona, mode: PresentationMode): PresentationState {
        if (mode == PresentationMode.SLEEPING) {
            return PresentationState(
                hue = 205.0, saturation = 30.0, lightness = 15.0,
                breathePeriodSeconds = 3.0, pattern = PresentationPattern.BREATHE, bloomRadiusFraction = 0.5,
            )
        }

        val breathePeriod = 0.6 + state.e * 1.8

        val hue: Double
        val sat: Double
        val lit: Double
        if (state.e < 0.35) {
            hue = 205.0
            sat = 55.0
            lit = 45.0 + state.m * 12.0
        } else {
            hue = persona.signatureHue + state.m * 14.0
            sat = persona.signatureSaturation + state.m * 20.0
            lit = 42.0 + state.m * 20.0
        }

        val boostedSat = if (mode == PresentationMode.THINKING) sat + 20.0 else sat

        return PresentationState(
            hue = hue,
            saturation = min(100.0, boostedSat),
            lightness = min(100.0, lit),
            breathePeriodSeconds = breathePeriod,
            pattern = if (mode == PresentationMode.THINKING) PresentationPattern.SOLID else PresentationPattern.BREATHE,
        )
    }

    /** Beat 1 — "everything stills... the screen... goes dark... holds still, black, waiting" (Doc 2.3 §3). */
    fun pause(): PresentationState = PresentationState(
        hue = 0.0, saturation = 0.0, lightness = 0.0,
        breathePeriodSeconds = 0.0, pattern = PresentationPattern.DARK, bloomRadiusFraction = 0.0,
    )

    /** Beat 2 — "a single point of light blooms into existence at the center of the dark screen — small, soft-edged, barely brighter than the blackness" (Doc 2.3 §3). */
    fun spark(persona: CompanionPersona): PresentationState = PresentationState(
        hue = persona.signatureHue,
        saturation = persona.signatureSaturation * 0.5,
        lightness = 10.0,
        breathePeriodSeconds = 2.0,
        pattern = PresentationPattern.BLOOM,
        bloomRadiusFraction = 0.015,
    )

    /**
     * Beat 3 at [progress] in `[0, 1]` — "the point of light expands outward from
     * center, flooding the full screen with the companion's signature warm color...
     * the way a sunrise fills a room" (Doc 2.3 §3). [progress] is owned by the
     * choreographer's animation clock, not this compiler — keeping this a pure
     * function of (persona, progress) is what makes it independently previewable
     * and testable, the same reason `_compile_led` was kept pure.
     */
    fun bloom(persona: CompanionPersona, progress: Double): PresentationState {
        val p = progress.coerceIn(0.0, 1.0)
        return PresentationState(
            hue = persona.signatureHue,
            saturation = persona.signatureSaturation,
            lightness = 28.0 + 62.0 * p,
            breathePeriodSeconds = 0.6,
            pattern = PresentationPattern.BLOOM,
            bloomRadiusFraction = p,
            // Beat 4 "if it earns its place": only ever offered late in the bloom, easy to ignore.
            formHint = if (p > 0.85) FormHint.SUGGESTION_OF_PRESENCE else FormHint.NONE,
        )
    }

    /** Fast pulse while awaiting the smart brain — direct port of `UIController.thinking_cue`'s two-tone alternation, expressed as endpoints for the choreographer to animate between. */
    fun thinkingCueFrames(): Pair<PresentationState, PresentationState> = Pair(
        PresentationState(hue = 200.0, saturation = 80.0, lightness = 70.0, breathePeriodSeconds = 0.2, pattern = PresentationPattern.SOLID),
        PresentationState(hue = 200.0, saturation = 40.0, lightness = 30.0, breathePeriodSeconds = 0.2, pattern = PresentationPattern.SOLID),
    )
}
