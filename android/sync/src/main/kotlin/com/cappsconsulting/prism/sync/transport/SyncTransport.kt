package com.cappsconsulting.prism.sync.transport

import com.cappsconsulting.prism.sync.crypto.SealedEnvelope
import com.cappsconsulting.prism.sync.pairing.LinkedDevice

/**
 * Doc 3.0 §3.3: "Local-first... peer-to-peer... Relay fallback... routes through
 * the Prism backend... but only as a zero-knowledge relay."
 *
 * A concrete peer-to-peer transport (Wi-Fi Direct / NSD service discovery) and a
 * concrete relay transport (HTTPS to the existing backend) both need the Android
 * platform and a live network — exactly what this sandbox doesn't have, and not
 * meaningfully fakeable without pretending to verify something we can't.
 *
 * What *can* live here — and is the part actually worth fixing in a shared
 * contract — is the seam itself: every implementation moves [SealedEnvelope]s
 * and nothing else. Callers above this interface never construct, see, or hand
 * over plaintext; callers below it never get anything *but* opaque blobs to carry.
 * That asymmetry is what makes "the relay is structurally incapable of
 * eavesdropping — it never holds a key" (Doc 3.0 §3.4) true of *any* conforming
 * implementation, not just a particular careful one.
 */
interface SyncTransport {
    suspend fun send(to: LinkedDevice, envelope: SealedEnvelope)

    /** Returns the next available envelope, or null if none is currently available. */
    suspend fun receive(): SealedEnvelope?
}

/**
 * The "local-first... fallback" policy from Doc 3.0 §3.3, expressed as plain
 * delegation so it can be proven correct independent of what either underlying
 * transport actually does on the wire (see `FallbackSyncTransportTest`, which
 * substitutes recording fakes for both and a controllable reachability probe).
 *
 * Reachability is injected ([isPeerReachable]) rather than computed here on
 * purpose: "do we currently share a network with that device" is itself a
 * platform/connectivity question (`ConnectivityManager`, NSD presence, …) — a
 * second seam this module shouldn't reach past, for the same reason [SyncTransport]
 * doesn't implement real networking.
 */
class FallbackSyncTransport(
    private val peerToPeer: SyncTransport,
    private val relay: SyncTransport,
    private val isPeerReachable: suspend (LinkedDevice) -> Boolean,
) : SyncTransport {
    override suspend fun send(to: LinkedDevice, envelope: SealedEnvelope) {
        if (isPeerReachable(to)) peerToPeer.send(to, envelope) else relay.send(to, envelope)
    }

    /** Prefers whatever the local link already has waiting; falls through to the relay only if it's silent. */
    override suspend fun receive(): SealedEnvelope? = peerToPeer.receive() ?: relay.receive()
}
