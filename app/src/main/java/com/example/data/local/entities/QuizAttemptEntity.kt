package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_attempts")
data class QuizAttemptEntity(
    @PrimaryKey val id: String,
    val quizSetId: String,
    val quizTitle: String,
    val categoryName: String,
    val studentId: String,
    val studentName: String,
    val studentEmail: String,
    val studentPhotoUrl: String = "",
    val score: Int,
    val totalQuestions: Int,
    val percentage: Float,
    val timeSpentSeconds: Int,
    val completedAt: Long = System.currentTimeMillis(),
    val userAnswersJson: String = "{}" // Mapping of questionId -> selectedOptionIndex
)
