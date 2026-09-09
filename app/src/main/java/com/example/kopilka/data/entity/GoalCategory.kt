package com.example.kopilka.data.entity
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

@Entity(
    tableName = "goal_categories",
    primaryKeys = ["goalId", "categoryId"],
    foreignKeys = [
        ForeignKey(entity = Goal::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("goalId"), Index("categoryId")]
)
data class GoalCategory(
    val goalId: Int,
    val categoryId: Int
)