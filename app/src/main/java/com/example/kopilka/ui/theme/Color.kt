package com.example.kopilka.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class KopilkaColors(
    val base: Color, val card: Color, val surface: Color, val elevated: Color,
    val accent: Color, val accentDim: Color, val accentMid: Color, val accentGlow: Color,
    val text: Color, val text2: Color, val text3: Color,
    val error: Color, val errorDim: Color,
    val border: Color, val borderAccent: Color,
    val warning: Color
)

val DarkKopilkaColors = KopilkaColors(
    base = Color(0xFF0A0F0D), card = Color(0xFF131B18), surface = Color(0xFF1C2824), elevated = Color(0xFF243028),
    accent = Color(0xFF34D399), accentDim = Color(0x1A34D399), accentMid = Color(0x3334D399), accentGlow = Color(0x4734D399),
    text = Color(0xFFFFFFFF), text2 = Color(0xFFE5E7EB), text3 = Color(0xFF9CA3AF),
    error = Color(0xFFEF4444), errorDim = Color(0x1AEF4444),
    border = Color(0x0FFFFFFF), borderAccent = Color(0x2E34D399),
    warning = Color(0xFFF59E0B)
)

val LightKopilkaColors = KopilkaColors(
    base = Color(0xFFF7FAF9), card = Color(0xFFFFFFFF), surface = Color(0xFFEFF3F1), elevated = Color(0xFFE3E9E6),
    accent = Color(0xFF10B981), accentDim = Color(0x1A10B981), accentMid = Color(0x3310B981), accentGlow = Color(0x4710B981),
    text = Color(0xFF0A0F0D), text2 = Color(0xFF374151), text3 = Color(0xFF6B7280),
    error = Color(0xFFDC2626), errorDim = Color(0x1ADC2626),
    border = Color(0x14000000), borderAccent = Color(0x2610B981),
    warning = Color(0xFFD97706)
)

val LocalKopilkaColors = staticCompositionLocalOf { DarkKopilkaColors }

// Имена остаются теми же
val ColorBase: Color @Composable get() = LocalKopilkaColors.current.base
val ColorCard: Color @Composable get() = LocalKopilkaColors.current.card
val ColorSurface: Color @Composable get() = LocalKopilkaColors.current.surface
val ColorElevated: Color @Composable get() = LocalKopilkaColors.current.elevated
val ColorAccent: Color @Composable get() = LocalKopilkaColors.current.accent
val ColorAccentDim: Color @Composable get() = LocalKopilkaColors.current.accentDim
val ColorAccentMid: Color @Composable get() = LocalKopilkaColors.current.accentMid
val ColorAccentGlow: Color @Composable get() = LocalKopilkaColors.current.accentGlow
val ColorText: Color @Composable get() = LocalKopilkaColors.current.text
val ColorText2: Color @Composable get() = LocalKopilkaColors.current.text2
val ColorText3: Color @Composable get() = LocalKopilkaColors.current.text3
val ColorError: Color @Composable get() = LocalKopilkaColors.current.error
val ColorErrorDim: Color @Composable get() = LocalKopilkaColors.current.errorDim
val ColorBorder: Color @Composable get() = LocalKopilkaColors.current.border
val ColorBorderAccent: Color @Composable get() = LocalKopilkaColors.current.borderAccent
val ColorWarning: Color @Composable get() = LocalKopilkaColors.current.warning