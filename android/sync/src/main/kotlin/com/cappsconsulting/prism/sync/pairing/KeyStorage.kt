package com.cappsconsulting.prism.sync.pairing

/**
 * Seam over platform secure-hardware key storage — Doc 3.0 §3.2 step 3: "Stored in
 * platform secure hardware. Android Keystore / StrongBox on both ends — the same
 * guarantee Hard Line 3 already requires for recognition templates, reused for the
 * pairing key."
 *
 * `"AndroidKeyStore"` is a `java.security.KeyStore` provider that simply does not
 * exist on a plain JVM — there is no faithful, testable-here implementation of this
 * interface, and pretending otherwise (e.g. backing it with a file or an in-memory
 * map "for now") would understate the guarantee Doc 3.0 actually asks for. So this
 * stays an interface: the real implementation is unavoidably `:companion-app` /
 * `:parent-suite-app` platform code (`KeyStore.getInstance("AndroidKeyStore")`),
 * and everything in *this* module is written to need nothing more than this seam —
 * callers pass an alias, never raw key bytes, and [LinkedDevice] stores only the alias.
 */
interface KeyStorage {
    /** Persists [sharedKey] under [alias] in secure hardware, replacing any prior value. */
    fun store(alias: String, sharedKey: ByteArray)

    /** Returns the stored key for [alias], or null if nothing is stored under it. */
    fun retrieve(alias: String): ByteArray?

    /** Irreversibly destroys the key under [alias] — what makes "unlinking... revokes... instantly" true. */
    fun remove(alias: String)
}
