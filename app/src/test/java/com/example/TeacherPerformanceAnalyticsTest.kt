package com.example

import com.example.data.local.entities.QuizAttemptEntity
import com.example.data.local.entities.QuizSetEntity
import com.example.data.local.entities.UserEntity
import com.example.data.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Teacher Dashboard Performance Analytics, Recharts bar chart data models,
 * and grade distribution metrics.
 */
class TeacherPerformanceAnalyticsTest {

    private val sampleStudents = listOf(
        UserEntity("s1", "Aashish Shrestha", "aashish@edu.com", "", UserRole.STUDENT),
        UserEntity("s2", "Sophia Patel", "sophia@edu.com", "", UserRole.STUDENT),
        UserEntity("s3", "Marcus Chen", "marcus@edu.com", "", UserRole.STUDENT),
        UserEntity("s4", "Elena Rostova", "elena@edu.com", "", UserRole.STUDENT)
    )

    private val sampleQuizSets = listOf(
        QuizSetEntity("q1", "Algorithms & Data Structures", "CS", "cat1", "Computer Science", "t1", "Prof. Alan", 15, 60, "Medium"),
        QuizSetEntity("q2", "Calculus & Linear Algebra", "Math", "cat2", "Mathematics", "t1", "Prof. Alan", 20, 70, "Hard")
    )

    private val sampleAttempts = listOf(
        QuizAttemptEntity("att1", "q1", "Algorithms", "CS", "s1", "Aashish", "aashish@edu.com", "", 10, 10, 100f, 120, 1000L),
        QuizAttemptEntity("att2", "q2", "Calculus", "Math", "s1", "Aashish", "aashish@edu.com", "", 9, 10, 90f, 150, 1100L),
        QuizAttemptEntity("att3", "q1", "Algorithms", "CS", "s2", "Sophia", "sophia@edu.com", "", 8, 10, 80f, 130, 1200L),
        QuizAttemptEntity("att4", "q2", "Calculus", "Math", "s2", "Sophia", "sophia@edu.com", "", 8, 10, 80f, 140, 1300L),
        QuizAttemptEntity("att5", "q1", "Algorithms", "CS", "s3", "Marcus", "marcus@edu.com", "", 7, 10, 70f, 160, 1400L),
        QuizAttemptEntity("att6", "q1", "Algorithms", "CS", "s4", "Elena", "elena@edu.com", "", 5, 10, 50f, 170, 1500L)
    )

    @Test
    fun testStudentPerformanceSummaryAggregation() {
        val s1Attempts = sampleAttempts.filter { it.studentId == "s1" }
        assertEquals(2, s1Attempts.size)

        val avgScore = s1Attempts.map { it.percentage }.average().toFloat()
        assertEquals(95f, avgScore, 0.01f)

        val passRate = (s1Attempts.count { it.percentage >= 60f }.toFloat() / s1Attempts.size.toFloat()) * 100f
        assertEquals(100f, passRate, 0.01f)

        val summary = StudentPerformanceSummary(
            studentId = "s1",
            studentName = "Aashish Shrestha",
            studentEmail = "aashish@edu.com",
            quizzesTaken = s1Attempts.size,
            averageScore = avgScore,
            highestScore = s1Attempts.maxOf { it.percentage },
            passRate = passRate,
            totalScore = s1Attempts.sumOf { it.score },
            gradeTier = "A (Honors)"
        )

        assertEquals("s1", summary.studentId)
        assertEquals(95f, summary.averageScore, 0.01f)
        assertEquals(100f, summary.highestScore, 0.01f)
        assertEquals(19, summary.totalScore)
        assertEquals("A (Honors)", summary.gradeTier)
    }

    @Test
    fun testQuizPerformanceSummaryAggregation() {
        val q1Attempts = sampleAttempts.filter { it.quizSetId == "q1" }
        assertEquals(4, q1Attempts.size)

        val avgScore = q1Attempts.map { it.percentage }.average().toFloat()
        // (100 + 80 + 70 + 50) / 4 = 300 / 4 = 75%
        assertEquals(75f, avgScore, 0.01f)

        val passCount = q1Attempts.count { it.percentage >= 60f } // 100, 80, 70 pass -> 3
        val passRate = (passCount.toFloat() / q1Attempts.size.toFloat()) * 100f
        assertEquals(75f, passRate, 0.01f)

        val quizSummary = QuizPerformanceSummary(
            quizSetId = "q1",
            quizTitle = "Algorithms & Data Structures",
            categoryName = "Computer Science",
            totalAttempts = q1Attempts.size,
            averageScore = avgScore,
            passRate = passRate
        )

        assertEquals(4, quizSummary.totalAttempts)
        assertEquals(75f, quizSummary.averageScore, 0.01f)
        assertEquals(75f, quizSummary.passRate, 0.01f)
    }

    @Test
    fun testGradeDistributionCalculations() {
        val totalAttempts = sampleAttempts.size // 6 attempts: 100, 90, 80, 80, 70, 50
        assertEquals(6, totalAttempts)

        val aCount = sampleAttempts.count { it.percentage >= 90f } // 100, 90 -> 2
        val bCount = sampleAttempts.count { it.percentage in 80f..89.9f } // 80, 80 -> 2
        val cCount = sampleAttempts.count { it.percentage in 70f..79.9f } // 70 -> 1
        val dCount = sampleAttempts.count { it.percentage in 60f..69.9f } // 0
        val fCount = sampleAttempts.count { it.percentage < 60f } // 50 -> 1

        assertEquals(2, aCount)
        assertEquals(2, bCount)
        assertEquals(1, cCount)
        assertEquals(0, dCount)
        assertEquals(1, fCount)

        val aPct = (aCount.toFloat() / totalAttempts.toFloat()) * 100f
        assertEquals(33.33f, aPct, 0.1f)

        val fPct = (fCount.toFloat() / totalAttempts.toFloat()) * 100f
        assertEquals(16.67f, fPct, 0.1f)
    }

    @Test
    fun testOverallClassAveragesAndRankings() {
        val allPct = sampleAttempts.map { it.percentage }
        val overallAvg = allPct.average().toFloat()
        // (100 + 90 + 80 + 80 + 70 + 50) / 6 = 470 / 6 = 78.333%
        assertEquals(78.33f, overallAvg, 0.1f)

        val overallPassCount = sampleAttempts.count { it.percentage >= 60f } // 5
        val overallPassRate = (overallPassCount.toFloat() / sampleAttempts.size.toFloat()) * 100f
        assertEquals(83.33f, overallPassRate, 0.1f)
    }

    @Test
    fun testChartDisplayModesAndEnginesEnum() {
        val modes = ChartDisplayMode.values()
        assertTrue(modes.contains(ChartDisplayMode.STUDENTS))
        assertTrue(modes.contains(ChartDisplayMode.QUIZZES))
        assertTrue(modes.contains(ChartDisplayMode.DISTRIBUTION))

        val engines = ChartEngineType.values()
        assertTrue(engines.contains(ChartEngineType.RECHARTS))
        assertTrue(engines.contains(ChartEngineType.COMPOSE))
    }
}
