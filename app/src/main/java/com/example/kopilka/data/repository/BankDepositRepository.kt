package com.example.kopilka.data.repository

import com.example.kopilka.data.dao.BankDepositDao
import com.example.kopilka.data.entity.BankDeposit
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BankDepositRepository @Inject constructor(
    private val bankDepositDao: BankDepositDao
) {
    fun getByGoalId(goalId: Int): Flow<BankDeposit?> = bankDepositDao.getByGoalId(goalId)
    suspend fun insert(bankDeposit: BankDeposit) = bankDepositDao.insert(bankDeposit)
    suspend fun update(bankDeposit: BankDeposit) = bankDepositDao.update(bankDeposit)
    suspend fun delete(bankDeposit: BankDeposit) = bankDepositDao.delete(bankDeposit)
}