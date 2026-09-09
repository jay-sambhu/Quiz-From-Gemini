package com.example

import com.example.data.gemini.GeminiQuizService
import com.example.data.local.entities.QuestionEntity
import com.example.data.local.entities.QuizSetEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class TeacherQuestionEditorTest {

    private val sampleQuizSet = QuizSetEntity(
        id = "quiz_test_101",
        title = "Cloud Infrastructure & Distributed Systems",
        description = "Advanced system design assessment",
        categoryId = "cat_cs",
        categoryName = "Computer Science",
        creatorTeacherId = "teacher_1",
        creatorTeacherName = "Prof. Alan Turing",
        durationMinutes = 15,
        passPercentage = 75,
        difficulty = "Medium",
        createdAt = System.currentTimeMillis()
    )

    @Test
    fun testManualQuestionEntityCreation() {
        val qId = "q_man_${UUID.randomUUID().toString().take(8)}"
        val question = QuestionEntity(
            id = qId,
            quizSetId = sampleQuizSet.id,
            questionText = "What principle states that no system can simultaneously provide Consistency, Availability, and Partition tolerance?",
            optionA = "CAP Theorem",
            optionB = "Amdahl's Law",
            optionC = "Little's Law",
            optionD = "Moore's Law",
            correctOptionIndex = 0,
            explanation = "The CAP theorem (Brewer's theorem) states that a distributed data store cannot guarantee all three simultaneously."
        )

        assertEquals(qId, question.id)
        assertEquals(sampleQuizSet.id, question.quizSetId)
        assertEquals("CAP Theorem", question.optionA)
        assertEquals(0, question.correctOptionIndex)
        assertTrue(question.explanation.contains("Brewer's theorem"))
    }

    @Test
    fun testQuestionValidationLogic() {
        val emptyQuestionText = ""
        val emptyOptionA = ""
        val validOptionB = "B"

        val isValid = emptyQuestionText.isNotBlank() && emptyOptionA.isNotBlank() && validOptionB.isNotBlank()
        assertFalse("Question with empty text should be invalid", isValid)

        val validQuestionText = "Valid question?"
        val validOptionA = "Choice A"
        val validCheck = validQuestionText.isNotBlank() && validOptionA.isNotBlank() && validOptionB.isNotBlank()
        assertTrue("Question with text and at least 2 options should be valid", validCheck)
    }

    @Test
    fun testAiQuestionSuggestionIntegration() = runBlocking {
        val geminiService = GeminiQuizService(apiKey = "")
        val suggestion = geminiService.suggestSingleQuestion(
            topic = sampleQuizSet.title,
            promptHint = "CAP Theorem trade-offs",
            difficultyLevel = "Medium"
        )

        assertNotNull(suggestion)
        assertTrue(suggestion.questionText.isNotBlank())
        assertTrue(suggestion.optionA.isNotBlank())
        assertTrue(suggestion.optionB.isNotBlank())
        assertTrue(suggestion.optionC.isNotBlank())
        assertTrue(suggestion.optionD.isNotBlank())
        assertTrue(suggestion.correctOptionIndex in 0..3)
        assertTrue(suggestion.explanation.isNotBlank())

        // Map into QuestionEntity for saving
        val newQuestion = QuestionEntity(
            id = "q_from_ai_1",
            quizSetId = sampleQuizSet.id,
            questionText = suggestion.questionText,
            optionA = suggestion.optionA,
            optionB = suggestion.optionB,
            optionC = suggestion.optionC,
            optionD = suggestion.optionD,
            correctOptionIndex = suggestion.correctOptionIndex,
            explanation = suggestion.explanation
        )

        assertEquals("q_from_ai_1", newQuestion.id)
        assertEquals(sampleQuizSet.id, newQuestion.quizSetId)
        assertEquals(suggestion.questionText, newQuestion.questionText)
    }

    @Test
    fun testAiDistractorsSuggestionForTeacherQuestion() = runBlocking {
        val geminiService = GeminiQuizService(apiKey = "")
        val teacherQuestion = "What data structure does Redis primarily use to represent Sorted Sets?"

        val result = geminiService.suggestDistractorsAndExplanation(
            questionText = teacherQuestion,
            topic = "Database Engineering",
            difficultyLevel = "Hard"
        )

        assertEquals(teacherQuestion, result.questionText)
        assertTrue(result.optionA.isNotBlank())
        assertTrue(result.optionB.isNotBlank())
        assertTrue(result.optionC.isNotBlank())
        assertTrue(result.optionD.isNotBlank())
        assertTrue(result.explanation.isNotBlank())
    }

    @Test
    fun testQuestionDuplicateLogic() {
        val original = QuestionEntity(
            id = "q_orig_1",
            quizSetId = "set_1",
            questionText = "Original question text?",
            optionA = "A",
            optionB = "B",
            optionC = "C",
            optionD = "D",
            correctOptionIndex = 1,
            explanation = "Explanation"
        )

        val duplicate = original.copy(
            id = "q_orig_1_copy",
            questionText = "${original.questionText} (Copy)"
        )

        assertEquals("q_orig_1_copy", duplicate.id)
        assertEquals("Original question text? (Copy)", duplicate.questionText)
        assertEquals(original.correctOptionIndex, duplicate.correctOptionIndex)
        assertEquals(original.optionA, duplicate.optionA)
    }
}
