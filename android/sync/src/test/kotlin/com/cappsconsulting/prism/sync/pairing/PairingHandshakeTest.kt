package com.cappsconsulting.prism.sync.pairing

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Proves the property the whole pairing proposal (Doc 3.0 §3.2) leans on: two
 * devices that each generate their own keypair and exchange only *public* halves
 * — the part that's safe to put in a photographable QR code — arrive at the
 * exact same symmetric secret, without that secret ever having existed anywhere
 * but the two devices' own memory.
 */
class PairingHandshakeTest {

    @Test
    fun `both sides of an ECDH exchange derive the identical shared key`() {
        val parentSuite = PairingHandshake.generateEphemeralKeyPair()
        val companion = PairingHandshake.generateEphemeralKeyPair()

        // Each side combines ITS OWN private key with the OTHER side's public key...
        val derivedByParentSuite = PairingHandshake.deriveSharedKey(parentSuite.private, companion.public)
        val derivedByCompanion = PairingHandshake.deriveSharedKey(companion.private, parentSuite.public)

        // ...and yet — ECDH's defining symmetry — they land on the same secret.
        assertThat(derivedByParentSuite).isEqualTo(derivedByCompanion)
        assertThat(derivedByParentSuite).isNotEmpty()
    }

    @Test
    fun `unrelated keypairs never coincidentally agree on a shared key`() {
        val a = PairingHandshake.generateEphemeralKeyPair()
        val b = PairingHandshake.generateEphemeralKeyPair()
        val stranger = PairingHandshake.generateEphemeralKeyPair()

        val real = PairingHandshake.deriveSharedKey(a.private, b.public)
        val withAnImpostor = PairingHandshake.deriveSharedKey(a.private, stranger.public)

        assertThat(real).isNotEqualTo(withAnImpostor)
    }

    @Test
    fun `every freshly generated keypair is unique -- no reuse across pairing attempts`() {
        val first = PairingHandshake.generateEphemeralKeyPair()
        val second = PairingHandshake.generateEphemeralKeyPair()

        assertThat(PairingHandshake.encodePublicKey(first.public))
            .isNotEqualTo(PairingHandshake.encodePublicKey(second.public))
    }

    @Test
    fun `public key encoding round-trips through the bytes that actually ride inside a QR code`() {
        val original = PairingHandshake.generateEphemeralKeyPair().public

        val roundTripped = PairingHandshake.decodePublicKey(PairingHandshake.encodePublicKey(original))

        assertThat(roundTripped).isEqualTo(original)
        assertThat(roundTripped.encoded).isEqualTo(original.encoded)
    }
}
