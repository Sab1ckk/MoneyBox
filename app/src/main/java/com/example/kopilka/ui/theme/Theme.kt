// Путь в проекте: app/src/main/java/com/example/kopilka/ui/theme/Theme.kt

package com.example.kopilka.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val KopilkaColorScheme = darkColorScheme(
    background = ColorBase,
    surface = ColorCard,
    surfaceVariant = ColorSurface,
    primary = ColorAccent,
    onPrimary = ColorBase,
    onBackground = ColorText,
    onSurface = ColorText,
    error = ColorError,
    onError = ColorText
)

/**
 * Единственная тема приложения — тёмная (см. Kopilka_Figma_Design_Prompt.md:
 * "Тема: тёмная (dark mode) как основная и единственная").
 */
@Composable
fun KopilkaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KopilkaColorScheme,
        content = content
    )
}
