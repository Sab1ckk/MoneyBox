package com.example.kopilka.data.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Insert
import androidx.room3.Delete
import com.example.kopilka.data.entity.Category
import com.example.kopilka.data.entity.Goal
import com.example.kopilka.data.entity.GoalCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalCategoryDao {
    @Query("""
        SELECT categories.* FROM categories
        INNER JOIN goal_categories ON categories.id = goal_categories.categoryId
        WHERE goal_categories.goalId = :goalId
    """)
    fun getCategoriesForGoal(goalId: Int): Flow<List<Category>>

    @Query("""
        SELECT goals.* FROM goals
        INNER JOIN goal_categories ON goals.id = goal_categories.goalId
        WHERE goal_categories.categoryId = :categoryId
    """)
    fun getGoalsForCategory(categoryId: Int): Flow<List<Goal>>

    @Insert
    suspend fun insert(goalCategory: GoalCategory)

    @Delete
    suspend fun delete(goalCategory: GoalCategory)

    @Query("DELETE FROM goal_categories WHERE goalId = :goalId")
    suspend fun deleteAllForGoal(goalId: Int)
}