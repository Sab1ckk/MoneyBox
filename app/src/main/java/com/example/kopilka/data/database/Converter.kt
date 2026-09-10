package com.example.kopilka.data.database
import androidx.room3.DaoReturnTypeConverters
import java.time.LocalDate

class Converters {
    @DaoReturnTypeConverters
    fun fromEpochDay(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }
    @DaoReturnTypeConverters
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()
}