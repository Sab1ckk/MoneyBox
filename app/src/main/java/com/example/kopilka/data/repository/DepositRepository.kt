package com.example.kopilka.data.repository

import com.example.kopilka.data.dao.DepositDao
import com.example.kopilka.data.entity.Deposit
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DepositRepository@Inject constructor(
    private val depositDao: DepositDao
) {
    fun getDepositsForGoal(goalId: Int): Flow<List<Deposit>> = depositDao.getDepositsForGoal(goalId)
    suspend fun insert(deposit: Deposit) = depositDao.insert(deposit)
    suspend fun delete(deposit: Deposit) = depositDao.delete(deposit)
}