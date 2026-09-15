package com.example.kopilka.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Delete
import com.example.kopilka.data.entity.Deposit
import kotlinx.coroutines.flow.Flow

@Dao
interface DepositDao {
    @Query("SELECT * FROM deposits WHERE goalId = :goalId ORDER BY date DESC")
    fun getDepositsForGoal(goalId: Int): Flow<List<Deposit>>

    @Insert
    suspend fun insert(deposit: Deposit)

    @Delete
    suspend fun delete(deposit: Deposit)
}