package com.example.kopilka.data.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val targetAmount: Double,
    val deadline: LocalDate,
    val createdDate: LocalDate,
    val isCompleted: Boolean = false
)