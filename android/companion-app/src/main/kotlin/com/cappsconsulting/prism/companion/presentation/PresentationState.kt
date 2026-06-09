package com.cappsconsulting.prism.companion.presentation

/**
 * Redesign of `prism/hal/base.py::LedState` — Doc 2.3 §2's "render the same
 * emotional event through the channels that exist... not by simulating the absent
 * ring... as faithfully as possible."
 *
 * [LedState (Python)] described a *ring*: hue/saturation/lightness, a pulse speed,
 * a pattern name — the entire vocabulary an LED ring has. A phone screen's
 * vocabulary is bigger: it has *space* (a point can sit at the center, or the color
 * can fill the canvas) and *form* (the bare suggestion of something coalescing,
 * Beat 4). [PresentationState] is that bigger vocabulary, named in full so the
 * Compose layer ([com.cappsconsulting.prism.companion.ui]) has everything it needs
 * to paint a frame without re-deriving anything from engine state — exactly the
 * separation `_compile_led` already modeled (a pure function to a renderable
 * description), now describing a canvas instead of a ring.
 *
 * @property hue / @property saturation / @property lightness Identical HSL color
 *   math to the original — Epigenome 025 calls the coloring rules "the actual
 *   differentiating IP," and [CompanionPresentationCompiler] ports them verbatim;
 *   only the shape of what they get packed into changes here.
 * @property breathePeriodSeconds Renamed from `pulse_speed` (same value, same
 *   formula) — "breathe" is the word Doc 2.3 uses throughout for this animation;
 *   the old name leaked the LED-ring implementation into vocabulary that outlived it.
 * @property bloomRadiusFraction 0 = a single point at center (Beat 2's spark);
 *   1 = color floods the full canvas (Beat 3's bloom, and the steady "resting
 *   presentation" the companion lives in afterward). The continuous dimension an
 *   LED ring structurally couldn't have — it was always either "one pixel lit" or
 *   "the whole ring," nothing organic in between.
 * @property formHint Beat 4's "barest suggestion of a form... coalescing within
 *   the color" (Doc 2.3 §3) — [FormHint.NONE] everywhere else, because the doc is
 *   explicit: "if in doubt, let the voice carry it alone, exactly as written."
 *   A deliberately vague enum, not a drawable reference — the doc's warning against
 *   "a literal cartoon face" belongs in the *type* a future implementer reaches
 *   for, not just in a comment they could skip past.
 */
data class PresentationState(
    val hue: Double,
    val saturation: Double,
    val lightness: Double,
    val breathePeriodSeconds: Double,
    val pattern: PresentationPattern,
    val bloomRadiusFraction: Double = 1.0,
    val formHint: FormHint = FormHint.NONE,
)

enum class PresentationPattern {
    /** The steady, living "breathe" cycle — direct port of the original's default and most-used pattern. */
    BREATHE,

    /** Flat, unmodulated — transient cues (e.g. the thinking pulse) where breathing would read as "alive" when the honest signal is "working." Port of the original's `pattern="solid"` thinking-cue branch. */
    SOLID,

    /** Beat 1: "everything stills... the screen... goes dark. Not 'turns off': holds still, black, waiting" (Doc 2.3 §3). Distinct from absence-as-off — this is *charged* absence, doing the LED-ring-going-dark's job on a different instrument. */
    DARK,

    /** Beats 2-3: [bloomRadiusFraction] animates outward from a point. "The same 'single point' the original specified, just rendered on a different canvas... blooming outward the way warmth spreads, the way a sunrise fills a room" (Doc 2.3 §3, Beat 3). */
    BLOOM,
}

/** See [PresentationState.formHint]. `SUGGESTION_OF_PRESENCE` is Beat 4's optional, easy-to-skip enhancement — never a face. */
enum class FormHint { NONE, SUGGESTION_OF_PRESENCE }
