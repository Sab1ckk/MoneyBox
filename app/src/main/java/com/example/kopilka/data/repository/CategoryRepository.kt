package com.example.kopilka.data.repository

import com.example.kopilka.data.dao.CategoryDao
import com.example.kopilka.data.entity.Category
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategoryRepository@Inject constructor(
    private val categoryDao: CategoryDao
){
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()
    fun getCategoryById(id: Int): Flow<Category?> = categoryDao.getCategoryById(id)
    suspend fun insert(category: Category): Long = categoryDao.insert(category)
    suspend fun update(category: Category) = categoryDao.update(category)
    suspend fun delete(category: Category) = categoryDao.delete(category)
}