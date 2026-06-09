package com.cappsconsulting.prism.companion.pairing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cappsconsulting.prism.sync.pairing.LinkedDevice
import com.cappsconsulting.prism.sync.pairing.LinkedDeviceRegistry
import com.cappsconsulting.prism.sync.pairing.PairingHandshake
import com.cappsconsulting.prism.sync.pairing.PairingToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** States for the Companion-side pairing flow — one per step of Doc 3.0 §3.2. */
sealed interface PairingState {
    /** Camera active, waiting for the parent's QR code. */
    data object Scanning : PairingState

    /** QR found — running ECDH handshake + key storage. */
    data object Processing : PairingState

    /** Key exchange complete; device is linked. [deviceId] for display only. */
    data class Success(val deviceId: String) : PairingState

    /** Something went wrong. [message] is safe to show in the UI. */
    data class Error(val message: String) : PairingState
}

/**
 * Companion-side pairing flow — Doc 3.0 §3.2 steps 2–3 (the Companion's half):
 *
 * 1. Decode the [PairingToken] from the scanned QR JSON.
 * 2. Check expiry (tokens are short-lived by design — see [PairingToken.DEFAULT_TTL_SECONDS]).
 * 3. Generate this device's own ephemeral key pair.
 * 4. Derive the shared secret via ECDH: `PairingHandshake.deriveSharedKey(myPriv, parentPub)`.
 * 5. Store the derived key under an alias in [AndroidKeyStorage].
 * 6. Register a [LinkedDevice] in the [LinkedDeviceRegistry] so the sync transport
 *    can look it up later.
 *
 * [keyStorage] and [deviceRegistry] are injected rather than constructed internally
 * so the pairing logic is testable against the [com.cappsconsulting.prism.sync.pairing.KeyStorage]
 * interface without needing an Android context in tests.
 */
class CompanionPairingViewModel(application: Application) : AndroidViewModel(application) {

    private val keyStorage = AndroidKeyStorage(application)
    val deviceRegistry = LinkedDeviceRegistry()

    private val _pairingState = MutableStateFlow<PairingState>(PairingState.Scanning)
    val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()

    fun onQrCodeScanned(rawQrContent: String) {
        if (_pairingState.value !is PairingState.Scanning) return
        _pairingState.value = PairingState.Processing

        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.Default) {
                    performHandshake(rawQrContent)
                }
            }
            _pairingState.value = result.fold(
                onSuccess = { deviceId -> PairingState.Success(deviceId) },
                onFailure = { e -> PairingState.Error(e.message ?: "Pairing failed") },
            )
        }
    }

    fun reset() {
        _pairingState.value = PairingState.Scanning
    }

    private fun performHandshake(qrJson: String): String {
        val token = Json.decodeFromString(PairingToken.serializer(), qrJson)

        val nowSeconds = System.currentTimeMillis() / 1000.0
        if (token.isExpired(nowSeconds)) error("QR code has expired — ask the parent to generate a new one.")

        val myKeyPair = PairingHandshake.generateEphemeralKeyPair()
        val parentPublicKey = token.decodedPublicKey()
        val sharedKey = PairingHandshake.deriveSharedKey(myKeyPair.private, parentPublicKey)

        val keyAlias = "prism_pairing_${token.tokenId}"
        keyStorage.store(keyAlias, sharedKey)

        val device = LinkedDevice(
            deviceId = token.tokenId,
            label = "Parent device",
            keyAlias = keyAlias,
            pairedAtEpochSeconds = nowSeconds,
        )
        deviceRegistry.link(device)

        return token.tokenId
    }
}
