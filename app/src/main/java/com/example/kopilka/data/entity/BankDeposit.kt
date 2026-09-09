package com.example.kopilka.data.entity
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey
import androidx.room3.Index
import java.time.LocalDate

@Entity(
    tableName = "bank_deposits",
    foreignKeys = [ForeignKey(
        entity = Goal::class,
        parentColumns = ["id"],
        childColumns = ["goalId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("goalId", unique = true)] // одна цель — один вклад
)
data class BankDeposit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val goalId: Int,
    val interestRate: Double,        // годовая ставка, % (например 14.0)
    val capitalizationMonths: Int,   // период капитализации: 1 = ежемесячно, 3 = ежеквартально, 12 = раз в год
    val startDate: LocalDate,
    val initialAmount: Double = 0.0  // сумма, положенная на вклад в момент открытия
)
