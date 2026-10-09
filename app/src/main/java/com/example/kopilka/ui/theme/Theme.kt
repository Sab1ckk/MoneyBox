// Путь в проекте: app/src/main/java/com/example/kopilka/ui/theme/Theme.kt

package com.example.kopilka.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.kopilka.data.datastore.ThemeModeSetting

enum class ThemeMode { LIGHT, DARK, SYSTEM }

fun ThemeModeSetting.toThemeMode(): ThemeMode = when (this) {
    ThemeModeSetting.LIGHT -> ThemeMode.LIGHT
    ThemeModeSetting.DARK -> ThemeMode.DARK
    ThemeModeSetting.SYSTEM -> ThemeMode.SYSTEM
}

@Composable
fun KopilkaTheme(themeMode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val palette = if (darkTheme) DarkKopilkaColors else LightKopilkaColors

    CompositionLocalProvider(LocalKopilkaColors provides palette) {
        val colorScheme = if (darkTheme) {
            darkColorScheme(
                background = palette.base,
                surface = palette.card,
                surfaceVariant = palette.surface,
                primary = palette.accent,
                onPrimary = palette.base,
                onBackground = palette.text,
                onSurface = palette.text,
                error = palette.error,
                onError = palette.text
            )
        } else {
            lightColorScheme(
                background = palette.base,
                surface = palette.card,
                surfaceVariant = palette.surface,
                primary = palette.accent,
                onPrimary = palette.base,
                onBackground = palette.text,
                onSurface = palette.text,
                error = palette.error,
                onError = palette.text
            )
        }
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
