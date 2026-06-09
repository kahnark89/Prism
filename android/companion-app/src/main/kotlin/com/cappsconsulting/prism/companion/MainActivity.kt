package com.cappsconsulting.prism.companion

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cappsconsulting.prism.companion.ui.theme.PrismCompanionTheme

/**
 * The Companion app's single activity — `AndroidManifest.xml` already
 * commits to that shape: `lockTaskMode="if_whitelisted"`, portrait-locked,
 * one `LAUNCHER` entry. "Sealed enclosure, no detachable parts" (Doc 3.0
 * §4's translation table) is a single screen in this codebase, not a
 * welded chassis. [enableEdgeToEdge] plus [PrismCompanionTheme]'s all-black
 * scheme is this class's half of the handoff `themes.xml` documents — "set
 * the activity's window to edge-to-edge black before Compose takes over" —
 * which `targetSdk = 35` makes mandatory on Android 15 devices anyway, but
 * which Beat 1 requires from frame one regardless of OS version.
 *
 * What's *not* here yet, named rather than faked: a running
 * [com.cappsconsulting.prism.companion.orchestrator.CompanionOrchestrator].
 * Building one means constructing a real
 * [com.cappsconsulting.prism.companion.hal.CompanionHal] — CameraX,
 * `AudioRecord`, `TextToSpeech`, and composable-`VibrationEffect` bindings
 * for its [com.cappsconsulting.prism.companion.hal.CameraSource],
 * [com.cappsconsulting.prism.companion.hal.MicrophoneSource],
 * [com.cappsconsulting.prism.companion.hal.SpeakerOutput], and
 * [com.cappsconsulting.prism.companion.hal.HapticOutput] members (each named
 * in `hal/android/` as future work, none yet written) — and a real
 * [com.cappsconsulting.prism.companion.recognition.RecognitionEngine], which
 * per its own kdoc has no honest mock: "a fake 'yes, this is your enrolled
 * child' is never an honest placeholder for a biometric safety gate." Wiring
 * [com.cappsconsulting.prism.companion.ui.CompanionScreen] to an orchestrator
 * that can't run would be exactly the fabrication this port has refused since
 * [com.cappsconsulting.prism.companion.vision.VisionClassifier]'s kdoc first
 * named the line. So: [AwaitingCompanionPlaceholder] stands in until those
 * pieces land; the moment they do, this `setContent` block's only change is
 * which composable it calls.
 */
class MainActivity : ComponentActivity() {

    private val requestRuntimePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        // Nothing to react to yet — no pipeline depends on the result until
        // CompanionOrchestrator exists. Requesting now means the OS dialog is
        // already past by the time the orchestrator is wired in, so a child
        // is never interrupted mid-session by a permission prompt.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestRuntimePermissions.launch(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
        )
        setContent {
            PrismCompanionTheme {
                AwaitingCompanionPlaceholder(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

/**
 * Stands in for [com.cappsconsulting.prism.companion.ui.CompanionScreen]
 * until [MainActivity]'s missing pieces exist to drive it — a screen that
 * tells a developer the truth (why nothing is moving yet) rather than one
 * that tells a child a story the hardware can't yet back up. Black
 * background, quiet centered text: even this placeholder holds Beat 1's
 * "holds still, black, waiting" register, because the alternative — a
 * generic Android "Hello World" white screen — would be a louder, less
 * honest interruption of the mood this app exists to hold than simply
 * naming the wait.
 */
@Composable
private fun AwaitingCompanionPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Prism",
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Waiting on its hardware bindings — camera, voice, haptics, " +
                "and on-device recognition — to be wired in.\n\n" +
                "See HardwareAbstractionLayer.kt and RecognitionEngine.kt " +
                "for exactly what's missing, and why it's named here " +
                "rather than faked.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
