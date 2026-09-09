package com.example.data.model

/**
 * Data structures representing student performance metrics, quiz analytics,
 * and grade distributions for the Teacher Dashboard charts and Recharts visualizer.
 */
data class StudentLeaderboardEntry(
    val studentId: String,
    val studentName: String,
    val studentEmail: String,
    val photoUrl: String = "",
    val totalScore: Int = 0,
    val totalQuizzesTaken: Int = 0,
    val averagePercentage: Float = 0f,
    val rank: Int = 0,
    val percentile: Float = 0f,
    val highestScore: Int = 0,
    val lastActiveTimestamp: Long = 0L
)

data class StudentPerformanceSummary(
    val studentId: String,
    val studentName: String,
    val studentEmail: String,
    val quizzesTaken: Int,
    val averageScore: Float,
    val highestScore: Float,
    val passRate: Float,
    val totalScore: Int,
    val rank: Int = 1,
    val gradeTier: String = "A",
    val statusColor: String = "#10B981"
)

data class QuizPerformanceSummary(
    val quizSetId: String,
    val quizTitle: String,
    val categoryName: String,
    val totalAttempts: Int,
    val averageScore: Float,
    val passRate: Float
)

data class GradeDistributionItem(
    val grade: String,
    val label: String,
    val count: Int,
    val percentageOfTotal: Float,
    val hexColor: String
)

data class TeacherAnalyticsOverview(
    val totalStudentsEvaluated: Int = 0,
    val totalAttemptsEvaluated: Int = 0,
    val classAverageScore: Float = 0f,
    val overallPassRate: Float = 0f,
    val topPerformingQuiz: String = "None",
    val studentSummaries: List<StudentPerformanceSummary> = emptyList(),
    val quizSummaries: List<QuizPerformanceSummary> = emptyList(),
    val gradeDistributions: List<GradeDistributionItem> = emptyList()
)

enum class ChartDisplayMode(val label: String) {
    STUDENTS("Students"),
    QUIZZES("Quizzes"),
    DISTRIBUTION("Grades")
}

enum class ChartEngineType(val label: String) {
    RECHARTS("Recharts (Web)"),
    COMPOSE("Native Compose")
}

