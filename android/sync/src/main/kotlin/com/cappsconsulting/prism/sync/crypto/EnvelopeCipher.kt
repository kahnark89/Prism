package com.cappsconsulting.prism.sync.crypto

import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.Serializable

private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val KEY_ALGORITHM = "AES"
private const val GCM_TAG_BITS = 128
private const val NONCE_BYTES = 12

/**
 * The opaque, paired-key-encrypted blob — the *only* shape of thing that crosses
 * the sync boundary on the wire (Doc 3.0 §3.3: the relay "stores and forwards
 * opaque, paired-key-encrypted blobs it cannot decrypt"). [nonce] travels in the
 * clear alongside [ciphertext] (this is how AES-GCM is meant to be used — the
 * nonce isn't the secret, the key is) and is exactly as informative to a relay
 * as any other random-looking bytes in the envelope: not at all.
 */
@Serializable
data class SealedEnvelope(val nonce: ByteArray, val ciphertext: ByteArray) {
    override fun equals(other: Any?): Boolean = other is SealedEnvelope &&
        nonce.contentEquals(other.nonce) &&
        ciphertext.contentEquals(other.ciphertext)

    override fun hashCode(): Int = 31 * nonce.contentHashCode() + ciphertext.contentHashCode()
}

/**
 * AES-256-GCM sealing over the ECDH-derived shared key (see
 * [com.cappsconsulting.prism.sync.pairing.PairingHandshake.deriveSharedKey]).
 *
 * GCM is *authenticated* encryption: [open] doesn't just fail to produce the right
 * plaintext when the key is wrong or the bytes were altered in transit — it refuses
 * to produce *any* plaintext, throwing [AEADBadTagException]. That refusal is what
 * makes the zero-knowledge-relay claim in Doc 3.0 §3.3/§3.4 a checkable structural
 * property rather than an unfalsifiable promise: a relay (or anyone else on the
 * wire) that tries to tamper with or originate traffic produces something that
 * provably does not decrypt, instead of something that silently decrypts to noise.
 */
object EnvelopeCipher {
    private val random = SecureRandom()

    fun seal(plaintext: ByteArray, sharedKey: ByteArray): SealedEnvelope {
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey(sharedKey), GCMParameterSpec(GCM_TAG_BITS, nonce))
        return SealedEnvelope(nonce = nonce, ciphertext = cipher.doFinal(plaintext))
    }

    /** @throws AEADBadTagException if [sharedKey] doesn't match, or [envelope] was altered after sealing. */
    fun open(envelope: SealedEnvelope, sharedKey: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(sharedKey), GCMParameterSpec(GCM_TAG_BITS, envelope.nonce))
        return cipher.doFinal(envelope.ciphertext)
    }

    private fun secretKey(sharedKey: ByteArray) = SecretKeySpec(sharedKey, KEY_ALGORITHM)
}
