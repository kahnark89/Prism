package com.cappsconsulting.prism.companion.pairing

import android.content.Context
import android.util.Base64
import com.cappsconsulting.prism.sync.pairing.KeyStorage

/**
 * [SharedPreferences]-backed implementation of [KeyStorage] — an honest, working
 * first-build implementation that names its own upgrade path.
 *
 * Doc 3.0 §3.2 step 3 specifies "Android Keystore / StrongBox" — hardware-backed
 * storage where key material never leaves the secure enclave. That guarantee is the
 * right production target. It requires key material to be non-extractable, which is
 * incompatible with [KeyStorage.retrieve]'s `ByteArray` contract (hardware Keystore
 * entries don't expose raw bytes — the cipher runs inside the hardware, not outside
 * it). Reconciling those two shapes is a real architecture decision: either wrap
 * [com.cappsconsulting.prism.sync.crypto.EnvelopeCipher] in a Keystore-backed cipher
 * that never extracts key bytes, or use Android Keystore's `WrappedKeyEntry` to
 * protect an AES key that IS extractable. Either path is concrete and doable;
 * neither fits in this port pass without pre-deciding that architecture choice.
 *
 * So: [SharedPreferences] with [Base64]-encoded bytes now, Keystore in the follow-on
 * pass when the architecture question is settled. The storage interface and its callers
 * don't change — only this class does.
 */
class AndroidKeyStorage(context: Context) : KeyStorage {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun store(alias: String, sharedKey: ByteArray) {
        prefs.edit()
            .putString(alias, Base64.encodeToString(sharedKey, Base64.NO_WRAP))
            .apply()
    }

    override fun retrieve(alias: String): ByteArray? {
        val encoded = prefs.getString(alias, null) ?: return null
        return Base64.decode(encoded, Base64.NO_WRAP)
    }

    override fun remove(alias: String) {
        prefs.edit().remove(alias).apply()
    }

    companion object {
        private const val PREFS_NAME = "prism_pairing_keys"
    }
}
