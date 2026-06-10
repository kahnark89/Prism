package com.cappsconsulting.prism.companion.ui.mechanical

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.cappsconsulting.prism.companion.orchestrator.VisionReadout
import com.cappsconsulting.prism.companion.presentation.PresentationState
import com.cappsconsulting.prism.companion.ui.PresentationAccentBorder
import com.cappsconsulting.prism.engine.perspective.PerspectiveEngine
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

// CameraViewfinder used when no orchestrator-wired PreviewView is available (standalone mode)

/**
 * Mechanical mode, painted exactly as Doc 2.3 §3 Beat 1 describes the screen
 * its awakening interrupts: "the glass-box camera view, labels, confidence
 * bars — Doc 1.6's deliberate let-down." Three layers, stacked in the order a
 * child's eye would actually meet them:
 *
 *  1. [CameraViewfinder] — the literal live feed, full-bleed. Not a metaphor
 *     for "the fast brain is looking": the fast brain *is* looking, and
 *     showing a child exactly what a camera sees — not an artist's idea of
 *     what a camera sees — is the let-down's entire honesty.
 *  2. [PresentationAccentBorder] — [presentation] keeps ticking in this mode
 *     too (see that composable's kdoc for why); a thin mood-colored frame is
 *     the truthful amount of attention to give that signal here, present
 *     without performing.
 *  3. [GlassBoxOverlay] — [lastReadout] rendered as the label-and-confidence-bar
 *     pairing that Doc 1.6 §5 calls the AI-literacy curriculum *itself*:
 *     "Being wrong becomes the fun part... where AI-literacy and emotional
 *     safety become the same thing." A "just guessing" reading isn't a flaw
 *     being surfaced apologetically; it's the lesson, displayed plainly —
 *     as a bar and banded words, never a numeral (Hard Line 6, ratified
 *     absolute: Epigenome 029).
 *
 * The tap gesture spans the whole screen — "the shutter button -> the tap
 * gesture," named in [com.cappsconsulting.prism.companion.orchestrator.CompanionOrchestrator]'s
 * kdoc — landing on [onTapToLook] exactly where `register_button_callback`
 * once registered its handler.
 */
/**
 * [cameraPreviewView]: when the full orchestrator is wired up, [CameraXSource] owns
 * the camera binding and exposes its [PreviewView] here — `AndroidView` mounts it
 * directly. When null (standalone, no orchestrator), [CameraViewfinder] falls back
 * to its own [ProcessCameraProvider] binding for the preview. The two paths diverge
 * only in how the viewfinder is sourced; everything above it (tap gesture, accent
 * border, glass-box overlay) is identical in both cases.
 */
@Composable
fun MechanicalPresentationScreen(
    presentation: PresentationState,
    lastReadout: VisionReadout?,
    onTapToLook: () -> Unit,
    onAdminGesture: (() -> Unit)? = null,
    cameraPreviewView: PreviewView? = null,
    modifier: Modifier = Modifier,
) {
    val currentOnTapToLook by rememberUpdatedState(onTapToLook)
    val currentOnAdmin by rememberUpdatedState(onAdminGesture)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { currentOnTapToLook() },
                    onLongPress = { currentOnAdmin?.invoke() },
                )
            },
    ) {
        if (cameraPreviewView != null) {
            AndroidView(factory = { cameraPreviewView }, modifier = Modifier.fillMaxSize())
        } else {
            CameraViewfinder(modifier = Modifier.fillMaxSize())
        }
        PresentationAccentBorder(state = presentation, modifier = Modifier.fillMaxSize())
        GlassBoxOverlay(readout = lastReadout, modifier = Modifier.align(Alignment.TopStart))
    }
}

/**
 * Bridges CameraX's `View`-based [PreviewView] into Compose via [AndroidView]
 * — a *second*, independent `Preview` use case standing beside
 * [com.cappsconsulting.prism.companion.hal.CameraSource]'s pull-based
 * `captureFrame()`, not a layer over it. That interface's own kdoc is explicit
 * it has no continuous stream; the live feed a child watches and the single
 * frame [com.cappsconsulting.prism.companion.orchestrator.CompanionOrchestrator.handleTapToLook]
 * captures are siblings sharing one [ProcessCameraProvider], exactly the way
 * CameraX expects multiple use cases to coexist on one camera.
 *
 * Bound exactly once per [LocalLifecycleOwner], inside [LaunchedEffect] —
 * *not* [AndroidView]'s `update` lambda, which Compose re-runs on every
 * recomposition and would mean rebinding the camera every time [presentation]
 * or [lastReadout] ticks one screen up.
 */
@Composable
private fun CameraViewfinder(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(lifecycleOwner) {
        val provider = awaitCameraProvider(context)
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

/**
 * `ProcessCameraProvider.getInstance(context)` returns a Guava `ListenableFuture`.
 * Bridging it through `suspendCancellableCoroutine` plus the stdlib's
 * [kotlin.coroutines.resume] gets the one value this needs without pulling in
 * `kotlinx-coroutines-guava` for what is, structurally, a single `addListener` callback.
 */
private suspend fun awaitCameraProvider(context: Context): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            { continuation.resume(future.get()) },
            ContextCompat.getMainExecutor(context),
        )
    }

/**
 * Doc 1.6 §5's "glass box," in text: what the fast brain thinks it's looking
 * at, and how sure it is — honestly, but never as a numeral. Hard Line 6 is
 * absolute (architect sign-off, Epigenome 029): no visible numeric score
 * anywhere in the device, the child-facing glass box included. The uncertainty
 * itself is still shown plainly — a bar the eye can read and the same banded
 * words ([PerspectiveEngine.confidenceWords]) the companion uses when it
 * speaks, so what she sees and what she hears never disagree. [readout] is
 * `null` for the stretch between launch and the first tap; see
 * [com.cappsconsulting.prism.companion.orchestrator.CompanionOrchestrator.lastReadout]'s
 * kdoc for why that gap is shown honestly rather than papered over with an
 * invented first reading.
 */
@Composable
private fun GlassBoxOverlay(readout: VisionReadout?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(20.dp)
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Text(
            text = "I think I'm looking at…",
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = readout?.label ?: "(tap the screen to look)",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "…and how sure I am:",
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.labelLarge,
        )
        LinearProgressIndicator(
            progress = { readout?.confidence?.toFloat() ?: 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
        )
        Text(
            text = readout?.let { PerspectiveEngine.confidenceWords(it.confidence) } ?: "—",
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
