package com.example.kopilka.data.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Insert
import androidx.room3.Update
import androidx.room3.Delete
import com.example.kopilka.data.entity.BankDeposit
import kotlinx.coroutines.flow.Flow

@Dao
interface BankDepositDao {
    @Query("SELECT * FROM bank_deposits WHERE goalId = :goalId")
    fun getByGoalId(goalId: Int): Flow<BankDeposit?>

    @Insert
    suspend fun insert(bankDeposit: BankDeposit)

    @Update
    suspend fun update(bankDeposit: BankDeposit)

    @Delete
    suspend fun delete(bankDeposit: BankDeposit)
}