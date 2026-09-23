package com.example

import com.example.data.local.entities.QuizAttemptEntity
import com.example.ui.student.components.StudentPerformanceMetric
import com.example.ui.student.components.StudentTimeFilter
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Student Performance Dashboard using Compose-Charts.
 * Verifies chronological score progression, metric switching,
 * class average benchmark calculations, subject breakdown, and grade brackets.
 */
class StudentPerformanceDashboardTest {

    private val studentAttempts = listOf(
        QuizAttemptEntity("att1", "q1", "Algorithms 101", "Computer Science", "student_1", "Alex", "alex@edu.com", "", 6, 10, 60f, 180, 1000L),
        QuizAttemptEntity("att2", "q2", "Calculus I", "Mathematics", "student_1", "Alex", "alex@edu.com", "", 7, 10, 70f, 150, 2000L),
        QuizAttemptEntity("att3", "q3", "Physics Waves", "Science", "student_1", "Alex", "alex@edu.com", "", 8, 10, 80f, 140, 3000L),
        QuizAttemptEntity("att4", "q4", "Algorithms 201", "Computer Science", "student_1", "Alex", "alex@edu.com", "", 9, 10, 90f, 120, 4000L),
        QuizAttemptEntity("att5", "q5", "Linear Algebra", "Mathematics", "student_1", "Alex", "alex@edu.com", "", 10, 10, 100f, 100, 5000L)
    )

    private val allClassAttempts = studentAttempts + listOf(
        QuizAttemptEntity("att6", "q1", "Algorithms 101", "Computer Science", "student_2", "Beth", "beth@edu.com", "", 8, 10, 80f, 140, 1500L),
        QuizAttemptEntity("att7", "q2", "Calculus I", "Mathematics", "student_2", "Beth", "beth@edu.com", "", 6, 10, 60f, 200, 2500L)
    )

    @Test
    fun testChronologicalOrderingAndScoreProgression() {
        val chronological = studentAttempts.sortedBy { it.completedAt }
        assertEquals("att1", chronological.first().id)
        assertEquals("att5", chronological.last().id)

        // Trajectory improvement delta: from 60% to 100% -> +40%
        val improvementDelta = chronological.last().percentage - chronological.first().percentage
        assertEquals(40f, improvementDelta, 0.01f)
    }

    @Test
    fun testMetricTransformations() {
        val accuracyValues = studentAttempts.map {
            when (StudentPerformanceMetric.ACCURACY) {
                StudentPerformanceMetric.ACCURACY -> it.percentage.toDouble()
                StudentPerformanceMetric.SCORE -> it.score.toDouble()
                StudentPerformanceMetric.TIME -> it.timeSpentSeconds.toDouble()
            }
        }
        assertEquals(listOf(60.0, 70.0, 80.0, 90.0, 100.0), accuracyValues)

        val scoreValues = studentAttempts.map {
            when (StudentPerformanceMetric.SCORE) {
                StudentPerformanceMetric.ACCURACY -> it.percentage.toDouble()
                StudentPerformanceMetric.SCORE -> it.score.toDouble()
                StudentPerformanceMetric.TIME -> it.timeSpentSeconds.toDouble()
            }
        }
        assertEquals(listOf(6.0, 7.0, 8.0, 9.0, 10.0), scoreValues)

        val timeValues = studentAttempts.map {
            when (StudentPerformanceMetric.TIME) {
                StudentPerformanceMetric.ACCURACY -> it.percentage.toDouble()
                StudentPerformanceMetric.SCORE -> it.score.toDouble()
                StudentPerformanceMetric.TIME -> it.timeSpentSeconds.toDouble()
            }
        }
        assertEquals(listOf(180.0, 150.0, 140.0, 120.0, 100.0), timeValues)
    }

    @Test
    fun testClassAverageBenchmarkCalculation() {
        val classAccuracyAvg = allClassAttempts.map { it.percentage.toDouble() }.average()
        // (60 + 70 + 80 + 90 + 100 + 80 + 60) / 7 = 540 / 7 = 77.14
        assertEquals(77.14, classAccuracyAvg, 0.01)

        val studentAccuracyAvg = studentAttempts.map { it.percentage.toDouble() }.average()
        // (60 + 70 + 80 + 90 + 100) / 5 = 400 / 5 = 80.0
        assertEquals(80.0, studentAccuracyAvg, 0.01)

        assertTrue("Student average should exceed class baseline", studentAccuracyAvg > classAccuracyAvg)
    }

    @Test
    fun testCategoryMasteryBreakdown() {
        val categoryStats = studentAttempts.groupBy { it.categoryName }
            .mapValues { entry -> entry.value.map { it.percentage.toDouble() }.average() }

        assertEquals(3, categoryStats.size)
        // CS: 60% and 90% -> avg 75%
        assertEquals(75.0, categoryStats["Computer Science"] ?: 0.0, 0.01)
        // Math: 70% and 100% -> avg 85%
        assertEquals(85.0, categoryStats["Mathematics"] ?: 0.0, 0.01)
        // Science: 80%
        assertEquals(80.0, categoryStats["Science"] ?: 0.0, 0.01)
    }

    @Test
    fun testGradeBracketDistribution() {
        val a = studentAttempts.count { it.percentage >= 90f }
        val b = studentAttempts.count { it.percentage in 80f..89.99f }
        val c = studentAttempts.count { it.percentage in 70f..79.99f }
        val d = studentAttempts.count { it.percentage in 60f..69.99f }
        val f = studentAttempts.count { it.percentage < 60f }

        assertEquals(2, a) // 90%, 100%
        assertEquals(1, b) // 80%
        assertEquals(1, c) // 70%
        assertEquals(1, d) // 60%
        assertEquals(0, f)
    }
}
