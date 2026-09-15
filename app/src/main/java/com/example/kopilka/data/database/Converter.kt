package com.example.kopilka.data.database
import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }
    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()
}