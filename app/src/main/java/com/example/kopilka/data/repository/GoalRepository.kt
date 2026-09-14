package com.example.kopilka.data.repository

import com.example.kopilka.data.dao.GoalDao
import com.example.kopilka.data.entity.Goal
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class GoalRepository @Inject constructor(
    private val goalDao: GoalDao
) {
    fun getAllGoals(): Flow<List<Goal>> = goalDao.getAllGoals()
    fun getGoalById(id: Int): Flow<Goal?> = goalDao.getGoalById(id)
    suspend fun insert(goal: Goal): Long = goalDao.insert(goal)
    suspend fun update(goal: Goal) = goalDao.update(goal)
    suspend fun delete(goal: Goal) = goalDao.delete(goal)
}