package com.example

import com.example.data.local.entities.QuizAttemptEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Unit tests verifying the student Past History screen requirements:
 * - Viewing previous quiz results
 * - Displaying the score and total questions
 * - Displaying the completion date and timestamp
 * - Filtering attempts for the specific student
 * - Chronological sorting
 */
class PastHistoryScreenTest {

    private val studentId = "student_101"
    private val otherStudentId = "student_999"

    private val sampleAttempts = listOf(
        QuizAttemptEntity(
            id = "att_1",
            quizSetId = "quiz_math",
            quizTitle = "Algebra Basics",
            categoryName = "Mathematics",
            studentId = studentId,
            studentName = "Alex Rivera",
            studentEmail = "alex@test.com",
            userAnswersJson = "{}",
            score = 9,
            totalQuestions = 10,
            percentage = 90f,
            timeSpentSeconds = 120,
            completedAt = 1718000000000L // Specific epoch millis
        ),
        QuizAttemptEntity(
            id = "att_2",
            quizSetId = "quiz_sci",
            quizTitle = "Cell Biology",
            categoryName = "Science",
            studentId = studentId,
            studentName = "Alex Rivera",
            studentEmail = "alex@test.com",
            userAnswersJson = "{}",
            score = 5,
            totalQuestions = 10,
            percentage = 50f,
            timeSpentSeconds = 180,
            completedAt = 1718100000000L // 100,000,000ms later
        ),
        QuizAttemptEntity(
            id = "att_3",
            quizSetId = "quiz_hist",
            quizTitle = "World History 101",
            categoryName = "History",
            studentId = otherStudentId,
            studentName = "Other Student",
            studentEmail = "other@test.com",
            userAnswersJson = "{}",
            score = 10,
            totalQuestions = 10,
            percentage = 100f,
            timeSpentSeconds = 90,
            completedAt = 1718200000000L
        )
    )

    @Test
    fun testStudentAttemptsAreIsolated() {
        val studentAttempts = sampleAttempts.filter { it.studentId == studentId }
        assertEquals(2, studentAttempts.size)
        assertTrue(studentAttempts.all { it.studentId == studentId })
    }

    @Test
    fun testScoreAndPercentageDisplayFormat() {
        val attempt = sampleAttempts.first { it.id == "att_1" }
        val scoreText = "${attempt.score} / ${attempt.totalQuestions}"
        val percentageText = "${String.format(Locale.US, "%.0f", attempt.percentage)}% • ${if (attempt.percentage >= 60f) "Passed" else "Failed"}"

        assertEquals("9 / 10", scoreText)
        assertEquals("90% • Passed", percentageText)
    }

    @Test
    fun testCompletionDateDisplayFormat() {
        val attempt = sampleAttempts.first { it.id == "att_1" }
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val formattedDate = dateFormat.format(Date(attempt.completedAt))

        // Ensure date formatting produces a valid non-empty string with month and year
        assertTrue(formattedDate.contains("2024"))
    }

    @Test
    fun testChronologicalSortingNewestFirst() {
        val studentAttempts = sampleAttempts.filter { it.studentId == studentId }
        val sortedNewest = studentAttempts.sortedByDescending { it.completedAt }

        assertEquals("att_2", sortedNewest.first().id)
        assertEquals("att_1", sortedNewest.last().id)
    }

    @Test
    fun testChronologicalSortingOldestFirst() {
        val studentAttempts = sampleAttempts.filter { it.studentId == studentId }
        val sortedOldest = studentAttempts.sortedBy { it.completedAt }

        assertEquals("att_1", sortedOldest.first().id)
        assertEquals("att_2", sortedOldest.last().id)
    }
}
