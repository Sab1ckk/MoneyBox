package com.example.kopilka.data.entity
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

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