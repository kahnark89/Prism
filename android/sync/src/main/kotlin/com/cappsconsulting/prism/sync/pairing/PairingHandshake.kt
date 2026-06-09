package com.cappsconsulting.prism.sync.pairing

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement

/**
 * On-device ECDH key exchange — Doc 3.0 §3.2 step 2: "the two apps derive a shared
 * secret without that secret ever existing outside the two devices — not on a
 * server, not in a QR code that could be photographed and reused (the token is
 * single-use and short-lived; the *derived* key is what persists)."
 *
 * This object owns only the *math* (key agreement + a minimal key derivation step),
 * which is plain `java.security`/`javax.crypto` — identical on the JVM and on
 * Android, and so exactly the slice of the pairing flow this sandbox can compile
 * and prove correct. *Storage* of the derived key is a platform concern (Android
 * Keystore / StrongBox, Doc 3.0 §3.2 step 3) and is deliberately kept out of this
 * object — see [KeyStorage].
 */
object PairingHandshake {
    private const val CURVE = "secp256r1"
    private const val KEY_ALGORITHM = "EC"
    private const val AGREEMENT_ALGORITHM = "ECDH"
    private const val DIGEST_ALGORITHM = "SHA-256"

    /** One side's half of the exchange — generated fresh per pairing attempt, never reused. */
    fun generateEphemeralKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance(KEY_ALGORITHM)
        generator.initialize(ECGenParameterSpec(CURVE))
        return generator.generateKeyPair()
    }

    /** X.509/SubjectPublicKeyInfo encoding — what actually rides inside the QR code's [PairingToken]. */
    fun encodePublicKey(key: PublicKey): ByteArray = key.encoded

    fun decodePublicKey(encoded: ByteArray): PublicKey =
        KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(X509EncodedKeySpec(encoded))

    /**
     * Runs ECDH from this side's private key and the other side's public key, then
     * hashes the agreed secret down to a fixed-length symmetric key — a minimal KDF,
     * matching "the *derived* key is what persists" in Doc 3.0 §3.2. Both participants
     * compute the *same* value (ECDH's defining symmetry: {myPriv, theirPub} and
     * {theirPriv, myPub} agree), so this single function serves both sides of the
     * handshake — there's no "initiator" vs "responder" branch to keep in sync.
     */
    fun deriveSharedKey(myPrivateKey: PrivateKey, theirPublicKey: PublicKey): ByteArray {
        val agreement = KeyAgreement.getInstance(AGREEMENT_ALGORITHM)
        agreement.init(myPrivateKey)
        agreement.doPhase(theirPublicKey, true)
        return MessageDigest.getInstance(DIGEST_ALGORITHM).digest(agreement.generateSecret())
    }
}
