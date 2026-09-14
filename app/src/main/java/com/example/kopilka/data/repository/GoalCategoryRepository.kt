package com.example.kopilka.data.repository

import com.example.kopilka.data.dao.GoalCategoryDao
import com.example.kopilka.data.entity.Category
import com.example.kopilka.data.entity.Goal
import com.example.kopilka.data.entity.GoalCategory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GoalCategoryRepository@Inject constructor(
    private val goalCategoryDao: GoalCategoryDao
) {
    fun getCategoriesForGoal(goalId: Int): Flow<List<Category>> = goalCategoryDao.getCategoriesForGoal(goalId)
    fun getGoalsForCategory(categoryId: Int): Flow<List<Goal>> = goalCategoryDao.getGoalsForCategory(categoryId)
    suspend fun insert(goalCategory: GoalCategory) = goalCategoryDao.insert(goalCategory)
    suspend fun delete(goalCategory: GoalCategory) = goalCategoryDao.delete(goalCategory)
    suspend fun deleteAllForGoal(goalId: Int) = goalCategoryDao.deleteAllForGoal(goalId)
}