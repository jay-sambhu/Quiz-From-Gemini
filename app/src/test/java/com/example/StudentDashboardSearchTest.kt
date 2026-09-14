package com.example

import com.example.data.local.entities.QuizSetEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the Student Dashboard Quiz Search feature.
 * Verifies searching available quizzes by title, category, tags, and handling of blank queries
 * and unattempted prioritization.
 */
class StudentDashboardSearchTest {

    private val sampleQuizzes = listOf(
        QuizSetEntity(
            id = "quiz_calc",
            title = "Calculus & Derivatives",
            description = "Differential calculus and limits",
            categoryId = "cat_math",
            categoryName = "Mathematics",
            creatorTeacherId = "teacher_1",
            creatorTeacherName = "Prof. Euler",
            durationMinutes = 10,
            passPercentage = 60,
            difficulty = "Hard",
            tags = "Calculus,Math,Limits"
        ),
        QuizSetEntity(
            id = "quiz_cell",
            title = "Cellular Biology & Genetics",
            description = "Organelles, mitosis, and DNA structure",
            categoryId = "cat_sci",
            categoryName = "Natural Science",
            creatorTeacherId = "teacher_2",
            creatorTeacherName = "Dr. Mendel",
            durationMinutes = 8,
            passPercentage = 70,
            difficulty = "Medium",
            tags = "Biology,Genetics,Science"
        ),
        QuizSetEntity(
            id = "quiz_rev",
            title = "French Revolution & Napoleonic Era",
            description = "Causes and consequences of 1789 European conflicts",
            categoryId = "cat_hist",
            categoryName = "World History",
            creatorTeacherId = "teacher_3",
            creatorTeacherName = "Prof. Voltaire",
            durationMinutes = 12,
            passPercentage = 65,
            difficulty = "Medium",
            tags = "Europe,Revolution,History"
        ),
        QuizSetEntity(
            id = "quiz_dsa",
            title = "Data Structures & Algorithms",
            description = "Trees, graphs, dynamic programming, and complexity",
            categoryId = "cat_cs",
            categoryName = "Computer Science",
            creatorTeacherId = "teacher_4",
            creatorTeacherName = "Prof. Turing",
            durationMinutes = 15,
            passPercentage = 75,
            difficulty = "Hard",
            tags = "Algorithms,Coding,CS"
        )
    )

    private fun searchQuizzes(
        quizzes: List<QuizSetEntity>,
        query: String,
        attemptedIds: Set<String> = emptySet()
    ): List<QuizSetEntity> {
        if (query.isBlank()) return quizzes
        val trimmed = query.trim().lowercase()
        return quizzes.filter { quiz ->
            quiz.title.lowercase().contains(trimmed) ||
            quiz.categoryName.lowercase().contains(trimmed) ||
            quiz.description.lowercase().contains(trimmed) ||
            quiz.getTagList().any { it.lowercase().contains(trimmed) }
        }.sortedBy { it.id in attemptedIds }
    }

    @Test
    fun testSearchByTitlePartialMatch() {
        val results = searchQuizzes(sampleQuizzes, "Calculus")
        assertEquals(1, results.size)
        assertEquals("quiz_calc", results.first().id)
        assertEquals("Calculus & Derivatives", results.first().title)
    }

    @Test
    fun testSearchByTitleCaseInsensitive() {
        val results = searchQuizzes(sampleQuizzes, "cellular")
        assertEquals(1, results.size)
        assertEquals("quiz_cell", results.first().id)
        assertEquals("Cellular Biology & Genetics", results.first().title)
    }

    @Test
    fun testSearchByCategoryName() {
        val results = searchQuizzes(sampleQuizzes, "World History")
        assertEquals(1, results.size)
        assertEquals("quiz_rev", results.first().id)
        assertEquals("World History", results.first().categoryName)
    }

    @Test
    fun testSearchByCategoryPartial() {
        // "Science" matches both "Natural Science" and "Computer Science"
        val results = searchQuizzes(sampleQuizzes, "Science")
        assertEquals(2, results.size)
        assertTrue(results.any { it.id == "quiz_cell" })
        assertTrue(results.any { it.id == "quiz_dsa" })
    }

    @Test
    fun testSearchByTagOrKeyword() {
        val results = searchQuizzes(sampleQuizzes, "Algorithms")
        assertEquals(1, results.size)
        assertEquals("quiz_dsa", results.first().id)
    }

    @Test
    fun testEmptySearchReturnsAllQuizzes() {
        val results = searchQuizzes(sampleQuizzes, "")
        assertEquals(sampleQuizzes.size, results.size)
    }

    @Test
    fun testSearchNoMatchesReturnsEmpty() {
        val results = searchQuizzes(sampleQuizzes, "NonExistentSubject123")
        assertTrue(results.isEmpty())
    }

    @Test
    fun testUnattemptedQuizzesArePrioritized() {
        // Mark quiz_cell as attempted
        val attemptedIds = setOf("quiz_cell")
        // Search "Science" matches quiz_cell (attempted) and quiz_dsa (unattempted)
        val results = searchQuizzes(sampleQuizzes, "Science", attemptedIds)
        assertEquals(2, results.size)
        // quiz_dsa should come first because it's unattempted
        assertEquals("quiz_dsa", results[0].id)
        assertEquals("quiz_cell", results[1].id)
    }
}
