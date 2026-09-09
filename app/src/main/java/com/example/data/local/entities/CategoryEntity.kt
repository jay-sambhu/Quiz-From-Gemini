package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val iconName: String = "Category",
    val colorHex: String = "#6200EE",
    val createdAt: Long = System.currentTimeMillis()
)
