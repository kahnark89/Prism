package com.cappsconsulting.prism.parentsuite.ui.pairing

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.cappsconsulting.prism.parentsuite.viewmodel.ParentSuiteViewModel
import com.cappsconsulting.prism.sync.pairing.LinkedDevice
import com.cappsconsulting.prism.sync.pairing.PairingToken
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Pairing screen — Doc 3.0 §3.2 step 1: "Parent Suite generates and displays a
 * one-time QR code (pairing token + public key, short-lived)."
 *
 * The QR bitmap comes from [ParentSuiteViewModel.pairingQr] — generated via
 * [com.journeyapps.barcodescanner.BarcodeEncoder] encoding the token's JSON
 * on a background dispatcher. This screen has no [android.Manifest.permission.CAMERA]
 * permission and needs none: the Companion scans; the Parent Suite only displays.
 *
 * The TTL countdown shown below the QR keeps the parent informed of how much scan
 * time remains without requiring them to know what a "pairing token" is —
 * Doc 2.2's glass-box instinct ("nothing about the system's own connections
 * is hidden from the parent") applied to the pairing flow itself.
 *
 * The "linked devices" list below obeys the same instinct: all paired devices
 * visible, each independently revocable — not hidden because the parent might
 * not understand what they are.
 */
@Composable
fun PairingScreen(viewModel: ParentSuiteViewModel, modifier: Modifier = Modifier) {
    val token by viewModel.activePairingToken.collectAsState()
    val qrBitmap by viewModel.pairingQr.collectAsState()
    val linkedDevices by viewModel.linkedDevices.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("Pair a new device", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Open Prism on your Companion device and scan this code to link it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            if (token != null && qrBitmap != null) {
                ActivePairingTokenCard(
                    token = token!!,
                    qrBitmap = qrBitmap!!,
                    onClear = viewModel::clearPairingToken,
                    onRegenerate = viewModel::generatePairingToken,
                )
            } else {
                Button(
                    onClick = viewModel::generatePairingToken,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Generate QR code")
                }
            }
        }

        if (linkedDevices.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("Linked devices", style = MaterialTheme.typography.titleMedium)
            }
            items(linkedDevices, key = { it.deviceId }) { device ->
                LinkedDeviceRow(
                    device = device,
                    onUnlink = { viewModel.unlinkDevice(device.deviceId) },
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ActivePairingTokenCard(
    token: PairingToken,
    qrBitmap: android.graphics.Bitmap,
    onClear: () -> Unit,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var secondsRemaining by remember { mutableIntStateOf(0) }

    LaunchedEffect(token.expiresAtEpochSeconds) {
        while (true) {
            val now = System.currentTimeMillis() / 1000.0
            val remaining = (token.expiresAtEpochSeconds - now).toInt().coerceAtLeast(0)
            secondsRemaining = remaining
            if (remaining == 0) break
            delay(1_000)
        }
    }

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "Pairing QR code — scan with Companion",
                modifier = Modifier.size(240.dp),
            )
            Spacer(Modifier.height(12.dp))
            if (secondsRemaining > 0) {
                Text(
                    text = "Expires in ${secondsRemaining}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (secondsRemaining <= 30) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            } else {
                Text(
                    text = "Expired",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRegenerate) { Text("Regenerate") }
                TextButton(onClick = onClear) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun LinkedDeviceRow(
    device: LinkedDevice,
    onUnlink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = device.label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "paired ${device.pairedAtEpochSeconds.toFormattedDate()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onUnlink) {
                Text("Unlink", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun Double.toFormattedDate(): String = runCatching {
    Instant.ofEpochSecond(toLong())
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
}.getOrElse { "" }
