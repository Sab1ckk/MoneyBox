package com.example.kopilka.data.database

import com.example.kopilka.data.entity.Category
import androidx.compose.material.icons.Icons


object DefaultCategories {
    val list = listOf(
        Category(name = "Путешествие", icon = "✈", color = "#34D399"),
        Category(name = "Электроника", icon = "💻", color = "#60A5FA"),
        Category(name = "Автомобиль", icon = "🚗", color = "#F59E0B"),
        Category(name = "Недвижимость", icon = "🏠", color = "#A78BFA"),
        Category(name = "Образование", icon = "📚", color = "#F472B6"),
        Category(name = "Черный день", icon = "🛡", color = "#34D399"),
        Category(name = "Здоровье", icon = "❤", color = "#EC4899"),
        Category(name = "Другое", icon = "●", color = "#6B7280")
    )

    /** true — категория из предустановленного набора (удалять её нельзя). */
    fun isDefault(category: Category): Boolean = list.any { it.name == category.name }
}
