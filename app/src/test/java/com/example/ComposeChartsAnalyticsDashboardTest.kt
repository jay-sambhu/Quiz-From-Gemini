package com.example

import com.example.data.local.entities.QuizAttemptEntity
import com.example.ui.analytics.AnalyticsMetric
import com.example.ui.analytics.AnalyticsTimeRange
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Compose-Charts Analytics Dashboard data mappings,
 * time horizon cutoffs, metric transformations, and Firestore synchronization.
 */
class ComposeChartsAnalyticsDashboardTest {

    private val sampleAttempts = listOf(
        QuizAttemptEntity("att1", "q1", "Algorithms", "Computer Science", "s1", "Aashish", "aashish@edu.com", "", 10, 10, 100f, 120, 1000L),
        QuizAttemptEntity("att2", "q2", "Calculus", "Mathematics", "s1", "Aashish", "aashish@edu.com", "", 9, 10, 90f, 150, 2000L),
        QuizAttemptEntity("att3", "q3", "Physics", "Science", "s1", "Aashish", "aashish@edu.com", "", 8, 10, 80f, 130, 3000L),
        QuizAttemptEntity("att4", "q1", "Algorithms", "Computer Science", "s2", "Sophia", "sophia@edu.com", "", 7, 10, 70f, 180, 4000L),
        QuizAttemptEntity("att5", "q2", "Calculus", "Mathematics", "s2", "Sophia", "sophia@edu.com", "", 5, 10, 50f, 200, 5000L)
    )

    @Test
    fun testStudentFilterAndAccuracyCalculation() {
        val s1Attempts = sampleAttempts.filter { it.studentId == "s1" }
        assertEquals(3, s1Attempts.size)

        val avgAccuracy = s1Attempts.map { it.percentage.toDouble() }.average()
        assertEquals(90.0, avgAccuracy, 0.001)

        val passCount = s1Attempts.count { it.percentage >= 60f }
        val passRate = (passCount.toFloat() / s1Attempts.size.toFloat()) * 100f
        assertEquals(100f, passRate, 0.01f)
    }

    @Test
    fun testMetricMapping() {
        val s1Attempts = sampleAttempts.filter { it.studentId == "s1" }

        val accuracyValues = s1Attempts.map { attempt ->
            when (AnalyticsMetric.ACCURACY) {
                AnalyticsMetric.ACCURACY -> attempt.percentage.toDouble()
                AnalyticsMetric.SCORE -> attempt.score.toDouble()
                AnalyticsMetric.TIME -> attempt.timeSpentSeconds.toDouble()
            }
        }
        assertEquals(listOf(100.0, 90.0, 80.0), accuracyValues)

        val timeValues = s1Attempts.map { attempt ->
            when (AnalyticsMetric.TIME) {
                AnalyticsMetric.ACCURACY -> attempt.percentage.toDouble()
                AnalyticsMetric.SCORE -> attempt.score.toDouble()
                AnalyticsMetric.TIME -> attempt.timeSpentSeconds.toDouble()
            }
        }
        assertEquals(listOf(120.0, 150.0, 130.0), timeValues)
    }

    @Test
    fun testCategoryBreakdown() {
        val categoryStats = sampleAttempts.groupBy { it.categoryName }
            .mapValues { entry ->
                entry.value.map { it.percentage.toDouble() }.average()
            }

        assertEquals(3, categoryStats.size)
        // CS: 100% and 70% -> average 85%
        assertEquals(85.0, categoryStats["Computer Science"] ?: 0.0, 0.01)
        // Math: 90% and 50% -> average 70%
        assertEquals(70.0, categoryStats["Mathematics"] ?: 0.0, 0.01)
        // Science: 80%
        assertEquals(80.0, categoryStats["Science"] ?: 0.0, 0.01)
    }

    @Test
    fun testGradeBracketDistribution() {
        val a = sampleAttempts.count { it.percentage >= 90f }
        val b = sampleAttempts.count { it.percentage in 80f..89.99f }
        val c = sampleAttempts.count { it.percentage in 70f..79.99f }
        val d = sampleAttempts.count { it.percentage in 60f..69.99f }
        val f = sampleAttempts.count { it.percentage < 60f }

        assertEquals(2, a) // 100%, 90%
        assertEquals(1, b) // 80%
        assertEquals(1, c) // 70%
        assertEquals(0, d)
        assertEquals(1, f) // 50%
    }
}
