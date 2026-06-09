package com.cappsconsulting.prism.sync.pairing

import java.security.PublicKey
import kotlinx.serialization.Serializable

/**
 * The payload encoded into the QR code the Parent Suite displays — Doc 3.0 §3.2
 * step 1: "a one-time QR code (a pairing token + its public key, short-lived)."
 *
 * [tokenId] is single-use; [publicKeyEncoded] is this side's ephemeral ECDH public
 * key (see [PairingHandshake]). Both are safe to put somewhere photographable —
 * the doc is explicit that the *token* could be "photographed and reused" in
 * principle, which is exactly why [isExpired] exists and why the thing that
 * actually persists afterward is the *derived* shared key, never this token.
 */
@Serializable
data class PairingToken(
    val tokenId: String,
    val publicKeyEncoded: ByteArray,
    val issuedAtEpochSeconds: Double,
    val expiresAtEpochSeconds: Double,
) {
    init {
        require(expiresAtEpochSeconds > issuedAtEpochSeconds) { "a pairing token must expire after it's issued" }
    }

    fun isExpired(nowEpochSeconds: Double): Boolean = nowEpochSeconds >= expiresAtEpochSeconds

    fun decodedPublicKey(): PublicKey = PairingHandshake.decodePublicKey(publicKeyEncoded)

    // data class's generated equals/hashCode would use ByteArray identity, not content —
    // wrong for a value that round-trips through QR encode/decode and tests alike.
    override fun equals(other: Any?): Boolean = other is PairingToken &&
        tokenId == other.tokenId &&
        publicKeyEncoded.contentEquals(other.publicKeyEncoded) &&
        issuedAtEpochSeconds == other.issuedAtEpochSeconds &&
        expiresAtEpochSeconds == other.expiresAtEpochSeconds

    override fun hashCode(): Int {
        var result = tokenId.hashCode()
        result = 31 * result + publicKeyEncoded.contentHashCode()
        result = 31 * result + issuedAtEpochSeconds.hashCode()
        result = 31 * result + expiresAtEpochSeconds.hashCode()
        return result
    }

    companion object {
        /** "Short-lived" per Doc 3.0 §3.2 — long enough to scan, short enough that a stale photo is useless. */
        const val DEFAULT_TTL_SECONDS = 120.0

        fun issue(
            tokenId: String,
            publicKey: PublicKey,
            nowEpochSeconds: Double,
            ttlSeconds: Double = DEFAULT_TTL_SECONDS,
        ): PairingToken = PairingToken(
            tokenId = tokenId,
            publicKeyEncoded = PairingHandshake.encodePublicKey(publicKey),
            issuedAtEpochSeconds = nowEpochSeconds,
            expiresAtEpochSeconds = nowEpochSeconds + ttlSeconds,
        )
    }
}
