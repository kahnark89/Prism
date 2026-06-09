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
import com.cappsconsulting.prism.companion.pairing.CompanionPairingViewModel
import com.cappsconsulting.prism.companion.ui.CompanionNavHost
import com.cappsconsulting.prism.companion.ui.theme.PrismCompanionTheme

/**
 * The Companion app's single activity — `AndroidManifest.xml` commits to
 * that shape: `lockTaskMode="if_whitelisted"`, portrait-locked, one
 * `LAUNCHER` entry. [enableEdgeToEdge] plus [PrismCompanionTheme]'s all-black
 * scheme is this class's half of the handoff `themes.xml` documents.
 *
 * [CompanionViewModel.initialize] is called before [setContent] — synchronous
 * up to the internal `viewModelScope.launch`, so [CompanionViewModel.orchestrator]
 * is non-null by the time Compose first renders. The ViewModel's re-entrancy
 * guard makes activity-restart calls no-ops.
 *
 * Permissions are requested before `initialize` so the camera and microphone
 * dialogs clear before the session begins — a child is never interrupted
 * mid-session by a permission prompt.
 *
 * Navigation: [CompanionNavHost] handles the full destination graph (session,
 * enrollment, pairing, API-key, admin menu). The admin menu is accessed via
 * long-press on the session screen — invisible to the child during normal use.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: CompanionViewModel by viewModels()
    private val pairingViewModel: CompanionPairingViewModel by viewModels()

    private val requestRuntimePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* permission results arrive asynchronously; session already started */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestRuntimePermissions.launch(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
        )
        viewModel.initialize(this)
        setContent {
            PrismCompanionTheme {
                CompanionNavHost(
                    viewModel = viewModel,
                    pairingViewModel = pairingViewModel,
                    apiKeyStore = viewModel.apiKeyStore,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
