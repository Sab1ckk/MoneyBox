package com.example.kopilka.data.database

import androidx.room.Database
import androidx.room.Room
import android.content.Context
import androidx.room.TypeConverters
import androidx.room.RoomDatabase
import com.example.kopilka.data.entity.Goal
import com.example.kopilka.data.entity.GoalCategory
import com.example.kopilka.data.entity.Category
import com.example.kopilka.data.entity.Deposit
import com.example.kopilka.data.entity.BankDeposit
import com.example.kopilka.data.dao.GoalDao
import com.example.kopilka.data.dao.DepositDao
import com.example.kopilka.data.dao.CategoryDao
import com.example.kopilka.data.dao.GoalCategoryDao
import com.example.kopilka.data.dao.BankDepositDao

@Database(
    entities = [Goal::class, Deposit::class, Category::class, GoalCategory::class, BankDeposit::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KopilkaDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun depositDao(): DepositDao
    abstract fun categoryDao(): CategoryDao
    abstract fun goalCategoryDao(): GoalCategoryDao
    abstract fun bankDepositDao(): BankDepositDao

}