package com.cappsconsulting.prism.companion.llm

import com.cappsconsulting.prism.engine.perspective.LlmClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@Serializable
private data class MessagesRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val system: String,
    val messages: List<ChatMessage>,
)

@Serializable
private data class ChatMessage(val role: String, val content: String)

@Serializable
private data class MessagesResponse(val content: List<ContentBlock>)

@Serializable
private data class ContentBlock(val type: String, val text: String = "")

private val json = Json { ignoreUnknownKeys = true }

/**
 * [LlmClient] that calls the Anthropic Messages API (`POST /v1/messages`) using Android's
 * built-in [HttpURLConnection] — no third-party HTTP library required.
 *
 * API key is read from [ApiKeyStore] on every call so changes take effect immediately.
 * If no key is configured, [complete] throws [IOException]; [PerspectiveEngine] catches
 * any exception from the LLM call and routes to its offline fallback templates — so
 * "no key set" degrades gracefully rather than crashing.
 *
 * The call runs on [Dispatchers.IO] because [HttpURLConnection] blocks the calling thread
 * for the full network round-trip, which is exactly what IO threads are for.
 *
 * Model is passed in per call (from [com.cappsconsulting.prism.engine.config.PrismConfig.llmModel])
 * — currently `claude-haiku-4-5-20251001`, the fastest/cheapest model for the short
 * 1–2 sentence persona responses this pipeline produces.
 */
class AnthropicLlmClient(private val apiKeyStore: ApiKeyStore) : LlmClient {

    override suspend fun complete(
        model: String,
        maxTokens: Int,
        systemPrompt: String,
        userMessage: String,
    ): String = withContext(Dispatchers.IO) {
        val apiKey = apiKeyStore.getKey()
            ?: throw IOException("Anthropic API key not configured — enter it in the Prism settings screen")

        val requestBody = json.encodeToString(
            MessagesRequest(
                model = model,
                maxTokens = maxTokens,
                system = systemPrompt,
                messages = listOf(ChatMessage(role = "user", content = userMessage)),
            )
        )

        val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("x-api-key", apiKey)
            setRequestProperty("anthropic-version", ANTHROPIC_VERSION)
            setRequestProperty("content-type", "application/json")
            connectTimeout = 10_000
            readTimeout = 30_000
            doOutput = true
        }

        try {
            conn.outputStream.use { it.write(requestBody.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            if (code != 200) {
                val error = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                throw IOException("Anthropic API error $code: $error")
            }

            val responseBody = conn.inputStream.bufferedReader().readText()
            val response = json.decodeFromString<MessagesResponse>(responseBody)
            response.content.firstOrNull { it.type == "text" }?.text?.trim()
                ?: throw IOException("No text content in Anthropic response")
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val API_URL = "https://api.anthropic.com/v1/messages"
        const val ANTHROPIC_VERSION = "2023-06-01"
    }
}
