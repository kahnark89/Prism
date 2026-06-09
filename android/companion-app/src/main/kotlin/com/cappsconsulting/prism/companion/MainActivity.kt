package com.cappsconsulting.prism.companion

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.cappsconsulting.prism.companion.orchestrator.CompanionViewModel
import com.cappsconsulting.prism.companion.ui.CompanionScreen
import com.cappsconsulting.prism.companion.ui.theme.PrismCompanionTheme

/**
 * The Companion app's single activity — `AndroidManifest.xml` commits to
 * that shape: `lockTaskMode="if_whitelisted"`, portrait-locked, one
 * `LAUNCHER` entry. "Sealed enclosure, no detachable parts" (Doc 3.0 §4's
 * translation table) is a single screen in this codebase, not a welded
 * chassis. [enableEdgeToEdge] plus [PrismCompanionTheme]'s all-black scheme
 * is this class's half of the handoff `themes.xml` documents — "set the
 * activity's window to edge-to-edge black before Compose takes over" —
 * which `targetSdk = 35` makes mandatory on Android 15 devices anyway, but
 * which Beat 1 requires from frame one regardless of OS version.
 *
 * [CompanionViewModel.initialize] is called before [setContent] — it's
 * synchronous up to the internal `viewModelScope.launch`, so
 * [CompanionViewModel.orchestrator] is non-null by the time Compose first
 * renders, and the ViewModel's re-entrancy guard makes activity-restart
 * calls to [initialize] no-ops.
 *
 * Permissions are requested before `initialize` so the camera and
 * microphone dialogs clear before the session begins — a child is never
 * interrupted mid-session by a permission prompt.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: CompanionViewModel by viewModels()

    private val requestRuntimePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* permission results arrive after the session is already running */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestRuntimePermissions.launch(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
        )
        viewModel.initialize(this)
        setContent {
            PrismCompanionTheme {
                CompanionScreen(
                    orchestrator = viewModel.orchestrator!!,
                    cameraPreviewView = viewModel.cameraPreviewView,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
