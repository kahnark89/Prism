package com.cappsconsulting.prism.sync.pairing

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PairingTokenTest {

    private val keyPair = PairingHandshake.generateEphemeralKeyPair()

    @Test
    fun `issue stamps a short, fixed lifetime from the moment it's created`() {
        val token = PairingToken.issue(tokenId = "tok-1", publicKey = keyPair.public, nowEpochSeconds = 1_000.0)

        assertThat(token.issuedAtEpochSeconds).isEqualTo(1_000.0)
        assertThat(token.expiresAtEpochSeconds).isEqualTo(1_000.0 + PairingToken.DEFAULT_TTL_SECONDS)
        assertThat(token.isExpired(nowEpochSeconds = 1_000.0)).isFalse()
    }

    @Test
    fun `a token is valid right up to its expiry instant and expired from that instant on`() {
        val token = PairingToken.issue(tokenId = "tok-1", publicKey = keyPair.public, nowEpochSeconds = 1_000.0, ttlSeconds = 60.0)

        assertThat(token.isExpired(nowEpochSeconds = 1_059.999)).isFalse()
        assertThat(token.isExpired(nowEpochSeconds = 1_060.0)).isTrue()
        assertThat(token.isExpired(nowEpochSeconds = 1_200.0)).isTrue()
    }

    @Test
    fun `a token that does not outlive its own issue instant is rejected outright`() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            PairingToken(tokenId = "x", publicKeyEncoded = ByteArray(0), issuedAtEpochSeconds = 100.0, expiresAtEpochSeconds = 100.0)
        }
    }

    @Test
    fun `decodedPublicKey recovers exactly the key that was encoded into the token`() {
        val token = PairingToken.issue(tokenId = "tok-1", publicKey = keyPair.public, nowEpochSeconds = 1_000.0)

        assertThat(token.decodedPublicKey()).isEqualTo(keyPair.public)
    }

    @Test
    fun `equality and hashing compare the encoded key by content, not by array identity`() {
        val a = PairingToken.issue(tokenId = "tok-1", publicKey = keyPair.public, nowEpochSeconds = 1_000.0)
        // Re-derive from freshly-copied bytes — same content, different ByteArray instance.
        val b = a.copy(publicKeyEncoded = a.publicKeyEncoded.copyOf())

        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }
}
