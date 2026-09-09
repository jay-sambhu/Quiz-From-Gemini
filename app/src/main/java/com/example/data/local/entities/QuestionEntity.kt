package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: String,
    val quizSetId: String,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOptionIndex: Int, // 0 for A, 1 for B, 2 for C, 3 for D
    val explanation: String = ""
)
