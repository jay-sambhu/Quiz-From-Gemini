package com.example.data.gemini

import android.content.Context
import com.example.data.local.entities.QuestionEntity
import com.example.data.model.QuizQuestion

object GeminiQuizGenerator {

    private val service = GeminiQuizService.getInstance()

    suspend fun generateQuestionsForTopic(
        topic: String,
        quizSetId: String,
        count: Int = 5,
        difficultyLevel: String = "Medium"
    ): List<QuestionEntity> {
        return service.generateQuizQuestions(
            topic = topic,
            difficultyLevel = difficultyLevel,
            count = count,
            quizSetId = quizSetId
        )
    }

    suspend fun generateQuizQuestions(
        context: Context,
        topic: String,
        difficultyLevel: String = "Medium",
        count: Int = 5,
        category: String = "",
        tag: String = "",
        quizSetId: String = ""
    ): List<QuizQuestion> {
        val apiKeyManager = GeminiApiKeyManager.getInstance(context)
        val questionService = GeminiQuizQuestionService.getInstance(apiKeyManager)
        return questionService.generateQuizQuestions(
            topic = topic,
            difficultyLevel = difficultyLevel,
            count = count,
            category = category,
            tag = tag,
            quizSetId = quizSetId
        )
    }
}
