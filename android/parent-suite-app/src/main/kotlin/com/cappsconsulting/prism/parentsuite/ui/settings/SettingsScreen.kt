package com.cappsconsulting.prism.parentsuite.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.cappsconsulting.prism.parentsuite.data.ChildProfile
import com.cappsconsulting.prism.parentsuite.viewmodel.ParentSuiteViewModel

/**
 * Settings screen — Doc 2.2 §5's child-profile configuration surface, plus the
 * entry points to [com.cappsconsulting.prism.parentsuite.ui.pairing.PairingScreen]
 * and [com.cappsconsulting.prism.parentsuite.ui.preview.PreviewModeScreen].
 *
 * Child name and active-companion edits save on focus-loss (no explicit Save button).
 * Doc 2.2 §5 explicitly calls these "two operational fields" — not a profile the
 * child would object to, just enough for the Companion to greet them by name.
 */
@Composable
fun SettingsScreen(
    viewModel: ParentSuiteViewModel,
    onNavigateToPairing: () -> Unit,
    onNavigateToPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val childProfile by viewModel.childProfile.collectAsState()

    var childNameDraft by remember(childProfile.childName) {
        mutableStateOf(childProfile.childName)
    }
    var companionIdDraft by remember(childProfile.activeCompanionId) {
        mutableStateOf(childProfile.activeCompanionId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(0.dp))

        Text("Child", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = childNameDraft,
            onValueChange = { childNameDraft = it },
            label = { Text("Child's name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && childNameDraft != childProfile.childName) {
                        viewModel.updateChildProfile(
                            childProfile.copy(childName = childNameDraft),
                        )
                    }
                },
        )
        Spacer(Modifier.height(0.dp))

        HorizontalDivider()
        Text("Companion device", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = companionIdDraft,
            onValueChange = { companionIdDraft = it },
            label = { Text("Active Companion ID") },
            singleLine = true,
            supportingText = { Text("Set automatically when you pair a Companion device.") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && companionIdDraft != childProfile.activeCompanionId) {
                        viewModel.updateChildProfile(
                            childProfile.copy(activeCompanionId = companionIdDraft),
                        )
                    }
                },
        )
        Button(
            onClick = onNavigateToPairing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Add / manage devices")
        }
        Spacer(Modifier.height(0.dp))

        HorizontalDivider()
        Text("Preview mode", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Run a test session end-to-end on your own inputs before a real session. Requires a connected Companion.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onNavigateToPreview,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Enter preview mode")
        }
    }
}
