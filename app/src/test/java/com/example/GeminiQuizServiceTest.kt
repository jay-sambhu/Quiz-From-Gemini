package com.example

import com.example.data.gemini.GeminiQuizService
import com.example.data.gemini.QuizDifficulty
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GeminiQuizServiceTest {

    @Test
    fun testQuizDifficultyParsing() {
        assertEquals(QuizDifficulty.EASY, QuizDifficulty.fromString("Easy"))
        assertEquals(QuizDifficulty.EASY, QuizDifficulty.fromString("easy"))
        assertEquals(QuizDifficulty.MEDIUM, QuizDifficulty.fromString("Medium"))
        assertEquals(QuizDifficulty.HARD, QuizDifficulty.fromString("Hard"))
        assertEquals(QuizDifficulty.MEDIUM, QuizDifficulty.fromString("Unknown"))
    }

    @Test
    fun testFallbackGenerationForDifficulties() = runBlocking {
        val service = GeminiQuizService(apiKey = "")
        val easyQuestions = service.generateFallbackQuestions("Kotlin Coroutines", QuizDifficulty.EASY, 3, "quiz_123")
        assertEquals(3, easyQuestions.size)
        assertTrue(easyQuestions.all { it.questionText.isNotBlank() })
        assertTrue(easyQuestions.all { it.correctOptionIndex in 0..3 })
        assertTrue(easyQuestions.all { it.explanation.isNotBlank() })

        val hardQuestions = service.generateFallbackQuestions("Kotlin Coroutines", QuizDifficulty.HARD, 4, "quiz_123")
        assertEquals(4, hardQuestions.size)
        assertTrue(hardQuestions.any { it.questionText.contains("concurrent", ignoreCase = true) || it.questionText.contains("distributed", ignoreCase = true) || it.questionText.contains("bottleneck", ignoreCase = true) })
    }

    @Test
    fun testSuggestSingleQuestionFallback() = runBlocking {
        val service = GeminiQuizService(apiKey = "")
        val suggestion = service.suggestSingleQuestion(
            topic = "Operating Systems",
            promptHint = "Deadlock prevention",
            difficultyLevel = "Hard"
        )
        assertNotNull(suggestion)
        assertTrue(suggestion.questionText.isNotBlank())
        assertTrue(suggestion.optionA.isNotBlank())
        assertTrue(suggestion.optionB.isNotBlank())
        assertTrue(suggestion.correctOptionIndex in 0..3)
        assertTrue(suggestion.explanation.isNotBlank())
    }

    @Test
    fun testSuggestDistractorsAndExplanationFallback() = runBlocking {
        val service = GeminiQuizService(apiKey = "")
        val result = service.suggestDistractorsAndExplanation(
            questionText = "Which scheduling algorithm avoids starvation?",
            topic = "Operating Systems",
            difficultyLevel = "Medium"
        )
        assertEquals("Which scheduling algorithm avoids starvation?", result.questionText)
        assertTrue(result.optionA.isNotBlank())
        assertTrue(result.optionB.isNotBlank())
        assertTrue(result.explanation.contains("Which scheduling algorithm avoids starvation?"))
    }
}
