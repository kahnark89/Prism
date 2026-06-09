package com.cappsconsulting.prism.parentsuite.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Preview / Test-Drive mode — Doc 2.2 §6: "runs the real system end-to-end on
 * the parent's test inputs" so the parent can see exactly what their child will see
 * before the first real session.
 *
 * Structurally requires a connected Companion (the Companion must scan a pairing QR
 * and be active on the same local network). The Companion's own pairing UI hasn't
 * been written yet — see the empty placeholder in
 * `companion-app/.../companion/pairing/`. Until that lands, this screen honestly
 * names the wait: "awaiting paired Companion" rather than showing a simulated or
 * mocked preview that would tell the parent a story the hardware can't yet back up.
 * That honesty is the same commitment
 * [com.cappsconsulting.prism.companion.MainActivity]'s [AwaitingCompanionPlaceholder]
 * makes on its side.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewModeScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview mode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Preview mode",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Awaiting a paired Companion.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Preview mode runs end-to-end on the real hardware — your inputs go through " +
                    "the Companion's full pipeline so you see exactly what your child will see.\n\n" +
                    "To use it: pair a Companion device via Settings → Add device, " +
                    "make sure both devices are on the same local network, then return here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
