package com.example.kopilka.data.entity
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val icon: String = ""
)
