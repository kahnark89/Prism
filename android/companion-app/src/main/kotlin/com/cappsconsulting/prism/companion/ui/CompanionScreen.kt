package com.cappsconsulting.prism.companion.ui

import android.util.Log
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.cappsconsulting.prism.companion.awakening.AwakeningState
import com.cappsconsulting.prism.companion.orchestrator.CompanionOrchestrator
import com.cappsconsulting.prism.companion.ui.awakened.AwakenedPresentationScreen
import com.cappsconsulting.prism.companion.ui.mechanical.MechanicalPresentationScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val TAG = "CompanionScreen"

/**
 * The top-level screen switcher — Compose's side of the "let-down vs. charge"
 * contrast Doc 2.3 §1 names as the choreography's whole emotional premise,
 * made *structural*: not one screen that conditionally dresses itself up, but
 * two genuinely different screens, swapped the instant
 * [CompanionOrchestrator.awakeningState] flips. That instant is exactly what
 * a plain getter on [com.cappsconsulting.prism.companion.awakening.AwakeningMachine]
 * couldn't give this layer — see that `StateFlow`'s own kdoc for why watching
 * it from outside is what turned it from a bare attribute into one.
 *
 * The third state, [AwakeningState.AWAKENING], gets neither child screen: for
 * those five beats, [CompanionOrchestrator.presentation] *is* the choreographer's
 * timeline — [CompanionOrchestrator] re-points that very `StateFlow` at
 * [com.cappsconsulting.prism.companion.awakening.AwakeningChoreographer]'s
 * frames for the sequence's duration — and Doc 2.3 §1 is emphatic the whole
 * thing plays as "one event." So this layer paints the bare [PresentationCanvas]
 * and offers *no* tap affordance: the Compose equivalent of the original
 * holding `_state == AWAKENING` as a guard against re-entry. Letting a curious
 * tap launch a second [CompanionOrchestrator.handleTapToLook] pipeline
 * mid-sequence — competing speech, competing pipeline-driven frames fighting
 * the choreographer for the same `StateFlow` — would be a UI-layer bug the
 * original's single-threaded asyncio loop never had a chance to introduce.
 */
@Composable
fun CompanionScreen(
    orchestrator: CompanionOrchestrator,
    cameraPreviewView: PreviewView? = null,
    modifier: Modifier = Modifier,
) {
    val awakeningState by orchestrator.awakeningState.collectAsState()
    val presentation by orchestrator.presentation.collectAsState()
    val lastReadout by orchestrator.lastReadout.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val onTapToLook: () -> Unit = {
        coroutineScope.launch {
            try {
                orchestrator.handleTapToLook()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "[tap-to-look] pipeline failed", e)
            }
        }
    }

    when (awakeningState) {
        AwakeningState.MECHANICAL -> MechanicalPresentationScreen(
            presentation = presentation,
            lastReadout = lastReadout,
            onTapToLook = onTapToLook,
            cameraPreviewView = cameraPreviewView,
            modifier = modifier,
        )

        AwakeningState.AWAKENING -> PresentationCanvas(
            state = presentation,
            modifier = modifier.fillMaxSize(),
        )

        AwakeningState.AWAKENED -> AwakenedPresentationScreen(
            presentation = presentation,
            onTapToLook = onTapToLook,
            modifier = modifier,
        )
    }
}
