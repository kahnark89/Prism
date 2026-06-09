package com.cappsconsulting.prism.parentsuite.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * The Parent Suite's theme — a readable management tool, the deliberate structural
 * contrast with [com.cappsconsulting.prism.companion.ui.theme.PrismCompanionTheme]'s
 * always-dark immersive display. Material3's default [lightColorScheme] without
 * customization: the parent needs the accessible, familiar surface of a standard
 * management app, not an immersive show.
 */
@Composable
fun PrismParentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        content = content,
    )
}
