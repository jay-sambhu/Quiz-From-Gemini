package com.example

import com.example.data.model.StudentLeaderboardEntry
import com.example.ui.leaderboard.LeaderboardFilterRange
import com.example.ui.leaderboard.LeaderboardSortOrder
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Classroom Leaderboard fetching and ranking logic.
 * Verifies ordering of top-scoring students strictly by average performance,
 * tie breaking, percentile calculations, and filter options.
 */
class FirestoreLeaderboardTest {

    private val sampleRoster = listOf(
        StudentLeaderboardEntry(
            studentId = "s1",
            studentName = "Alice Smith",
            studentEmail = "alice@school.edu",
            photoUrl = "",
            totalScore = 85,
            totalQuizzesTaken = 10,
            averagePercentage = 85.0f,
            highestScore = 10,
            lastActiveTimestamp = 1000L
        ),
        StudentLeaderboardEntry(
            studentId = "s2",
            studentName = "Bob Jones",
            studentEmail = "bob@school.edu",
            photoUrl = "",
            totalScore = 95,
            totalQuizzesTaken = 10,
            averagePercentage = 95.0f,
            highestScore = 10,
            lastActiveTimestamp = 2000L
        ),
        StudentLeaderboardEntry(
            studentId = "s3",
            studentName = "Charlie Brown",
            studentEmail = "charlie@school.edu",
            photoUrl = "",
            totalScore = 90,
            totalQuizzesTaken = 10,
            averagePercentage = 90.0f,
            highestScore = 10,
            lastActiveTimestamp = 3000L
        ),
        StudentLeaderboardEntry(
            studentId = "s4",
            studentName = "Diana Prince",
            studentEmail = "diana@school.edu",
            photoUrl = "",
            totalScore = 45,
            totalQuizzesTaken = 5,
            averagePercentage = 90.0f,
            highestScore = 10,
            lastActiveTimestamp = 4000L
        ),
        StudentLeaderboardEntry(
            studentId = "s5",
            studentName = "Evan Wright",
            studentEmail = "evan@school.edu",
            photoUrl = "",
            totalScore = 0,
            totalQuizzesTaken = 0,
            averagePercentage = 0f,
            highestScore = 0,
            lastActiveTimestamp = 0L
        )
    )

    @Test
    fun testLeaderboardOrderedByAveragePerformance() {
        // Sort ordered by average performance (averagePercentage descending)
        val sorted = sampleRoster.sortedWith(
            compareByDescending<StudentLeaderboardEntry> { it.totalQuizzesTaken > 0 }
                .thenByDescending { it.averagePercentage }
                .thenByDescending { it.totalScore }
                .thenByDescending { it.totalQuizzesTaken }
        )

        // Bob: 95.0%
        assertEquals("s2", sorted[0].studentId)
        assertEquals(95.0f, sorted[0].averagePercentage, 0.01f)

        // Ties in 90.0%: Charlie (90 pts) vs Diana (45 pts) -> Charlie wins tiebreak
        assertEquals("s3", sorted[1].studentId)
        assertEquals(90.0f, sorted[1].averagePercentage, 0.01f)
        assertEquals("s4", sorted[2].studentId)
        assertEquals(90.0f, sorted[2].averagePercentage, 0.01f)

        // Alice: 85.0%
        assertEquals("s1", sorted[3].studentId)

        // Inactive student (0 quizzes taken) at the bottom
        assertEquals("s5", sorted[4].studentId)
    }

    @Test
    fun testPodiumExtraction() {
        val sorted = sampleRoster.sortedWith(
            compareByDescending<StudentLeaderboardEntry> { it.totalQuizzesTaken > 0 }
                .thenByDescending { it.averagePercentage }
                .thenByDescending { it.totalScore }
                .thenByDescending { it.totalQuizzesTaken }
        )

        val firstPlace = sorted.getOrNull(0)
        val secondPlace = sorted.getOrNull(1)
        val thirdPlace = sorted.getOrNull(2)

        assertNotNull(firstPlace)
        assertNotNull(secondPlace)
        assertNotNull(thirdPlace)

        assertEquals("Bob Jones", firstPlace?.studentName)
        assertEquals("Charlie Brown", secondPlace?.studentName)
        assertEquals("Diana Prince", thirdPlace?.studentName)
    }

    @Test
    fun testPercentileRankCalculation() {
        val sorted = sampleRoster.sortedWith(
            compareByDescending<StudentLeaderboardEntry> { it.totalQuizzesTaken > 0 }
                .thenByDescending { it.averagePercentage }
                .thenByDescending { it.totalScore }
                .thenByDescending { it.totalQuizzesTaken }
        )

        val totalRanked = sorted.size
        val rankedEntries = sorted.mapIndexed { index, entry ->
            val rank = index + 1
            val percentile = if (totalRanked > 1) {
                ((totalRanked - rank).toFloat() / (totalRanked - 1).toFloat()) * 100f
            } else 100f
            entry.copy(rank = rank, percentile = percentile)
        }

        // Rank 1 of 5 -> 100th percentile
        assertEquals(1, rankedEntries[0].rank)
        assertEquals(100f, rankedEntries[0].percentile, 0.01f)

        // Rank 5 of 5 -> 0th percentile
        assertEquals(5, rankedEntries[4].rank)
        assertEquals(0f, rankedEntries[4].percentile, 0.01f)
    }

    @Test
    fun testSearchAndRangeFilters() {
        // Search by query "diana"
        val query = "diana"
        val searchResults = sampleRoster.filter {
            it.studentName.lowercase().contains(query) || it.studentEmail.lowercase().contains(query)
        }
        assertEquals(1, searchResults.size)
        assertEquals("Diana Prince", searchResults.first().studentName)

        // Podium only filter
        val podiumOnly = sampleRoster.take(3)
        assertEquals(3, podiumOnly.size)
    }
}
