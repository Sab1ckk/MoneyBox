// Путь в проекте: app/src/main/java/com/example/kopilka/utils/Formatters.kt

package com.example.kopilka.utils

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val RU_LOCALE = Locale("ru", "RU")

/** "127 500 ₽" */
fun formatFullAmount(amount: Double): String {
    val nf = NumberFormat.getNumberInstance(RU_LOCALE)
    nf.maximumFractionDigits = 0
    return "${nf.format(amount)} ₽"
}

/** "1 нояб. 2024 г." */
fun formatDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", RU_LOCALE)
    return date.format(formatter)
}

fun daysUntil(date: LocalDate): Long {
    return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date)
}

fun calcProgressPercent(saved: Double, target: Double): Int {
    if (target <= 0.0) return 0
    return ((saved / target) * 100).toInt().coerceIn(0, 100)
}
