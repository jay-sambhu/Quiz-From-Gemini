package com.example

import com.example.data.local.entities.QuizAttemptEntity
import com.example.data.local.entities.QuizSetEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for filtering Past History quiz attempts by difficulty (Easy, Medium, Hard)
 * and verifying accurate count breakdowns across complexity tiers.
 */
class AttemptHistoryDifficultyFilterTest {

    private val sampleQuizSets = listOf(
        QuizSetEntity("q_easy", "Basic Arithmetic", "Basic math operations", "cat1", "Mathematics", "t1", "Prof. Alan", 10, 60, "Easy"),
        QuizSetEntity("q_med1", "World History Trivia", "History exploration", "cat2", "History", "t1", "Prof. Alan", 15, 60, "Medium"),
        QuizSetEntity("q_med2", "Cell Biology Basics", "Biology concepts", "cat3", "Science", "t1", "Prof. Alan", 12, 60, "Medium"),
        QuizSetEntity("q_hard", "Quantum Mechanics & Relativity", "Advanced physics", "cat3", "Science", "t1", "Prof. Alan", 25, 70, "Hard")
    )

    private val sampleAttempts = listOf(
        QuizAttemptEntity("att1", "q_easy", "Basic Arithmetic", "Mathematics", "s1", "Student One", "s1@edu.com", "", 10, 10, 100f, 60, 1000L),
        QuizAttemptEntity("att2", "q_med1", "World History Trivia", "History", "s1", "Student One", "s1@edu.com", "", 8, 10, 80f, 180, 2000L),
        QuizAttemptEntity("att3", "q_med2", "Cell Biology Basics", "Science", "s1", "Student One", "s1@edu.com", "", 7, 10, 70f, 150, 3000L),
        QuizAttemptEntity("att4", "q_hard", "Quantum Mechanics & Relativity", "Science", "s1", "Student One", "s1@edu.com", "", 6, 10, 60f, 300, 4000L)
    )

    @Test
    fun testDifficultyCountsAccurate() {
        val quizMap = sampleQuizSets.associateBy { it.id }

        val easyCount = sampleAttempts.count { (quizMap[it.quizSetId]?.difficulty ?: "Medium").equals("Easy", ignoreCase = true) }
        val mediumCount = sampleAttempts.count { (quizMap[it.quizSetId]?.difficulty ?: "Medium").equals("Medium", ignoreCase = true) }
        val hardCount = sampleAttempts.count { (quizMap[it.quizSetId]?.difficulty ?: "Medium").equals("Hard", ignoreCase = true) }

        assertEquals(1, easyCount)
        assertEquals(2, mediumCount)
        assertEquals(1, hardCount)
        assertEquals(4, sampleAttempts.size)
    }

    @Test
    fun testFilterByEasyDifficulty() {
        val quizMap = sampleQuizSets.associateBy { it.id }
        val filterDifficulty = "Easy"

        val filtered = sampleAttempts.filter {
            val diff = quizMap[it.quizSetId]?.difficulty ?: "Medium"
            diff.equals(filterDifficulty, ignoreCase = true)
        }

        assertEquals(1, filtered.size)
        assertEquals("att1", filtered.first().id)
        assertEquals("Basic Arithmetic", filtered.first().quizTitle)
    }

    @Test
    fun testFilterByMediumDifficulty() {
        val quizMap = sampleQuizSets.associateBy { it.id }
        val filterDifficulty = "Medium"

        val filtered = sampleAttempts.filter {
            val diff = quizMap[it.quizSetId]?.difficulty ?: "Medium"
            diff.equals(filterDifficulty, ignoreCase = true)
        }

        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.id == "att2" })
        assertTrue(filtered.any { it.id == "att3" })
    }

    @Test
    fun testFilterByHardDifficulty() {
        val quizMap = sampleQuizSets.associateBy { it.id }
        val filterDifficulty = "Hard"

        val filtered = sampleAttempts.filter {
            val diff = quizMap[it.quizSetId]?.difficulty ?: "Medium"
            diff.equals(filterDifficulty, ignoreCase = true)
        }

        assertEquals(1, filtered.size)
        assertEquals("att4", filtered.first().id)
        assertEquals("Quantum Mechanics & Relativity", filtered.first().quizTitle)
    }

    @Test
    fun testFilterByAllReturnsCompleteList() {
        val quizMap = sampleQuizSets.associateBy { it.id }
        val filterDifficulty = "ALL"

        val filtered = if (filterDifficulty != "ALL") {
            sampleAttempts.filter {
                val diff = quizMap[it.quizSetId]?.difficulty ?: "Medium"
                diff.equals(filterDifficulty, ignoreCase = true)
            }
        } else {
            sampleAttempts
        }

        assertEquals(4, filtered.size)
    }
}
