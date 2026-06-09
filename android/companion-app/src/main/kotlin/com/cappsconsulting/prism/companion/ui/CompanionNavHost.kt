package com.cappsconsulting.prism.companion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cappsconsulting.prism.companion.llm.ApiKeyStore
import com.cappsconsulting.prism.companion.orchestrator.CompanionViewModel
import com.cappsconsulting.prism.companion.pairing.CompanionPairingScreen
import com.cappsconsulting.prism.companion.pairing.CompanionPairingViewModel
import com.cappsconsulting.prism.companion.ui.enrollment.CompanionEnrollmentScreen
import com.cappsconsulting.prism.companion.ui.settings.ApiKeyScreen

/**
 * Root navigation for the Companion app — four destinations plus a parent-only admin menu.
 *
 * Routes:
 *  - `session` — [CompanionScreen]; the child-facing session, start destination.
 *  - `admin` — [AdminMenuScreen]; parent-only, accessed via long-press on the session screen.
 *    Not visible to the child: the long-press threshold is high enough that a child's
 *    exploratory tapping won't trigger it.
 *  - `enrollment` — [CompanionEnrollmentScreen]; guided face-enrollment flow.
 *  - `pairing` — [CompanionPairingScreen]; QR scan to pair with the Parent Suite.
 *  - `apikey` — [ApiKeyScreen]; one-field screen to store the Anthropic key.
 *
 * The admin menu routes back into `enrollment`/`pairing`/`apikey` via `popUpTo("admin")
 * { inclusive = true }` so Back from any of them lands on `session`, not the menu.
 */
@Composable
fun CompanionNavHost(
    viewModel: CompanionViewModel,
    pairingViewModel: CompanionPairingViewModel,
    apiKeyStore: ApiKeyStore,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "session",
        modifier = modifier.fillMaxSize(),
    ) {
        composable("session") {
            CompanionScreen(
                orchestrator = viewModel.orchestrator!!,
                cameraPreviewView = viewModel.cameraPreviewView,
                onAdminGesture = { navController.navigate("admin") },
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable("admin") {
            AdminMenuScreen(
                isEnrolled = viewModel.isEnrolled(),
                onEnroll = {
                    navController.navigate("enrollment") {
                        popUpTo("admin") { inclusive = true }
                    }
                },
                onPair = {
                    navController.navigate("pairing") {
                        popUpTo("admin") { inclusive = true }
                    }
                },
                onApiKey = {
                    navController.navigate("apikey") {
                        popUpTo("admin") { inclusive = true }
                    }
                },
                onDismiss = { navController.popBackStack() },
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable("enrollment") {
            CompanionEnrollmentScreen(
                viewModel = viewModel,
                onEnrollmentComplete = { navController.popBackStack() },
                onCancel = { navController.popBackStack() },
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable("pairing") {
            CompanionPairingScreen(
                viewModel = pairingViewModel,
                onPairingComplete = { navController.popBackStack() },
                onCancel = { navController.popBackStack() },
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable("apikey") {
            ApiKeyScreen(
                apiKeyStore = apiKeyStore,
                onDone = { navController.popBackStack() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Parent-only admin menu — reached via long-press on the session screen; invisible
 * during normal child interaction. Shows enrollment status so the parent can see at
 * a glance whether the child has been enrolled.
 */
@Composable
private fun AdminMenuScreen(
    isEnrolled: Boolean,
    onEnroll: () -> Unit,
    onPair: () -> Unit,
    onApiKey: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(40.dp)
                .fillMaxWidth(0.8f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Prism Setup",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isEnrolled) "Child enrolled" else "Not yet enrolled",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.55f),
            )
            Spacer(Modifier.height(28.dp))
            Button(onClick = onEnroll, modifier = Modifier.fillMaxWidth()) {
                Text(if (isEnrolled) "Re-enroll child" else "Enroll child")
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onPair, modifier = Modifier.fillMaxWidth()) {
                Text("Pair with Parent Suite")
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onApiKey, modifier = Modifier.fillMaxWidth()) {
                Text("Set API key")
            }
            Spacer(Modifier.height(20.dp))
            TextButton(onClick = onDismiss) {
                Text("Back", color = Color.White.copy(alpha = 0.55f))
            }
        }
    }
}
