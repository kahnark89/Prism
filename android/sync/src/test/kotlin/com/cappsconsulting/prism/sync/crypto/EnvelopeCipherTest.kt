package com.cappsconsulting.prism.sync.crypto

import com.google.common.truth.Truth.assertThat
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import org.junit.Test

/**
 * [EnvelopeCipher] is what makes Doc 3.0's "zero-knowledge relay" claim checkable
 * rather than asserted: these tests prove both halves of that claim — that the
 * right key opens a sealed envelope cleanly, AND that anything else (wrong key,
 * altered bytes) fails *loudly*, never silently producing plausible-looking
 * garbage a relay could learn to interpret.
 */
class EnvelopeCipherTest {

    private val random = SecureRandom()
    private fun randomKey(bytes: Int = 32): ByteArray = ByteArray(bytes).also(random::nextBytes)

    @Test
    fun `sealing then opening with the same key recovers the exact original plaintext`() {
        val key = randomKey()
        val plaintext = "the menu state for tonight: pacing=balanced, boundary=20m".toByteArray()

        val envelope = EnvelopeCipher.seal(plaintext, key)
        val recovered = EnvelopeCipher.open(envelope, key)

        assertThat(recovered).isEqualTo(plaintext)
    }

    @Test
    fun `the sealed envelope reveals nothing recognizable about its contents`() {
        val key = randomKey()
        val plaintext = "session summary: concept=spiral, status=getting_it".toByteArray()

        val envelope = EnvelopeCipher.seal(plaintext, key)

        // What a curious relay actually sees — and it must not be, or contain, the plaintext.
        assertThat(envelope.ciphertext).isNotEqualTo(plaintext)
        val ciphertextAsText = String(envelope.ciphertext, Charsets.ISO_8859_1)
        assertThat(ciphertextAsText).doesNotContain("spiral")
        assertThat(ciphertextAsText).doesNotContain("getting_it")
    }

    @Test
    fun `opening with the wrong key is refused outright, not silently garbled`() {
        val sealedWith = randomKey()
        val openedWith = randomKey()
        val envelope = EnvelopeCipher.seal("a private moment between two devices".toByteArray(), sealedWith)

        org.junit.Assert.assertThrows(AEADBadTagException::class.java) {
            EnvelopeCipher.open(envelope, openedWith)
        }
    }

    @Test
    fun `tampering with the ciphertext in transit is detected and refused, not decrypted`() {
        val key = randomKey()
        val envelope = EnvelopeCipher.seal("untouched".toByteArray(), key)
        val tampered = envelope.copy(ciphertext = envelope.ciphertext.also { it[0] = it[0].inc() })

        org.junit.Assert.assertThrows(AEADBadTagException::class.java) {
            EnvelopeCipher.open(tampered, key)
        }
    }

    @Test
    fun `each sealing draws a fresh nonce -- identical plaintexts still produce distinct envelopes`() {
        val key = randomKey()
        val plaintext = "same message, twice".toByteArray()

        val first = EnvelopeCipher.seal(plaintext, key)
        val second = EnvelopeCipher.seal(plaintext, key)

        assertThat(first.nonce).isNotEqualTo(second.nonce)
        assertThat(first.ciphertext).isNotEqualTo(second.ciphertext)
        // Yet both still open to the same plaintext under the shared key — nonce reuse
        // would be the actual vulnerability; distinct nonces are the guard against it.
        assertThat(EnvelopeCipher.open(first, key)).isEqualTo(plaintext)
        assertThat(EnvelopeCipher.open(second, key)).isEqualTo(plaintext)
    }
}
