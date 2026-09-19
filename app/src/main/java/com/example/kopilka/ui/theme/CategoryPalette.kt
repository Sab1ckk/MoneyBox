package com.example.kopilka.ui.theme

import androidx.compose.ui.graphics.Color

object CategoryPalette {
    private val colors = listOf(
        ColorAccent,
        Color(0xFF60A5FA), // blue
        Color(0xFFF59E0B), // amber
        Color(0xFFA78BFA), // purple
        Color(0xFFF472B6), // pink
        Color(0xFFEC4899), // magenta
    )
    private val fallback = Color(0xFF6B7280) // grey — категория не задана

    fun colorFor(categoryId: Int?): Color {
        if (categoryId == null) return fallback
        val index = ((categoryId % colors.size) + colors.size) % colors.size
        return colors[index]
    }
}
