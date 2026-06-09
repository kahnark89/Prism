package com.cappsconsulting.prism.companion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Deliberately minimal, and deliberately *not* `isSystemInDarkTheme`-aware —
 * Doc 2.3 puts almost everything on the Compose canvas itself (full-screen
 * color blooms driven by [com.cappsconsulting.prism.companion.presentation.PresentationState],
 * not Material chrome), and `themes.xml`'s `Theme.Prism.Companion` already
 * commits the activity window to edge-to-edge black before Compose takes
 * over. This is that same commitment continued: a dark scheme whose surfaces
 * stay out of the way, so a [com.cappsconsulting.prism.companion.presentation.PresentationPattern.DARK]
 * frame reads as "the screen, holding still, black, waiting" (Beat 1) — not
 * "a dark canvas sitting inside a lighter app the system theme happened to pick."
 */
private val PrismDarkColorScheme = darkColorScheme(
    background = Color.Black,
    surface = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun PrismCompanionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PrismDarkColorScheme,
        content = content,
    )
}
