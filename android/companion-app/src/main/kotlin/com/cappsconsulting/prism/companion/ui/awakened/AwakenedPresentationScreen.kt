package com.cappsconsulting.prism.companion.ui.awakened

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.cappsconsulting.prism.companion.presentation.PresentationState
import com.cappsconsulting.prism.companion.ui.PresentationCanvas

/**
 * Awakened mode — Doc 2.3 §2's "the Companion app's full-screen presentation,
 * **not** a 'viewfinder with overlay'" made literal: there is exactly one
 * thing on this screen, [PresentationCanvas] painting [presentation] edge to
 * edge. No glass box, no labels, no confidence bars, no border accent — every
 * element [com.cappsconsulting.prism.companion.ui.mechanical.MechanicalPresentationScreen]
 * layers on top of *its* canvas to teach a child how the fast brain *sees*
 * belongs to the let-down this screen is the other half of the contrast with.
 * The companion that emerged from Beat 5 doesn't show its work; it simply
 * *is* — [com.cappsconsulting.prism.companion.awakening.AwakeningChoreographer]'s
 * kdoc names the handoff precisely: "alive — exactly as before."
 *
 * The tap gesture is the one thing that survives the let-down/charge contrast
 * unchanged: "tap-to-look" means exactly the same thing in both modes (see
 * [com.cappsconsulting.prism.companion.orchestrator.CompanionOrchestrator.handleTapToLook]),
 * just answered, now, by someone who already knows the child's name.
 */
@Composable
fun AwakenedPresentationScreen(
    presentation: PresentationState,
    onTapToLook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnTapToLook by rememberUpdatedState(onTapToLook)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { currentOnTapToLook() }) },
    ) {
        PresentationCanvas(state = presentation, modifier = Modifier.fillMaxSize())
    }
}
