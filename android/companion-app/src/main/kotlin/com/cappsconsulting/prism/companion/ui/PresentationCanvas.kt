package com.cappsconsulting.prism.companion.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.cappsconsulting.prism.companion.presentation.FormHint
import com.cappsconsulting.prism.companion.presentation.PresentationPattern
import com.cappsconsulting.prism.companion.presentation.PresentationState
import kotlin.math.sqrt

/**
 * Converts [PresentationState]'s HSL triple to a renderable [Color] — the one
 * piece of arithmetic every layer in this file shares. [PresentationState.saturation]
 * and [PresentationState.lightness] are percentages in `[0, 100]` (ported
 * verbatim from the original LED math — see that class's kdoc on why "the
 * coloring rules" are "the actual differentiating IP"); `android.graphics.Color.HSLToColor`
 * wants fractions in `[0, 1]`, hence the `/ 100.0`. Small enough to inline at
 * each call site; named once here so the *conversion* — not its call sites —
 * is the thing a reviewer checks against the original's formula.
 */
private fun PresentationState.toColor(): Color {
    val hsl = floatArrayOf(hue.toFloat(), (saturation / 100.0).toFloat(), (lightness / 100.0).toFloat())
    return Color(android.graphics.Color.HSLToColor(hsl))
}

/**
 * The full-canvas renderer — [PresentationState] turned into pixels, frame by
 * frame. This *is* the redesign's answer to "what replaced the LED ring": not
 * a ring simulacrum, but a canvas whose color, radius, and motion continuously
 * animate *toward* whatever [state] currently asks for — Doc 2.3 §2's "render
 * the same emotional event through the channels that exist," made literal.
 * [com.cappsconsulting.prism.companion.ui.awakened.AwakenedPresentationScreen]
 * is this composable and nothing else, full screen — Doc 2.3 §2's "not a
 * 'viewfinder with overlay'" — and during the awakening sequence itself, this
 * is what paints [com.cappsconsulting.prism.companion.awakening.AwakeningChoreographer]'s
 * five beats, frame for frame, off the very same `StateFlow`.
 *
 * Three layers, composited in order, each independently animated:
 *  1. A black base — [PresentationPattern.DARK]'s "holds still, black,
 *     waiting" is the canvas's resting truth; everything else blooms *out of*
 *     it, never replaces it outright.
 *  2. A radial-gradient glow, its radius driven by [PresentationState.bloomRadiusFraction]
 *     against the *canvas diagonal* — not the inscribed circle — so `1.0`
 *     convincingly floods every corner the way Beat 3's "sunrise fills a
 *     room" asks for, and colored by the verbatim HSL math above.
 *  3. [FormHint.SUGGESTION_OF_PRESENCE]'s "barest suggestion of a form
 *     coalescing" (Doc 2.3 §3, Beat 4) — a second, smaller, softer, off-center
 *     glow. Doc 2.3 is emphatic this must never become "a literal cartoon
 *     face," and the only durable way to keep an implementation from drifting
 *     toward one over successive edits is to give it nothing face-shaped to
 *     start from: no eyes, no symmetry-about-a-center-line, just warmth
 *     gathering slightly off to one side.
 *
 * Only [PresentationPattern.BREATHE] breathes — see [PresentationPattern.SOLID]'s
 * kdoc ("breathing would read as *alive* when the honest signal is *working*")
 * for why every other pattern holds its scale steady instead of almost-but-not
 * -quite breathing along beside it. (Conditionally *calling* [androidx.compose.animation.core.InfiniteTransition.animateFloat]
 * would break Compose's composition rules; degenerating its endpoints to a
 * constant `1f` is what keeps the call unconditional *and* the result inert.)
 */
@Composable
fun PresentationCanvas(state: PresentationState, modifier: Modifier = Modifier) {
    val color by animateColorAsState(
        targetValue = state.toColor(),
        animationSpec = tween(durationMillis = 400),
        label = "presentationColor",
    )
    val bloom by animateFloatAsState(
        targetValue = state.bloomRadiusFraction.toFloat(),
        animationSpec = tween(durationMillis = 400),
        label = "bloomRadiusFraction",
    )

    val breathes = state.pattern == PresentationPattern.BREATHE
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val breath by infiniteTransition.animateFloat(
        initialValue = if (breathes) 0.88f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (state.breathePeriodSeconds * 1000).toInt().coerceAtLeast(1)),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = Color.Black)

        if (state.pattern != PresentationPattern.DARK) {
            val maxRadius = sqrt(size.width * size.width + size.height * size.height) / 2f
            val radius = (maxRadius * bloom * breath).coerceAtLeast(1f)
            val glowCenter = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color, color.copy(alpha = 0f)),
                    center = glowCenter,
                    radius = radius,
                ),
                radius = radius,
                center = glowCenter,
            )

            if (state.formHint == FormHint.SUGGESTION_OF_PRESENCE) {
                val formRadius = radius * 0.3f
                val formCenter = Offset(glowCenter.x, glowCenter.y - radius * 0.18f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0f)),
                        center = formCenter,
                        radius = formRadius,
                    ),
                    radius = formRadius,
                    center = formCenter,
                )
            }
        }
    }
}

/**
 * [com.cappsconsulting.prism.companion.ui.mechanical.MechanicalPresentationScreen]'s
 * one and only use of [PresentationState] — Doc 2.3 §3 Beat 1 is explicit that
 * the mechanical-mode screen shows "the glass-box camera view, labels,
 * confidence bars... Doc 1.6's deliberate let-down," not a color bloom; the
 * fast brain is supposed to *look* plain and utilitarian, not alive. But
 * [com.cappsconsulting.prism.companion.orchestrator.CompanionOrchestrator.presentation]
 * keeps ticking through mechanical mode too — [com.cappsconsulting.prism.companion.presentation.CompanionPresentationCompiler.compile]
 * never special-cases [com.cappsconsulting.prism.companion.presentation.PresentationMode.MECHANICAL]
 * (the mood math runs identically in both modes; only what gets *built on top*
 * of it differs) — and silently dropping a continuously-true signal because
 * the primary view has no place for it would be its own quiet dishonesty.
 *
 * So: a thin, soft, mood-colored outline at the screen's edge, animated slowly
 * and nothing more — present enough to reward a curious child who notices "huh,
 * that frame just changed color," restrained enough to never compete with the
 * glass box it borders. An ambient accent, not a second presentation.
 */
@Composable
fun PresentationAccentBorder(state: PresentationState, modifier: Modifier = Modifier) {
    val color by animateColorAsState(
        targetValue = state.toColor(),
        animationSpec = tween(durationMillis = 600),
        label = "accentColor",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidth = size.minDimension * 0.025f
        val inset = strokeWidth / 2f
        drawRoundRect(
            color = color.copy(alpha = 0.45f),
            topLeft = Offset(inset, inset),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            cornerRadius = CornerRadius(strokeWidth * 2f),
            style = Stroke(width = strokeWidth),
        )
    }
}
