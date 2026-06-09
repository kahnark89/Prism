package com.cappsconsulting.prism.companion.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

/**
 * Companion-side pairing screen — the Companion's camera scans the QR code displayed
 * on the Parent Suite's [PairingScreen]. Three states:
 *
 * - [PairingState.Scanning]: [DecoratedBarcodeView] active, full-screen, black background.
 *   zxing-embedded owns the camera here — independent of [CameraXSource], which is bound
 *   to the main session screen; pairing is a separate screen, so there's no camera conflict.
 *
 * - [PairingState.Processing]: overlay spinner — ECDH key exchange in progress.
 *
 * - [PairingState.Success] / [PairingState.Error]: result card with a back button.
 *
 * [DecoratedBarcodeView.resume]/[pause] are lifecycle-managed via [LifecycleEventObserver]
 * inside [DisposableEffect] — the correct Compose pattern for View-level lifecycle hooks.
 */
@Composable
fun CompanionPairingScreen(
    viewModel: CompanionPairingViewModel,
    onPairingComplete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pairingState by viewModel.pairingState.collectAsState()

    when (val state = pairingState) {
        is PairingState.Scanning, is PairingState.Processing -> {
            Box(modifier = modifier.fillMaxSize()) {
                QrScannerView(
                    onQrCodeScanned = viewModel::onQrCodeScanned,
                    modifier = Modifier.fillMaxSize(),
                )
                if (pairingState is PairingState.Processing) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        }

        is PairingState.Success -> {
            Column(
                modifier = modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Paired", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Device linked successfully.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.75f),
                )
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onPairingComplete) {
                    Text("Continue", color = Color.White)
                }
            }
        }

        is PairingState.Error -> {
            Column(
                modifier = modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Couldn't pair", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                )
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = viewModel::reset) { Text("Try again", color = Color.White) }
                TextButton(onClick = onCancel) { Text("Cancel", color = Color.White.copy(alpha = 0.6f)) }
            }
        }
    }
}

@Composable
private fun QrScannerView(onQrCodeScanned: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val scannerView = remember {
        DecoratedBarcodeView(context).apply {
            decodeSingle(object : BarcodeCallback {
                override fun barcodeResult(result: BarcodeResult) {
                    result.text?.let { onQrCodeScanned(it) }
                }
            })
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> scannerView.resume()
                Lifecycle.Event.ON_PAUSE -> scannerView.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            scannerView.pause()
        }
    }

    AndroidView(factory = { scannerView }, modifier = modifier)
}
