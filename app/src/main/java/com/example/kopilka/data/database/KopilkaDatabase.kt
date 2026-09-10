package com.example.kopilka.data.database

import androidx.room3.Database
import androidx.room3.Room
import android.content.Context
import androidx.room.TypeConverters
import androidx.room3.RoomDatabase
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
abstract class AppDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun depositDao(): DepositDao
    abstract fun categoryDao(): CategoryDao
    abstract fun goalCategoryDao(): GoalCategoryDao
    abstract fun bankDepositDao(): BankDepositDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kopilka_db"
                )
                    .fallbackToDestructiveMigration() // на этапе разработки, до релиза миграции не нужны
                    .build().also { INSTANCE = it }
            }
    }
}