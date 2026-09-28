package com.example.kopilka.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import com.example.kopilka.data.entity.BankDeposit
import kotlinx.coroutines.flow.Flow

@Dao
interface BankDepositDao {
    @Query("SELECT * FROM bank_deposits WHERE goalId = :goalId")
    fun getByGoalId(goalId: Int): Flow<BankDeposit?>

    @Query("SELECT * FROM bank_deposits")
    suspend fun getAllOnce(): List<BankDeposit>

    @Insert
    suspend fun insert(bankDeposit: BankDeposit)
    @Update
    suspend fun update(bankDeposit: BankDeposit)

    @Delete
    suspend fun delete(bankDeposit: BankDeposit)

    @Query("DELETE FROM bank_deposits")
    suspend fun deleteAll()
}