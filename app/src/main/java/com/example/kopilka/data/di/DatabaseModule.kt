package com.example.kopilka.data.di

import android.content.Context
import androidx.room.Room
import com.example.kopilka.data.dao.BankDepositDao
import com.example.kopilka.data.dao.CategoryDao
import com.example.kopilka.data.dao.DepositDao
import com.example.kopilka.data.dao.GoalCategoryDao
import com.example.kopilka.data.dao.GoalDao
import com.example.kopilka.data.database.KopilkaDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): KopilkaDatabase {
        return Room.databaseBuilder(context,
            KopilkaDatabase::class.java,
            "kopilka_database").build()
    }

    @Provides
    fun provideGoalDao(
        database: KopilkaDatabase
    ): GoalDao {
        return database.goalDao()
    }

    @Provides
    fun provideDepositDao(
        database: KopilkaDatabase
    ): DepositDao {
        return database.depositDao()
    }

    @Provides
    fun provideCategoryDao(
        database: KopilkaDatabase
    ): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideGoalCategoryDao(
        database: KopilkaDatabase
    ): GoalCategoryDao {
        return database.goalCategoryDao()
    }

    @Provides
    fun provideBankDepositDao(
        database: KopilkaDatabase
    ): BankDepositDao{
        return database.bankDepositDao()
    }

}


