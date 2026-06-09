package com.cappsconsulting.prism.companion.llm

import android.content.Context

private const val PREFS_NAME = "prism_api_keys"
private const val KEY_ANTHROPIC = "anthropic_api_key"

/**
 * SharedPreferences-backed store for the Anthropic API key — the credential
 * [AnthropicLlmClient] reads on every LLM call.
 *
 * Android Keystore stores non-extractable hardware-backed keys; the Messages API needs the
 * raw key string for the `x-api-key` header. SharedPreferences is the honest first-build
 * choice. Upgrade path: Jetpack Security's `EncryptedSharedPreferences` (AES-GCM wrapping
 * via Keystore) — drop-in replacement for the same `getSharedPreferences` call once
 * `androidx.security:security-crypto` is added to the catalog.
 */
class ApiKeyStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getKey(): String? = prefs.getString(KEY_ANTHROPIC, null)?.takeIf { it.isNotBlank() }

    fun setKey(key: String) {
        prefs.edit().putString(KEY_ANTHROPIC, key.trim()).apply()
    }

    fun clearKey() {
        prefs.edit().remove(KEY_ANTHROPIC).apply()
    }
}
