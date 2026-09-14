package com.example.data.repository

import android.util.Log
import com.example.data.firestore.FirestoreManager
import com.example.data.gemini.GeminiQuizQuestionService
import com.example.data.gemini.GeminiQuizService
import com.example.data.gemini.QuizQuestionGenerationReport
import com.example.data.local.dao.QuizDao
import com.example.data.local.entities.*
import com.example.data.model.QuizQuestion
import com.example.data.model.StudentLeaderboardEntry
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

class QuizRepository(
    private val quizDao: QuizDao,
    val firestoreManager: FirestoreManager? = null,
    val geminiQuizService: GeminiQuizService = GeminiQuizService.getInstance(),
    val geminiQuizQuestionService: GeminiQuizQuestionService? = null
) {

    val allUsers: Flow<List<UserEntity>> = quizDao.getAllUsers()
    val allCategories: Flow<List<CategoryEntity>> = quizDao.getAllCategories()
    val allQuizSets: Flow<List<QuizSetEntity>> = quizDao.getAllQuizSets()
    val allQuestions: Flow<List<QuestionEntity>> = quizDao.getAllQuestions()
    val allAttempts: Flow<List<QuizAttemptEntity>> = quizDao.getAllAttempts()
    val allNotificationLogs: Flow<List<NotificationLogEntity>> = quizDao.getAllNotificationLogs()

    suspend fun getUserById(userId: String): UserEntity? = quizDao.getUserById(userId)

    suspend fun saveOrSyncUser(user: UserEntity) {
        quizDao.insertUser(user)
        firestoreManager?.saveUserProfile(user)
    }

    suspend fun fetchUserRole(userId: String, email: String? = null): UserRole? {
        val remoteRole = firestoreManager?.fetchUserRoleFromFirestore(userId, email)
        if (remoteRole != null) {
            if (userId.isNotBlank()) {
                quizDao.updateUserRole(userId, remoteRole)
            }
            return remoteRole
        }
        return if (userId.isNotBlank()) quizDao.getUserById(userId)?.role else null
    }

    suspend fun storeUserRole(
        userId: String,
        email: String,
        name: String,
        role: UserRole
    ): Boolean {
        quizDao.updateUserRole(userId, role)
        return firestoreManager?.storeUserRoleInFirestore(userId, email, name, role) ?: false
    }

    suspend fun updateUserRole(userId: String, newRole: UserRole) {
        quizDao.updateUserRole(userId, newRole)
        firestoreManager?.updateUserRole(userId, newRole)
    }

    suspend fun updateUserPreferences(userId: String, preferredSubject: String, notificationsEnabled: Boolean) {
        quizDao.updateUserPreferences(userId, preferredSubject, notificationsEnabled)
        firestoreManager?.updateUserPreferences(userId, preferredSubject, notificationsEnabled)
    }

    suspend fun updateUserProfilePhotoUrl(userId: String, photoUrl: String): Boolean {
        quizDao.updateUserPhotoUrl(userId, photoUrl)
        return firestoreManager?.updateUserProfilePhotoUrl(userId, photoUrl) ?: false
    }

    // --- Categories ---
    suspend fun createCategory(name: String, description: String, iconName: String, colorHex: String) {
        val category = CategoryEntity(
            id = "cat_" + UUID.randomUUID().toString().take(8),
            name = name,
            description = description,
            iconName = iconName,
            colorHex = colorHex
        )
        quizDao.insertCategory(category)
        firestoreManager?.saveCategory(category)
    }

    suspend fun deleteCategory(categoryId: String) {
        quizDao.deleteCategory(categoryId)
        firestoreManager?.deleteCategory(categoryId)
    }

    // --- Quiz Sets & Questions ---
    fun getQuizSetsByCategory(categoryId: String): Flow<List<QuizSetEntity>> = quizDao.getQuizSetsByCategory(categoryId)

    suspend fun getQuizSetById(quizSetId: String): QuizSetEntity? = quizDao.getQuizSetById(quizSetId)

    suspend fun getQuestionsForQuizSet(quizSetId: String): List<QuestionEntity> = quizDao.getQuestionsForQuizSet(quizSetId)

    fun getQuestionsForQuizSetFlow(quizSetId: String): Flow<List<QuestionEntity>> = quizDao.getQuestionsForQuizSetFlow(quizSetId)

    suspend fun getQuestionById(questionId: String): QuestionEntity? = quizDao.getQuestionById(questionId)

    /**
     * Saves a QuizQuestion into Firestore 'questions' collection and synchronizes Room cache.
     */
    suspend fun saveQuizQuestion(question: QuizQuestion): Boolean {
        if (question.options.isNotEmpty()) {
            quizDao.insertQuestion(question.toQuestionEntity())
        }
        return firestoreManager?.saveQuizQuestionToFirestore(question) ?: false
    }

    /**
     * Convenience function to construct and save a QuizQuestion directly to Firestore.
     */
    suspend fun saveQuizQuestion(
        questionText: String,
        options: List<String>,
        correctAnswer: String,
        category: String = "",
        tag: String = "",
        quizSetId: String = "",
        explanation: String = ""
    ): QuizQuestion {
        val question = QuizQuestion(
            questionText = questionText,
            options = options,
            correctAnswer = correctAnswer,
            category = category,
            tag = tag,
            quizSetId = quizSetId,
            explanation = explanation
        )
        saveQuizQuestion(question)
        return question
    }

    /**
     * Saves a list of QuizQuestion items to Firestore.
     */
    suspend fun saveQuizQuestions(questions: List<QuizQuestion>): Boolean {
        val entities = questions.filter { it.options.isNotEmpty() }.map { it.toQuestionEntity() }
        if (entities.isNotEmpty()) {
            quizDao.insertQuestions(entities)
        }
        return firestoreManager?.saveQuizQuestionsToFirestore(questions) ?: false
    }

    /**
     * Fetches QuizQuestions from Firestore, optionally filtered by category or tag.
     */
    suspend fun fetchQuizQuestionsFromFirestore(category: String? = null, tag: String? = null): List<QuizQuestion> {
        return firestoreManager?.fetchQuizQuestionsFromFirestore(category, tag) ?: emptyList()
    }

    /**
     * Automatically generates QuizQuestion objects using GeminiQuizQuestionService with multi-key rotation
     * and model failover support.
     */
    suspend fun generateQuizQuestions(
        topic: String,
        difficultyLevel: String = "Medium",
        count: Int = 5,
        category: String = "",
        tag: String = "",
        quizSetId: String = ""
    ): List<QuizQuestion> {
        return geminiQuizQuestionService?.generateQuizQuestions(
            topic = topic,
            difficultyLevel = difficultyLevel,
            count = count,
            category = category,
            tag = tag,
            quizSetId = quizSetId
        ) ?: emptyList()
    }

    /**
     * Generates QuizQuestion objects and returns an in-depth telemetry report with failover logs.
     */
    suspend fun generateQuizQuestionsWithReport(
        topic: String,
        difficultyLevel: String = "Medium",
        count: Int = 5,
        category: String = "",
        tag: String = "",
        quizSetId: String = ""
    ): QuizQuestionGenerationReport? {
        return geminiQuizQuestionService?.generateQuizQuestionsWithReport(
            topic = topic,
            difficultyLevel = difficultyLevel,
            count = count,
            category = category,
            tag = tag,
            quizSetId = quizSetId
        )
    }

    suspend fun saveOrUpdateQuestion(question: QuestionEntity) {
        quizDao.insertQuestion(question)
        firestoreManager?.saveOrUpdateQuestion(question)
    }

    suspend fun insertQuestions(questions: List<QuestionEntity>) {
        if (questions.isEmpty()) return
        quizDao.insertQuestions(questions)
        questions.forEach { q ->
            firestoreManager?.saveOrUpdateQuestion(q)
        }
    }

    suspend fun deleteQuestion(questionId: String) {
        quizDao.deleteQuestionById(questionId)
        firestoreManager?.deleteQuestion(questionId)
    }

    suspend fun createQuizSetWithQuestions(
        title: String,
        description: String,
        categoryId: String,
        categoryName: String,
        teacherId: String,
        teacherName: String,
        durationMinutes: Int,
        passPercentage: Int,
        difficulty: String,
        questions: List<QuestionEntity>,
        sendEmailNotifications: Boolean = true,
        tags: String = ""
    ): QuizSetEntity {
        val quizId = "quiz_" + UUID.randomUUID().toString().take(8)
        val computedTags = if (tags.isNotBlank()) tags else {
            generateDefaultTags(categoryName, title)
        }
        val quizSet = QuizSetEntity(
            id = quizId,
            title = title,
            description = description,
            categoryId = categoryId,
            categoryName = categoryName,
            creatorTeacherId = teacherId,
            creatorTeacherName = teacherName,
            durationMinutes = durationMinutes,
            passPercentage = passPercentage,
            difficulty = difficulty,
            tags = computedTags
        )
        quizDao.insertQuizSet(quizSet)

        val questionsWithQuizId = questions.map { q ->
            q.copy(quizSetId = quizId)
        }
        quizDao.insertQuestions(questionsWithQuizId)

        // Sync to Cloud Firestore
        firestoreManager?.saveQuizSetWithQuestions(quizSet, questionsWithQuizId)

        if (sendEmailNotifications) {
            // Simulate sending assignment emails to students & log to cloud
            val notif = NotificationLogEntity(
                id = "notif_" + UUID.randomUUID().toString().take(8),
                recipientEmail = "students@quizplatform.edu",
                recipientName = "Enrolled Students",
                subject = "New Quiz Assignment: $title",
                body = "Hello Students,\n\nProf. $teacherName has assigned a new quiz: '$title' ($categoryName).\nDuration: $durationMinutes mins | Pass Mark: $passPercentage%.\nLog in now to test your knowledge!",
                quizSetId = quizId
            )
            quizDao.insertNotificationLog(notif)
            firestoreManager?.saveNotification(notif)
        }

        return quizSet
    }

    suspend fun generateAndSaveGeminiQuiz(
        topic: String,
        title: String,
        description: String,
        categoryId: String,
        categoryName: String,
        teacherId: String,
        teacherName: String,
        questionCount: Int = 5,
        durationMinutes: Int = 10,
        difficulty: String = "Medium",
        tags: String = ""
    ): QuizSetEntity {
        val quizId = "quiz_ai_" + UUID.randomUUID().toString().take(8)
        val finalTitle = if (title.isBlank()) "AI Quiz: $topic" else title
        val computedTags = if (tags.isNotBlank()) tags else {
            generateDefaultTags(categoryName, "$topic $finalTitle")
        }
        val quizSet = QuizSetEntity(
            id = quizId,
            title = finalTitle,
            description = if (description.isBlank()) "Generated by Gemini AI for topic '$topic'" else description,
            categoryId = categoryId,
            categoryName = categoryName,
            creatorTeacherId = teacherId,
            creatorTeacherName = teacherName,
            durationMinutes = durationMinutes,
            difficulty = difficulty,
            tags = computedTags
        )
        quizDao.insertQuizSet(quizSet)

        val questions = geminiQuizService.generateQuizQuestions(
            topic = topic,
            difficultyLevel = difficulty,
            count = questionCount,
            quizSetId = quizId
        )
        quizDao.insertQuestions(questions)

        // Sync to Cloud Firestore
        firestoreManager?.saveQuizSetWithQuestions(quizSet, questions)

        // Log Email notification & sync to Firestore
        val notif = NotificationLogEntity(
            id = "notif_" + UUID.randomUUID().toString().take(8),
            recipientEmail = "students@quizplatform.edu",
            recipientName = "All Enrolled Students",
            subject = "New AI Quiz Assignment: ${quizSet.title}",
            body = "Hello Students,\n\nA new AI-generated quiz set on '$topic' has been assigned by $teacherName.\nLog in to take the test and earn points on the leaderboard!",
            quizSetId = quizId
        )
        quizDao.insertNotificationLog(notif)
        firestoreManager?.saveNotification(notif)

        return quizSet
    }

    suspend fun deleteQuizSet(quizSetId: String) {
        quizDao.deleteQuestionsForQuizSet(quizSetId)
        quizDao.deleteQuizSet(quizSetId)
        firestoreManager?.deleteQuizSet(quizSetId)
    }

    // --- Attempts & Submissions ---
    fun getAttemptsForStudent(studentId: String): Flow<List<QuizAttemptEntity>> = quizDao.getAttemptsForStudent(studentId)

    suspend fun submitQuizAttempt(
        quizSet: QuizSetEntity,
        student: UserEntity,
        score: Int,
        totalQuestions: Int,
        timeSpentSeconds: Int,
        userAnswersJson: String
    ): QuizAttemptEntity {
        val percentage = if (totalQuestions > 0) (score.toFloat() / totalQuestions.toFloat()) * 100f else 0f
        val attempt = QuizAttemptEntity(
            id = "att_" + UUID.randomUUID().toString().take(8),
            quizSetId = quizSet.id,
            quizTitle = quizSet.title,
            categoryName = quizSet.categoryName,
            studentId = student.id,
            studentName = student.name,
            studentEmail = student.email,
            studentPhotoUrl = student.photoUrl,
            score = score,
            totalQuestions = totalQuestions,
            percentage = percentage,
            timeSpentSeconds = timeSpentSeconds,
            userAnswersJson = userAnswersJson
        )
        quizDao.insertAttempt(attempt)
        // Store Session Data & attempt in Firestore (with timeout to prevent freezing on slow/offline networks)
        try {
            withTimeoutOrNull(3000L) {
                firestoreManager?.saveQuizAttempt(attempt)
            }
        } catch (e: Exception) {
            Log.w("QuizRepository", "Firestore attempt save failed or timed out: ${e.message}")
        }
        return attempt
    }

    /**
     * Retrieves completed quizzes and scores for a student from Firestore, ordered chronologically.
     * Falls back to local database if network/Firestore is unreachable.
     */
    suspend fun fetchStudentAttemptsFromFirestore(studentId: String): List<QuizAttemptEntity> {
        val firestoreAttempts = try {
            firestoreManager?.fetchStudentAttemptsFromFirestore(studentId)
        } catch (e: Exception) {
            Log.w("QuizRepository", "Failed to fetch student attempts from Firestore: ${e.message}")
            null
        }

        return if (!firestoreAttempts.isNullOrEmpty()) {
            firestoreAttempts
        } else {
            quizDao.getAttemptsForStudent(studentId).firstOrNull() ?: emptyList()
        }
    }

    suspend fun syncAllDataToFirestore(
        users: List<UserEntity>,
        categories: List<CategoryEntity>,
        quizSets: List<QuizSetEntity>,
        attempts: List<QuizAttemptEntity>,
        notifications: List<NotificationLogEntity>
    ) {
        val allQuestions = quizSets.flatMap { quizDao.getQuestionsForQuizSet(it.id) }
        firestoreManager?.syncLocalDataToFirestore(
            users = users,
            categories = categories,
            quizSets = quizSets,
            questions = allQuestions,
            attempts = attempts,
            notifications = notifications
        )
    }

    private fun generateDefaultTags(categoryName: String, context: String): String {
        val tags = mutableSetOf<String>()
        val lower = (categoryName + " " + context).lowercase()

        when {
            lower.contains("math") || lower.contains("calcul") || lower.contains("algebra") || lower.contains("matrix") || lower.contains("probab") -> {
                tags.addAll(listOf("Math", "Calculus", "LinearAlgebra", "STEM", "ProblemSolving"))
            }
            lower.contains("sci") || lower.contains("phys") || lower.contains("chem") || lower.contains("bio") || lower.contains("quantum") -> {
                tags.addAll(listOf("Science", "Physics", "Chemistry", "STEM", "Lab"))
            }
            lower.contains("hist") || lower.contains("civil") || lower.contains("war") || lower.contains("ancient") || lower.contains("renaiss") -> {
                tags.addAll(listOf("History", "WorldHistory", "Civilizations", "Humanities"))
            }
            lower.contains("comp") || lower.contains("algo") || lower.contains("data") || lower.contains("code") || lower.contains("software") -> {
                tags.addAll(listOf("ComputerScience", "Algorithms", "DataStructures", "Coding", "STEM"))
            }
            else -> {
                tags.add(categoryName.replace(" ", ""))
                tags.add("GeneralKnowledge")
                tags.add("StudyGuide")
            }
        }
        return tags.joinToString(", ")
    }

    /**
     * Fetches top-performing students directly from Cloud Firestore,
     * ranked by total points earned across all quizzes. Falls back to local Room database.
     */
    suspend fun fetchTopStudentsLeaderboardFromFirestore(limit: Int = 100): List<StudentLeaderboardEntry> {
        try {
            val remoteEntries = firestoreManager?.fetchTopStudentsLeaderboardFromFirestore(limit)
            if (!remoteEntries.isNullOrEmpty()) {
                return remoteEntries
            }
        } catch (e: Exception) {
            Log.w("QuizRepository", "Failed fetching Firestore leaderboard: ${e.message}")
        }

        // Offline / local cache aggregation fallback
        val users = quizDao.getAllUsers().firstOrNull() ?: emptyList()
        val attempts = quizDao.getAllAttempts().firstOrNull() ?: emptyList()
        val students = users.filter { it.role == UserRole.STUDENT }

        val entries = students.map { student ->
            val studentAttempts = attempts.filter { it.studentId == student.id }
            val totalScore = studentAttempts.sumOf { it.score }
            val totalTaken = studentAttempts.size
            val avgPct = if (totalTaken > 0) studentAttempts.map { it.percentage }.average().toFloat() else 0f
            val highestScore = studentAttempts.maxOfOrNull { it.score } ?: 0
            val lastActive = studentAttempts.maxOfOrNull { it.completedAt } ?: 0L

            StudentLeaderboardEntry(
                studentId = student.id,
                studentName = student.name,
                studentEmail = student.email,
                photoUrl = student.photoUrl,
                totalScore = totalScore,
                totalQuizzesTaken = totalTaken,
                averagePercentage = avgPct,
                highestScore = highestScore,
                lastActiveTimestamp = lastActive
            )
        }.sortedWith(
            compareByDescending<StudentLeaderboardEntry> { it.totalScore }
                .thenByDescending { it.averagePercentage }
                .thenByDescending { it.totalQuizzesTaken }
        ).take(limit)

        val totalCount = entries.size
        return entries.mapIndexed { index, item ->
            val rank = index + 1
            val percentile = if (totalCount > 1) {
                ((totalCount - rank).toFloat() / (totalCount - 1).toFloat()) * 100f
            } else 100f
            item.copy(rank = rank, percentile = percentile)
        }
    }

    suspend fun purgeAllSeededData() {
        try {
            quizDao.purgeSeededUsers()
            quizDao.purgeSeededQuizSets()
            quizDao.purgeSeededQuestions()
            quizDao.purgeSeededAttempts()
            quizDao.purgeSeededNotificationLogs()
            quizDao.purgeSeededCategories()
        } catch (e: Exception) {
            Log.w("QuizRepository", "Purge seeded data note: ${e.message}")
        }
    }
}
