package com.cappsconsulting.prism.companion.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cappsconsulting.prism.companion.llm.ApiKeyStore

/**
 * Minimal settings screen for entering the Anthropic API key — the one piece of
 * configuration that must come from outside the app before the smart-brain path works.
 *
 * Without a key, [AnthropicLlmClient] throws and [PerspectiveEngine] uses offline fallback
 * phrases; the companion is still functional, just less dynamic. The screen names this
 * honestly: "Without a key, Prism uses offline fallback phrases."
 *
 * Key is stored in [ApiKeyStore] (SharedPreferences, masked in the field). The screen is
 * parent-facing — accessed only via the admin long-press overlay, never during a child session.
 */
@Composable
fun ApiKeyScreen(
    apiKeyStore: ApiKeyStore,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var key by remember { mutableStateOf(apiKeyStore.getKey() ?: "") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Anthropic API Key",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Prism uses Claude to generate persona responses. Without a key, it uses offline fallback phrases.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text("sk-ant-…", color = Color.White.copy(alpha = 0.45f)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { apiKeyStore.setKey(key); onDone() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.45f),
                cursorColor = Color.White,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { apiKeyStore.setKey(key); onDone() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save")
        }
        Spacer(Modifier.height(8.dp))
        if (apiKeyStore.getKey() != null) {
            TextButton(onClick = { apiKeyStore.clearKey(); key = ""; onDone() }) {
                Text("Remove key", color = Color.White.copy(alpha = 0.45f))
            }
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onDone) {
            Text("Cancel", color = Color.White.copy(alpha = 0.55f))
        }
    }
}
