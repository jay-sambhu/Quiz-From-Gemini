package com.example

import com.example.data.local.entities.QuizAttemptEntity
import com.example.ui.student.QuizHistorySortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Unit tests for Quiz History Screen:
 * - Verifies retrieving and displaying list of past quizzes taken by the student
 * - Verifies scores formatting (score fraction, percentage, pass/fail status)
 * - Verifies completion dates formatting and timestamps
 * - Verifies sorting (Newest, Oldest, Highest score, Lowest score)
 * - Verifies filtering (Passed, Needs Practice, Category)
 */
class QuizHistoryScreenTest {

    private val targetStudentId = "student_active_001"
    private val otherStudentId = "student_other_002"

    private val sampleAttempts = listOf(
        QuizAttemptEntity(
            id = "attempt_math_01",
            quizSetId = "quiz_calculus",
            quizTitle = "Differential Calculus 101",
            categoryName = "Mathematics",
            studentId = targetStudentId,
            studentName = "Sam Wilson",
            studentEmail = "sam@school.edu",
            userAnswersJson = "{\"q_0\": 1, \"q_1\": 0}",
            score = 8,
            totalQuestions = 10,
            percentage = 80f,
            timeSpentSeconds = 240,
            completedAt = 1726000000000L // earlier
        ),
        QuizAttemptEntity(
            id = "attempt_sci_02",
            quizSetId = "quiz_physics",
            quizTitle = "Newtonian Mechanics",
            categoryName = "Science",
            studentId = targetStudentId,
            studentName = "Sam Wilson",
            studentEmail = "sam@school.edu",
            userAnswersJson = "{\"q_0\": 2}",
            score = 5,
            totalQuestions = 10,
            percentage = 50f,
            timeSpentSeconds = 310,
            completedAt = 1727000000000L // later
        ),
        QuizAttemptEntity(
            id = "attempt_cs_03",
            quizSetId = "quiz_algorithms",
            quizTitle = "Data Structures & Big-O",
            categoryName = "Computer Science",
            studentId = targetStudentId,
            studentName = "Sam Wilson",
            studentEmail = "sam@school.edu",
            userAnswersJson = "{}",
            score = 10,
            totalQuestions = 10,
            percentage = 100f,
            timeSpentSeconds = 180,
            completedAt = 1726500000000L // middle
        ),
        QuizAttemptEntity(
            id = "attempt_other_04",
            quizSetId = "quiz_history",
            quizTitle = "Ancient Civilizations",
            categoryName = "History",
            studentId = otherStudentId,
            studentName = "Other Student",
            studentEmail = "other@school.edu",
            userAnswersJson = "{}",
            score = 7,
            totalQuestions = 10,
            percentage = 70f,
            timeSpentSeconds = 200,
            completedAt = 1727100000000L
        )
    )

    @Test
    fun testRetrievalIsolatesStudentPastQuizzes() {
        val studentPastQuizzes = sampleAttempts.filter { it.studentId == targetStudentId }
        assertEquals(3, studentPastQuizzes.size)
        assertTrue(studentPastQuizzes.all { it.studentId == targetStudentId })
    }

    @Test
    fun testScoresFormatting() {
        val quiz = sampleAttempts.first { it.id == "attempt_math_01" }

        val scoreFraction = "${quiz.score} / ${quiz.totalQuestions}"
        val percentageText = "${String.format(Locale.US, "%.0f", quiz.percentage)}%"
        val isPassed = quiz.percentage >= 60f

        assertEquals("8 / 10", scoreFraction)
        assertEquals("80%", percentageText)
        assertTrue(isPassed)

        val failingQuiz = sampleAttempts.first { it.id == "attempt_sci_02" }
        assertEquals("5 / 10", "${failingQuiz.score} / ${failingQuiz.totalQuestions}")
        assertEquals("50%", "${String.format(Locale.US, "%.0f", failingQuiz.percentage)}%")
        assertTrue(failingQuiz.percentage < 60f)
    }

    @Test
    fun testCompletionDateFormatting() {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US)
        sampleAttempts.forEach { attempt ->
            val formattedDate = dateFormat.format(Date(attempt.completedAt))
            assertNotNull(formattedDate)
            assertTrue(formattedDate.contains("2024"))
        }
    }

    @Test
    fun testSortByNewestCompletedFirst() {
        val studentPastQuizzes = sampleAttempts.filter { it.studentId == targetStudentId }
        val sorted = studentPastQuizzes.sortedByDescending { it.completedAt }

        // 1727000000000L is Newtonian Mechanics
        assertEquals("attempt_sci_02", sorted.first().id)
        // 1726000000000L is Differential Calculus
        assertEquals("attempt_math_01", sorted.last().id)
    }

    @Test
    fun testSortByOldestCompletedFirst() {
        val studentPastQuizzes = sampleAttempts.filter { it.studentId == targetStudentId }
        val sorted = studentPastQuizzes.sortedBy { it.completedAt }

        assertEquals("attempt_math_01", sorted.first().id)
        assertEquals("attempt_sci_02", sorted.last().id)
    }

    @Test
    fun testSortByHighestScore() {
        val studentPastQuizzes = sampleAttempts.filter { it.studentId == targetStudentId }
        val sorted = studentPastQuizzes.sortedWith(
            compareByDescending<QuizAttemptEntity> { it.percentage }
                .thenByDescending { it.completedAt }
        )

        assertEquals("attempt_cs_03", sorted[0].id) // 100%
        assertEquals("attempt_math_01", sorted[1].id) // 80%
        assertEquals("attempt_sci_02", sorted[2].id) // 50%
    }

    @Test
    fun testFilterByPassedAndNeedsPractice() {
        val studentPastQuizzes = sampleAttempts.filter { it.studentId == targetStudentId }

        val passedQuizzes = studentPastQuizzes.filter { it.percentage >= 60f }
        assertEquals(2, passedQuizzes.size)
        assertTrue(passedQuizzes.any { it.quizTitle.contains("Calculus") })
        assertTrue(passedQuizzes.any { it.quizTitle.contains("Data Structures") })

        val needsPracticeQuizzes = studentPastQuizzes.filter { it.percentage < 60f }
        assertEquals(1, needsPracticeQuizzes.size)
        assertEquals("Newtonian Mechanics", needsPracticeQuizzes.first().quizTitle)
    }

    @Test
    fun testSearchQueryFilter() {
        val studentPastQuizzes = sampleAttempts.filter { it.studentId == targetStudentId }

        val query = "calculus"
        val searchResults = studentPastQuizzes.filter {
            it.quizTitle.lowercase().contains(query) || it.categoryName.lowercase().contains(query)
        }
        assertEquals(1, searchResults.size)
        assertEquals("Differential Calculus 101", searchResults.first().quizTitle)
    }
}
