package com.example.kopilka.data.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import java.time.LocalDate


@Entity(
    tableName = "deposits",
    foreignKeys = [ForeignKey(
        entity = Goal::class,
        parentColumns = ["id"],
        childColumns = ["goalId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("goalId")]
)
data class Deposit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val goalId: Int,
    val amount: Double,
    val date: LocalDate,
    val comment: String = ""
)
