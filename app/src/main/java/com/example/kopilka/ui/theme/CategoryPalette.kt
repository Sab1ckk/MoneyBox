// Путь в проекте: app/src/main/java/com/example/kopilka/ui/theme/CategoryPalette.kt

package com.example.kopilka.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.kopilka.data.entity.Category

object CategoryPalette {

    /** Палитра для пикера цвета в диалоге добавления категории + fallback для старых записей без цвета. */
    val PRESET_COLORS: List<Color> = listOf(
        Color(0xFF34D399), // зелёный
        Color(0xFF60A5FA), // синий
        Color(0xFFF59E0B), // янтарный
        Color(0xFFA78BFA), // фиолетовый
        Color(0xFFF472B6), // розовый
        Color(0xFFEC4899), // маджента
        Color(0xFF22D3EE), // бирюзовый
        Color(0xFFFB7185)  // коралловый
    )

    private val fallback = Color(0xFF6B7280) // серый — категория не задана

    /** Основной способ получить цвет категории — используется везде в UI. */
    fun colorForCategory(category: Category?): Color {
        val hex = category?.color
        if (!hex.isNullOrBlank()) {
            return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
        }
        return colorFor(category?.id)
    }

    /** Fallback по индексу id — для категорий, у которых поле color ещё не заполнено. */
    fun colorFor(categoryId: Int?): Color {
        if (categoryId == null) return fallback
        val index = ((categoryId % PRESET_COLORS.size) + PRESET_COLORS.size) % PRESET_COLORS.size
        return PRESET_COLORS[index]
    }

    fun toHex(color: Color): String {
        val argb = color.toArgb()
        return String.format("#%06X", 0xFFFFFF and argb)
    }
}
