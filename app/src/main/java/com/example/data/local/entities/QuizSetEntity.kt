package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_sets")
data class QuizSetEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val categoryId: String,
    val categoryName: String,
    val creatorTeacherId: String,
    val creatorTeacherName: String,
    val durationMinutes: Int = 10,
    val passPercentage: Int = 60,
    val difficulty: String = "Medium", // Easy, Medium, Hard
    val createdAt: Long = System.currentTimeMillis(),
    val isPublished: Boolean = true,
    val tags: String = "" // e.g. "Math, Calculus, STEM"
) {
    fun getTagList(): List<String> {
        if (tags.isBlank()) return emptyList()
        return tags.split(",")
            .map { it.trim().removePrefix("#") }
            .filter { it.isNotBlank() }
    }
}
