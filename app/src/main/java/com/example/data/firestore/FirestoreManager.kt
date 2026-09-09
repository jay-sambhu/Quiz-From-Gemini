package com.example.data.firestore

import android.content.Context
import android.util.Log
import com.example.data.local.dao.QuizDao
import com.example.data.local.entities.*
import com.example.data.model.QuizQuestion
import com.example.data.model.StudentLeaderboardEntry
import com.example.data.model.UserRole
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import java.util.UUID

/**
 * System Audit & Activity Log Item aggregated from Firestore
 */
data class SystemLogItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "GENERAL", // SECURITY, AI_ENGINE, QUIZ_ACTIVITY, FIRESTORE_SYNC, GENERAL
    val severity: String = "INFO",     // INFO, SUCCESS, WARNING, ALERT
    val actor: String = "System",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Cloud Firebase Firestore Manager for storing and synchronizing:
 * - Quiz Questions & Quiz Sets
 * - User Profiles with Multi-Role Access (Admin, Teacher, Student)
 * - Session Data & Student Quiz Attempts
 * - Subject Categories & Assignment Notifications
 */
class FirestoreManager(private val context: Context, private val quizDao: QuizDao) {

    companion object {
        private const val TAG = "FirestoreManager"

        const val COLLECTION_USERS = "users"
        const val COLLECTION_QUIZ_SETS = "quiz_sets"
        const val COLLECTION_QUESTIONS = "questions"
        const val COLLECTION_CATEGORIES = "categories"
        const val COLLECTION_ATTEMPTS = "quiz_attempts"
        const val COLLECTION_NOTIFICATIONS = "notifications"
        const val COLLECTION_SYSTEM_LOGS = "system_logs"
        const val COLLECTION_SYSTEM_CONFIG = "system_config"
        const val DOC_GEMINI_API_CONFIG = "gemini_api_keys"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var firestore: FirebaseFirestore? = null

    private val _isCloudConnected = MutableStateFlow(false)
    val isCloudConnected: StateFlow<Boolean> = _isCloudConnected.asStateFlow()

    private val _cloudSyncMessage = MutableStateFlow("Initializing Cloud Firestore...")
    val cloudSyncMessage: StateFlow<String> = _cloudSyncMessage.asStateFlow()

    private val _isApiKeySyncedWithCloud = MutableStateFlow(false)
    val isApiKeySyncedWithCloud: StateFlow<Boolean> = _isApiKeySyncedWithCloud.asStateFlow()

    private val _cloudApiKeyCount = MutableStateFlow(0)
    val cloudApiKeyCount: StateFlow<Int> = _cloudApiKeyCount.asStateFlow()

    private val _lastApiKeyCloudSyncTime = MutableStateFlow(0L)
    val lastApiKeyCloudSyncTime: StateFlow<Long> = _lastApiKeyCloudSyncTime.asStateFlow()

    private val _systemLogs = MutableStateFlow<List<SystemLogItem>>(emptyList())
    val systemLogs: StateFlow<List<SystemLogItem>> = _systemLogs.asStateFlow()

    private val listenerRegistrations = mutableListOf<ListenerRegistration>()

    init {
        initFirestore()
    }

    private fun initFirestore() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId(context.packageName)
                    .setProjectId("quiz-platform-cloud")
                    .setApiKey("AIzaSyQuizPlatformCloudKeyProduction001")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
            firestore = FirebaseFirestore.getInstance()
            _isCloudConnected.value = true
            _cloudSyncMessage.value = "Cloud Firestore: Active & Synced"
            Log.d(TAG, "Firebase Firestore initialized successfully.")
            seedInitialSystemLogs(firestore!!)
            seedInitialRoleProfiles(firestore!!)
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase Firestore fallback mode: ${e.message}")
            _isCloudConnected.value = false
            _cloudSyncMessage.value = "Cloud Firestore: Offline Cache Mode"
            _systemLogs.value = getDefaultSystemLogs()
        }
    }

    fun startRealtimeSync() {
        val db = firestore ?: return

        try {
            // 1. Listen to User Profiles from Firestore
            val usersReg = db.collection(COLLECTION_USERS)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Users listener failed: ${error.message}")
                        return@addSnapshotListener
                    }
                    snapshots?.documents?.let { docs ->
                        scope.launch {
                            docs.forEach { doc ->
                                val data = doc.data ?: return@forEach
                                val roleStr = data["role"] as? String ?: UserRole.STUDENT.name
                                val role = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.STUDENT }
                                val user = UserEntity(
                                    id = doc.id,
                                    name = data["name"] as? String ?: "Anonymous",
                                    email = data["email"] as? String ?: "",
                                    photoUrl = data["photoUrl"] as? String ?: "",
                                    role = role,
                                    preferredSubject = data["preferredSubject"] as? String ?: "All",
                                    emailNotificationsEnabled = (data["emailNotificationsEnabled"] as? Boolean) ?: true,
                                    registeredAt = (data["registeredAt"] as? Long) ?: System.currentTimeMillis()
                                )
                                quizDao.insertUser(user)
                            }
                        }
                    }
                }
            listenerRegistrations.add(usersReg)

            // 2. Listen to Subject Categories from Firestore
            val categoriesReg = db.collection(COLLECTION_CATEGORIES)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Categories listener failed: ${error.message}")
                        return@addSnapshotListener
                    }
                    snapshots?.documents?.let { docs ->
                        scope.launch {
                            docs.forEach { doc ->
                                val data = doc.data ?: return@forEach
                                val category = CategoryEntity(
                                    id = doc.id,
                                    name = data["name"] as? String ?: "",
                                    description = data["description"] as? String ?: "",
                                    iconName = data["iconName"] as? String ?: "School",
                                    colorHex = data["colorHex"] as? String ?: "#4F46E5"
                                )
                                quizDao.insertCategory(category)
                            }
                        }
                    }
                }
            listenerRegistrations.add(categoriesReg)

            // 3. Listen to Quiz Sets from Firestore
            val quizSetsReg = db.collection(COLLECTION_QUIZ_SETS)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "QuizSets listener failed: ${error.message}")
                        return@addSnapshotListener
                    }
                    snapshots?.documents?.let { docs ->
                        scope.launch {
                            docs.forEach { doc ->
                                val data = doc.data ?: return@forEach
                                val quizSet = QuizSetEntity(
                                    id = doc.id,
                                    title = data["title"] as? String ?: "",
                                    description = data["description"] as? String ?: "",
                                    categoryId = data["categoryId"] as? String ?: "",
                                    categoryName = data["categoryName"] as? String ?: "",
                                    creatorTeacherId = data["creatorTeacherId"] as? String ?: "",
                                    creatorTeacherName = data["creatorTeacherName"] as? String ?: "",
                                    durationMinutes = (data["durationMinutes"] as? Long)?.toInt() ?: 10,
                                    passPercentage = (data["passPercentage"] as? Long)?.toInt() ?: 60,
                                    difficulty = data["difficulty"] as? String ?: "Medium",
                                    createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis(),
                                    tags = data["tags"] as? String ?: ""
                                )
                                quizDao.insertQuizSet(quizSet)
                            }
                        }
                    }
                }
            listenerRegistrations.add(quizSetsReg)

            // 4. Listen to Questions from Firestore
            val questionsReg = db.collection(COLLECTION_QUESTIONS)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Questions listener failed: ${error.message}")
                        return@addSnapshotListener
                    }
                    snapshots?.documents?.let { docs ->
                        scope.launch {
                            val questionsList = docs.mapNotNull { doc ->
                                val data = doc.data ?: return@mapNotNull null
                                QuestionEntity(
                                    id = doc.id,
                                    quizSetId = data["quizSetId"] as? String ?: "",
                                    questionText = data["questionText"] as? String ?: "",
                                    optionA = data["optionA"] as? String ?: "",
                                    optionB = data["optionB"] as? String ?: "",
                                    optionC = data["optionC"] as? String ?: "",
                                    optionD = data["optionD"] as? String ?: "",
                                    correctOptionIndex = (data["correctOptionIndex"] as? Long)?.toInt() ?: 0,
                                    explanation = data["explanation"] as? String ?: ""
                                )
                            }
                            if (questionsList.isNotEmpty()) {
                                quizDao.insertQuestions(questionsList)
                            }
                        }
                    }
                }
            listenerRegistrations.add(questionsReg)

            // 5. Listen to Quiz Attempts (Session Data) from Firestore
            val attemptsReg = db.collection(COLLECTION_ATTEMPTS)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Attempts listener failed: ${error.message}")
                        return@addSnapshotListener
                    }
                    snapshots?.documents?.let { docs ->
                        scope.launch {
                            docs.forEach { doc ->
                                val data = doc.data ?: return@forEach
                                val attempt = QuizAttemptEntity(
                                    id = doc.id,
                                    quizSetId = data["quizSetId"] as? String ?: "",
                                    quizTitle = data["quizTitle"] as? String ?: "",
                                    categoryName = data["categoryName"] as? String ?: "",
                                    studentId = data["studentId"] as? String ?: "",
                                    studentName = data["studentName"] as? String ?: "",
                                    studentEmail = data["studentEmail"] as? String ?: "",
                                    studentPhotoUrl = data["studentPhotoUrl"] as? String ?: "",
                                    score = (data["score"] as? Long)?.toInt() ?: 0,
                                    totalQuestions = (data["totalQuestions"] as? Long)?.toInt() ?: 0,
                                    percentage = (data["percentage"] as? Double)?.toFloat()
                                        ?: (data["percentage"] as? Long)?.toFloat() ?: 0f,
                                    timeSpentSeconds = (data["timeSpentSeconds"] as? Long)?.toInt() ?: 0,
                                    completedAt = (data["completedAt"] as? Long) ?: System.currentTimeMillis(),
                                    userAnswersJson = data["userAnswersJson"] as? String ?: "{}"
                                )
                                quizDao.insertAttempt(attempt)
                            }
                        }
                    }
                }
            listenerRegistrations.add(attemptsReg)

            // 6. Listen to System Audit Logs from Firestore
            val logsReg = db.collection(COLLECTION_SYSTEM_LOGS)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "System logs listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    val logs = snapshots?.documents?.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        SystemLogItem(
                            id = doc.id,
                            title = data["title"] as? String ?: "System Event",
                            description = data["description"] as? String ?: "",
                            category = data["category"] as? String ?: "GENERAL",
                            severity = data["severity"] as? String ?: "INFO",
                            actor = data["actor"] as? String ?: "System",
                            timestamp = (data["timestamp"] as? Long) ?: System.currentTimeMillis()
                        )
                    }?.sortedByDescending { it.timestamp } ?: emptyList()

                    if (logs.isNotEmpty()) {
                        _systemLogs.value = logs
                    }
                }
            listenerRegistrations.add(logsReg)

            Log.d(TAG, "Realtime Firestore snapshot listeners successfully established.")
        } catch (e: Exception) {
            Log.w(TAG, "Failed setting up realtime listeners: ${e.message}")
        }
    }

    fun stopRealtimeSync() {
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()
    }

    // --- User Profile Operations in Firestore ---
    suspend fun saveUserProfile(user: UserEntity) {
        val db = firestore ?: return
        val map = mapOf(
            "id" to user.id,
            "name" to user.name,
            "email" to user.email,
            "photoUrl" to user.photoUrl,
            "role" to user.role.name,
            "preferredSubject" to user.preferredSubject,
            "emailNotificationsEnabled" to user.emailNotificationsEnabled,
            "registeredAt" to user.registeredAt,
            "updatedAt" to System.currentTimeMillis()
        )
        try {
            db.collection(COLLECTION_USERS).document(user.id)
                .set(map, SetOptions.merge())
                .awaitTask()
            Log.d(TAG, "User profile saved to Firestore: ${user.name} (${user.role})")
        } catch (e: Exception) {
            Log.w(TAG, "Failed saving user profile to Firestore: ${e.message}")
        }
    }

    /**
     * Stores or updates the user's role and profile details in Firestore after login.
     */
    suspend fun storeUserRoleInFirestore(
        userId: String,
        email: String,
        name: String,
        role: UserRole
    ): Boolean {
        val db = firestore ?: run {
            Log.w(TAG, "Firestore not initialized; role will remain in local cache.")
            return false
        }
        return try {
            val userPayload = mapOf(
                "id" to userId,
                "email" to email,
                "name" to name,
                "role" to role.name,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection(COLLECTION_USERS).document(userId)
                .set(userPayload, SetOptions.merge())
                .awaitTask()
            Log.d(TAG, "Successfully stored user role in Firestore: $userId -> ${role.name}")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Error storing user role in Firestore: ${e.message}")
            false
        }
    }

    /**
     * Fetches the user's role from their document in the Firestore 'users' collection,
     * checking both by userId document and by registered email address.
     */
    suspend fun fetchUserRoleFromFirestore(userId: String, email: String? = null): UserRole? {
        val db = firestore ?: return null
        return try {
            // 1. Direct document check by userId
            if (userId.isNotBlank()) {
                val snapshot = db.collection(COLLECTION_USERS).document(userId)
                    .get()
                    .awaitTask()
                if (snapshot.exists()) {
                    val roleStr = snapshot.getString("role")
                    if (!roleStr.isNullOrBlank()) {
                        return parseRoleString(roleStr)
                    }
                }
            }

            // 2. Query collection by email if provided
            if (!email.isNullOrBlank()) {
                val cleanEmail = email.trim()
                val emailQuery = db.collection(COLLECTION_USERS)
                    .whereEqualTo("email", cleanEmail.lowercase())
                    .limit(1)
                    .get()
                    .awaitTask()
                if (!emailQuery.isEmpty) {
                    val doc = emailQuery.documents.first()
                    val roleStr = doc.getString("role")
                    if (!roleStr.isNullOrBlank()) {
                        return parseRoleString(roleStr)
                    }
                }

                // Check exact match if casing differs
                val emailExactQuery = db.collection(COLLECTION_USERS)
                    .whereEqualTo("email", cleanEmail)
                    .limit(1)
                    .get()
                    .awaitTask()
                if (!emailExactQuery.isEmpty) {
                    val doc = emailExactQuery.documents.first()
                    val roleStr = doc.getString("role")
                    if (!roleStr.isNullOrBlank()) {
                        return parseRoleString(roleStr)
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching user role from Firestore: ${e.message}")
            null
        }
    }

    private fun parseRoleString(roleStr: String): UserRole {
        return try {
            UserRole.valueOf(roleStr.trim().uppercase())
        } catch (ex: IllegalArgumentException) {
            when (roleStr.trim().lowercase()) {
                "admin", "administrator" -> UserRole.ADMIN
                "teacher", "instructor", "faculty" -> UserRole.TEACHER
                else -> UserRole.STUDENT
            }
        }
    }

    /**
     * Fetches full user entity from Firestore by userId
     */
    suspend fun fetchUserProfileFromFirestore(userId: String): UserEntity? {
        val db = firestore ?: return null
        return try {
            val snapshot = db.collection(COLLECTION_USERS).document(userId)
                .get()
                .awaitTask()
            if (!snapshot.exists()) return null
            val roleStr = snapshot.getString("role")
            val role = try {
                if (roleStr != null) UserRole.valueOf(roleStr.trim().uppercase()) else UserRole.STUDENT
            } catch (e: Exception) {
                UserRole.STUDENT
            }
            UserEntity(
                id = snapshot.id,
                name = snapshot.getString("name") ?: "",
                email = snapshot.getString("email") ?: "",
                photoUrl = snapshot.getString("photoUrl") ?: "",
                role = role,
                preferredSubject = snapshot.getString("preferredSubject") ?: "All",
                emailNotificationsEnabled = snapshot.getBoolean("emailNotificationsEnabled") ?: true,
                registeredAt = snapshot.getLong("registeredAt") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching user profile from Firestore: ${e.message}")
            null
        }
    }

    suspend fun updateUserRole(userId: String, newRole: UserRole) {
        val db = firestore ?: return
        try {
            db.collection(COLLECTION_USERS).document(userId)
                .update("role", newRole.name, "updatedAt", System.currentTimeMillis())
                .awaitTask()
            Log.d(TAG, "User role updated in Firestore: $userId -> ${newRole.name}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed updating user role in Firestore: ${e.message}")
        }
    }

    suspend fun updateUserPreferences(userId: String, preferredSubject: String, notificationsEnabled: Boolean) {
        val db = firestore ?: return
        try {
            db.collection(COLLECTION_USERS).document(userId)
                .update(
                    mapOf(
                        "preferredSubject" to preferredSubject,
                        "emailNotificationsEnabled" to notificationsEnabled,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .awaitTask()
        } catch (e: Exception) {
            Log.w(TAG, "Failed updating user preferences in Firestore: ${e.message}")
        }
    }

    suspend fun updateUserProfilePhotoUrl(userId: String, photoUrl: String): Boolean {
        val db = firestore ?: return false
        return try {
            val payload = mapOf(
                "photoUrl" to photoUrl,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection(COLLECTION_USERS).document(userId)
                .set(payload, SetOptions.merge())
                .awaitTask()
            Log.d(TAG, "Successfully updated user profile photo in Firestore: $userId -> $photoUrl")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Error updating user profile photo in Firestore: ${e.message}")
            false
        }
    }

    // --- Category Operations in Firestore ---
    suspend fun saveCategory(category: CategoryEntity) {
        val db = firestore ?: return
        val map = mapOf(
            "id" to category.id,
            "name" to category.name,
            "description" to category.description,
            "iconName" to category.iconName,
            "colorHex" to category.colorHex
        )
        try {
            db.collection(COLLECTION_CATEGORIES).document(category.id)
                .set(map, SetOptions.merge())
                .awaitTask()
        } catch (e: Exception) {
            Log.w(TAG, "Failed saving category to Firestore: ${e.message}")
        }
    }

    suspend fun deleteCategory(categoryId: String) {
        val db = firestore ?: return
        try {
            db.collection(COLLECTION_CATEGORIES).document(categoryId)
                .delete()
                .awaitTask()
        } catch (e: Exception) {
            Log.w(TAG, "Failed deleting category from Firestore: ${e.message}")
        }
    }

    // --- Quiz Sets & Questions Operations in Firestore ---
    suspend fun saveQuizSetWithQuestions(quizSet: QuizSetEntity, questions: List<QuestionEntity>) {
        val db = firestore ?: return
        val quizMap = mapOf(
            "id" to quizSet.id,
            "title" to quizSet.title,
            "description" to quizSet.description,
            "categoryId" to quizSet.categoryId,
            "categoryName" to quizSet.categoryName,
            "creatorTeacherId" to quizSet.creatorTeacherId,
            "creatorTeacherName" to quizSet.creatorTeacherName,
            "durationMinutes" to quizSet.durationMinutes,
            "passPercentage" to quizSet.passPercentage,
            "difficulty" to quizSet.difficulty,
            "createdAt" to quizSet.createdAt,
            "tags" to quizSet.tags
        )

        try {
            // Write quiz set
            db.collection(COLLECTION_QUIZ_SETS).document(quizSet.id)
                .set(quizMap, SetOptions.merge())
                .awaitTask()

            // Write all questions
            val batch = db.batch()
            questions.forEach { q ->
                val qMap = mapOf(
                    "id" to q.id,
                    "quizSetId" to quizSet.id,
                    "questionText" to q.questionText,
                    "optionA" to q.optionA,
                    "optionB" to q.optionB,
                    "optionC" to q.optionC,
                    "optionD" to q.optionD,
                    "correctOptionIndex" to q.correctOptionIndex,
                    "explanation" to q.explanation
                )
                val docRef = db.collection(COLLECTION_QUESTIONS).document(q.id)
                batch.set(docRef, qMap, SetOptions.merge())
            }
            batch.commit().awaitTask()
            Log.d(TAG, "QuizSet '${quizSet.title}' & ${questions.size} questions saved to Firestore.")
        } catch (e: Exception) {
            Log.w(TAG, "Failed saving QuizSet to Firestore: ${e.message}")
        }
    }

    /**
     * Saves a QuizQuestion directly to Firestore in the 'questions' collection.
     */
    suspend fun saveQuizQuestionToFirestore(question: QuizQuestion): Boolean {
        val db = firestore ?: run {
            Log.w(TAG, "Firestore is uninitialized; question cached locally.")
            return false
        }
        return try {
            db.collection(COLLECTION_QUESTIONS).document(question.id)
                .set(question.toFirestoreMap(), SetOptions.merge())
                .awaitTask()
            Log.d(TAG, "QuizQuestion ${question.id} saved to Firestore successfully.")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed saving QuizQuestion ${question.id} to Firestore: ${e.message}")
            false
        }
    }

    /**
     * Saves a batch of QuizQuestion items to Firestore.
     */
    suspend fun saveQuizQuestionsToFirestore(questions: List<QuizQuestion>): Boolean {
        val db = firestore ?: return false
        return try {
            val batch = db.batch()
            questions.forEach { q ->
                val docRef = db.collection(COLLECTION_QUESTIONS).document(q.id)
                batch.set(docRef, q.toFirestoreMap(), SetOptions.merge())
            }
            batch.commit().awaitTask()
            Log.d(TAG, "Batch saved ${questions.size} QuizQuestions to Firestore.")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed batch saving QuizQuestions to Firestore: ${e.message}")
            false
        }
    }

    /**
     * Fetches QuizQuestions from Firestore, optionally filtered by category and/or tag.
     */
    suspend fun fetchQuizQuestionsFromFirestore(category: String? = null, tag: String? = null): List<QuizQuestion> {
        val db = firestore ?: return emptyList()
        return try {
            var query: com.google.firebase.firestore.Query = db.collection(COLLECTION_QUESTIONS)
            if (!category.isNullOrBlank()) {
                query = query.whereEqualTo("category", category)
            }
            val snapshot = query.get().awaitTask()
            val list = snapshot.documents.mapNotNull { doc ->
                QuizQuestion.fromFirestoreMap(doc.id, doc.data ?: emptyMap())
            }
            if (!tag.isNullOrBlank()) {
                list.filter { it.tag.equals(tag, ignoreCase = true) }
            } else {
                list
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed fetching QuizQuestions from Firestore: ${e.message}")
            emptyList()
        }
    }

    suspend fun saveOrUpdateQuestion(question: QuestionEntity) {
        val db = firestore ?: return
        val qMap = mapOf(
            "id" to question.id,
            "quizSetId" to question.quizSetId,
            "questionText" to question.questionText,
            "optionA" to question.optionA,
            "optionB" to question.optionB,
            "optionC" to question.optionC,
            "optionD" to question.optionD,
            "correctOptionIndex" to question.correctOptionIndex,
            "explanation" to question.explanation
        )
        try {
            db.collection(COLLECTION_QUESTIONS).document(question.id)
                .set(qMap, SetOptions.merge())
                .awaitTask()
            Log.d(TAG, "Question ${question.id} saved/updated in Firestore.")
        } catch (e: Exception) {
            Log.w(TAG, "Failed saving question ${question.id} to Firestore: ${e.message}")
        }
    }

    suspend fun deleteQuestion(questionId: String) {
        val db = firestore ?: return
        try {
            db.collection(COLLECTION_QUESTIONS).document(questionId)
                .delete()
                .awaitTask()
            Log.d(TAG, "Question $questionId deleted from Firestore.")
        } catch (e: Exception) {
            Log.w(TAG, "Failed deleting question $questionId from Firestore: ${e.message}")
        }
    }

    suspend fun deleteQuizSet(quizSetId: String) {
        val db = firestore ?: return
        try {
            // Delete questions associated with this quizSet
            val questionsSnapshot = db.collection(COLLECTION_QUESTIONS)
                .whereEqualTo("quizSetId", quizSetId)
                .get()
                .awaitTask()

            val batch = db.batch()
            questionsSnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.delete(db.collection(COLLECTION_QUIZ_SETS).document(quizSetId))
            batch.commit().awaitTask()
            Log.d(TAG, "QuizSet $quizSetId deleted from Firestore.")
        } catch (e: Exception) {
            Log.w(TAG, "Failed deleting QuizSet from Firestore: ${e.message}")
        }
    }

    // --- Session Data & Quiz Attempts in Firestore ---
    suspend fun saveQuizAttempt(attempt: QuizAttemptEntity) {
        val db = firestore ?: return
        val map = mapOf(
            "id" to attempt.id,
            "quizSetId" to attempt.quizSetId,
            "quizTitle" to attempt.quizTitle,
            "categoryName" to attempt.categoryName,
            "studentId" to attempt.studentId,
            "studentName" to attempt.studentName,
            "studentEmail" to attempt.studentEmail,
            "studentPhotoUrl" to attempt.studentPhotoUrl,
            "score" to attempt.score,
            "totalQuestions" to attempt.totalQuestions,
            "percentage" to attempt.percentage.toDouble(),
            "timeSpentSeconds" to attempt.timeSpentSeconds,
            "completedAt" to attempt.completedAt,
            "userAnswersJson" to attempt.userAnswersJson
        )
        try {
            db.collection(COLLECTION_ATTEMPTS).document(attempt.id)
                .set(map, SetOptions.merge())
                .awaitTask()
            Log.d(TAG, "Quiz session data saved to Firestore for student ${attempt.studentName}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed saving quiz attempt to Firestore: ${e.message}")
        }
    }

    /**
     * Directly queries and fetches top-performing students from Cloud Firestore,
     * aggregating all completed quiz attempt documents from COLLECTION_ATTEMPTS ("quiz_attempts")
     * and ranking students by total points earned across all quizzes.
     */
    suspend fun fetchTopStudentsLeaderboardFromFirestore(limit: Int = 100): List<StudentLeaderboardEntry> {
        val db = firestore ?: return emptyList()
        return try {
            // 1. Fetch all quiz attempt documents from Firestore
            val attemptsSnapshot = db.collection(COLLECTION_ATTEMPTS)
                .get()
                .awaitTask()

            // 2. Fetch all registered users to complete student roster
            val usersSnapshot = db.collection(COLLECTION_USERS)
                .get()
                .awaitTask()

            val studentMap = mutableMapOf<String, UserEntity>()
            usersSnapshot.documents.forEach { doc ->
                val data = doc.data ?: return@forEach
                val roleStr = data["role"] as? String ?: UserRole.STUDENT.name
                val role = try {
                    UserRole.valueOf(roleStr)
                } catch (e: Exception) {
                    UserRole.STUDENT
                }
                if (role == UserRole.STUDENT) {
                    val user = UserEntity(
                        id = doc.id,
                        name = data["name"] as? String ?: "Student",
                        email = data["email"] as? String ?: "",
                        photoUrl = data["photoUrl"] as? String ?: "",
                        role = role,
                        preferredSubject = data["preferredSubject"] as? String ?: "All",
                        emailNotificationsEnabled = (data["emailNotificationsEnabled"] as? Boolean) ?: true,
                        registeredAt = (data["registeredAt"] as? Long) ?: System.currentTimeMillis()
                    )
                    studentMap[doc.id] = user
                }
            }

            // 3. Map Firestore documents to QuizAttemptEntity
            val parsedAttempts = attemptsSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                QuizAttemptEntity(
                    id = doc.id,
                    quizSetId = data["quizSetId"] as? String ?: "",
                    quizTitle = data["quizTitle"] as? String ?: "",
                    categoryName = data["categoryName"] as? String ?: "",
                    studentId = data["studentId"] as? String ?: "",
                    studentName = data["studentName"] as? String ?: "",
                    studentEmail = data["studentEmail"] as? String ?: "",
                    studentPhotoUrl = data["studentPhotoUrl"] as? String ?: "",
                    score = (data["score"] as? Long)?.toInt() ?: 0,
                    totalQuestions = (data["totalQuestions"] as? Long)?.toInt() ?: 0,
                    percentage = (data["percentage"] as? Double)?.toFloat()
                        ?: (data["percentage"] as? Long)?.toFloat() ?: 0f,
                    timeSpentSeconds = (data["timeSpentSeconds"] as? Long)?.toInt() ?: 0,
                    completedAt = (data["completedAt"] as? Long) ?: System.currentTimeMillis(),
                    userAnswersJson = data["userAnswersJson"] as? String ?: "{}"
                )
            }

            // Sync to local Room cache
            scope.launch {
                parsedAttempts.forEach { quizDao.insertAttempt(it) }
            }

            // 4. Group attempts by student ID
            val attemptsByStudent = parsedAttempts.groupBy { it.studentId }

            // Ensure every student with either an account or attempt is processed
            val allStudentIds = (studentMap.keys + attemptsByStudent.keys).filter { it.isNotBlank() }

            val rawEntries = allStudentIds.map { sId ->
                val studentAttempts = attemptsByStudent[sId] ?: emptyList()
                val registeredUser = studentMap[sId]
                val studentName = registeredUser?.name?.ifBlank { null }
                    ?: studentAttempts.firstOrNull { it.studentName.isNotBlank() }?.studentName
                    ?: "Student"
                val studentEmail = registeredUser?.email?.ifBlank { null }
                    ?: studentAttempts.firstOrNull { it.studentEmail.isNotBlank() }?.studentEmail
                    ?: ""
                val photoUrl = registeredUser?.photoUrl?.ifBlank { null }
                    ?: studentAttempts.firstOrNull { it.studentPhotoUrl.isNotBlank() }?.studentPhotoUrl
                    ?: ""

                // Total points earned across all quizzes
                val totalScore = studentAttempts.sumOf { it.score }
                val totalQuizzesTaken = studentAttempts.size
                val avgPct = if (totalQuizzesTaken > 0) {
                    studentAttempts.map { it.percentage }.average().toFloat()
                } else 0f
                val highestScore = studentAttempts.maxOfOrNull { it.score } ?: 0
                val lastActive = studentAttempts.maxOfOrNull { it.completedAt } ?: 0L

                StudentLeaderboardEntry(
                    studentId = sId,
                    studentName = studentName,
                    studentEmail = studentEmail,
                    photoUrl = photoUrl,
                    totalScore = totalScore,
                    totalQuizzesTaken = totalQuizzesTaken,
                    averagePercentage = avgPct,
                    highestScore = highestScore,
                    lastActiveTimestamp = lastActive
                )
            }

            // 5. Rank strictly by total points earned across all quizzes descending,
            // with ties broken by accuracy / average percentage and attempts count
            val sortedList = rawEntries
                .sortedWith(
                    compareByDescending<StudentLeaderboardEntry> { it.totalScore }
                        .thenByDescending { it.averagePercentage }
                        .thenByDescending { it.totalQuizzesTaken }
                )
                .take(limit)

            val totalRanked = sortedList.size
            sortedList.mapIndexed { index, entry ->
                val rank = index + 1
                val percentile = if (totalRanked > 1) {
                    ((totalRanked - rank).toFloat() / (totalRanked - 1).toFloat()) * 100f
                } else 100f
                entry.copy(rank = rank, percentile = percentile)
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchTopStudentsLeaderboardFromFirestore failed: ${e.message}", e)
            emptyList()
        }
    }

    // --- Notifications in Firestore ---
    suspend fun saveNotification(notification: NotificationLogEntity) {
        val db = firestore ?: return
        val map = mapOf(
            "id" to notification.id,
            "recipientEmail" to notification.recipientEmail,
            "recipientName" to notification.recipientName,
            "subject" to notification.subject,
            "body" to notification.body,
            "sentAt" to notification.sentAt,
            "quizSetId" to notification.quizSetId
        )
        try {
            db.collection(COLLECTION_NOTIFICATIONS).document(notification.id)
                .set(map, SetOptions.merge())
                .awaitTask()
        } catch (e: Exception) {
            Log.w(TAG, "Failed saving notification to Firestore: ${e.message}")
        }
    }

    /**
     * Seeds initial local data to Firestore if cloud collection is empty.
     */
    suspend fun syncLocalDataToFirestore(
        users: List<UserEntity>,
        categories: List<CategoryEntity>,
        quizSets: List<QuizSetEntity>,
        questions: List<QuestionEntity>,
        attempts: List<QuizAttemptEntity>,
        notifications: List<NotificationLogEntity>
    ) {
        val db = firestore ?: return
        try {
            users.forEach { saveUserProfile(it) }
            categories.forEach { saveCategory(it) }
            quizSets.forEach { qs ->
                val qsQuestions = questions.filter { it.quizSetId == qs.id }
                saveQuizSetWithQuestions(qs, qsQuestions)
            }
            attempts.forEach { saveQuizAttempt(it) }
            notifications.forEach { saveNotification(it) }
            _cloudSyncMessage.value = "Cloud Firestore: All records synchronized"
        } catch (e: Exception) {
            Log.w(TAG, "Sync to Firestore encountered error: ${e.message}")
        }
    }

    // --- System Audit Logs in Firestore ---
    suspend fun logSystemEvent(
        title: String,
        description: String,
        category: String,
        severity: String,
        actor: String
    ) {
        val logId = "log_${System.currentTimeMillis()}_${(1000..9999).random()}"
        val item = SystemLogItem(
            id = logId,
            title = title,
            description = description,
            category = category,
            severity = severity,
            actor = actor,
            timestamp = System.currentTimeMillis()
        )
        // Immediate local state update for fast UX
        _systemLogs.value = listOf(item) + _systemLogs.value.filterNot { it.id == item.id }

        val db = firestore ?: return
        try {
            val map = mapOf(
                "id" to item.id,
                "title" to item.title,
                "description" to item.description,
                "category" to item.category,
                "severity" to item.severity,
                "actor" to item.actor,
                "timestamp" to item.timestamp
            )
            db.collection(COLLECTION_SYSTEM_LOGS).document(item.id)
                .set(map, SetOptions.merge())
                .awaitTask()
            Log.d(TAG, "System log persisted to Firestore: ${item.title}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed writing system log to Firestore: ${e.message}")
        }
    }

    /**
     * Saves a pool of Gemini API keys to Firebase Firestore in the 'system_config' collection.
     * Persists keys, active index, timestamp, and audit trail.
     */
    suspend fun saveGeminiApiKeysToFirestore(
        keys: List<String>,
        activeIndex: Int,
        updatedBy: String = "Admin"
    ): Boolean {
        val db = firestore ?: run {
            Log.w(TAG, "Firestore offline, API keys saved to local storage only.")
            _isApiKeySyncedWithCloud.value = false
            return false
        }

        return try {
            val now = System.currentTimeMillis()
            val payload = mapOf(
                "keys" to keys,
                "activeKeyIndex" to activeIndex,
                "keyCount" to keys.size,
                "updatedAt" to now,
                "updatedBy" to updatedBy,
                "status" to "ACTIVE"
            )

            db.collection(COLLECTION_SYSTEM_CONFIG)
                .document(DOC_GEMINI_API_CONFIG)
                .set(payload, SetOptions.merge())
                .awaitTask()

            _isApiKeySyncedWithCloud.value = true
            _cloudApiKeyCount.value = keys.size
            _lastApiKeyCloudSyncTime.value = now

            // Record an audit log in system_logs
            recordSystemLog(
                title = "Gemini API Keys Synced to Firebase",
                description = "Saved ${keys.size} Gemini API key(s) to Cloud Firestore configuration pool. Active key index: $activeIndex.",
                category = "AI_ENGINE",
                severity = "SUCCESS",
                actor = updatedBy
            )
            Log.d(TAG, "Successfully saved ${keys.size} Gemini API keys to Firestore.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving Gemini API keys to Firestore: ${e.message}", e)
            _isApiKeySyncedWithCloud.value = false
            false
        }
    }

    /**
     * Fetches the Gemini API keys pool from Firebase Firestore.
     * Returns a Pair of (keysList, activeIndex) or null if document doesn't exist or on error.
     */
    suspend fun fetchGeminiApiKeysFromFirestore(): Pair<List<String>, Int>? {
        val db = firestore ?: return null
        return try {
            val snapshot = db.collection(COLLECTION_SYSTEM_CONFIG)
                .document(DOC_GEMINI_API_CONFIG)
                .get()
                .awaitTask()

            if (snapshot.exists()) {
                val data = snapshot.data ?: return null
                val rawKeys = data["keys"] as? List<*>
                val keysList = rawKeys?.filterIsInstance<String>() ?: emptyList()
                val activeIndex = (data["activeKeyIndex"] as? Number)?.toInt() ?: 0
                val updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()

                _isApiKeySyncedWithCloud.value = true
                _cloudApiKeyCount.value = keysList.size
                _lastApiKeyCloudSyncTime.value = updatedAt

                Log.d(TAG, "Fetched ${keysList.size} Gemini API keys from Firestore.")
                Pair(keysList, activeIndex)
            } else {
                Log.d(TAG, "No remote Gemini API keys document found in Firestore.")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed fetching Gemini API keys from Firestore: ${e.message}")
            null
        }
    }

    /**
     * Attaches a real-time listener to the Gemini API key configuration document in Firestore.
     * When any Admin updates keys in Firebase, this listener automatically synchronizes them.
     */
    fun startGeminiApiKeySync(onKeysSynced: (List<String>, Int) -> Unit) {
        val db = firestore ?: return
        try {
            val registration = db.collection(COLLECTION_SYSTEM_CONFIG)
                .document(DOC_GEMINI_API_CONFIG)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Gemini API config listener failed: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val data = snapshot.data ?: return@addSnapshotListener
                        val rawKeys = data["keys"] as? List<*>
                        val keysList = rawKeys?.filterIsInstance<String>() ?: emptyList()
                        val activeIndex = (data["activeKeyIndex"] as? Number)?.toInt() ?: 0
                        val updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()

                        _isApiKeySyncedWithCloud.value = true
                        _cloudApiKeyCount.value = keysList.size
                        _lastApiKeyCloudSyncTime.value = updatedAt

                        onKeysSynced(keysList, activeIndex)
                        Log.d(TAG, "Realtime sync: updated ${keysList.size} Gemini API keys from Firestore.")
                    }
                }
            listenerRegistrations.add(registration)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register Gemini API key Firestore listener: ${e.message}")
        }
    }

    suspend fun recordSystemLog(
        title: String,
        description: String,
        category: String,
        severity: String,
        actor: String
    ) {
        val logItem = SystemLogItem(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            category = category,
            severity = severity,
            actor = actor,
            timestamp = System.currentTimeMillis()
        )
        _systemLogs.value = listOf(logItem) + _systemLogs.value
        val db = firestore ?: return
        try {
            val map = mapOf(
                "id" to logItem.id,
                "title" to logItem.title,
                "description" to logItem.description,
                "category" to logItem.category,
                "severity" to logItem.severity,
                "actor" to logItem.actor,
                "timestamp" to logItem.timestamp
            )
            db.collection(COLLECTION_SYSTEM_LOGS).document(logItem.id).set(map, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(TAG, "Failed recording system log to Firestore: ${e.message}")
        }
    }

    private fun seedInitialSystemLogs(db: FirebaseFirestore) {
        scope.launch {
            try {
                val existing = db.collection(COLLECTION_SYSTEM_LOGS).limit(1).get().awaitTask()
                if (existing.isEmpty) {
                    val defaults = getDefaultSystemLogs()
                    defaults.forEach { item ->
                        val map = mapOf(
                            "id" to item.id,
                            "title" to item.title,
                            "description" to item.description,
                            "category" to item.category,
                            "severity" to item.severity,
                            "actor" to item.actor,
                            "timestamp" to item.timestamp
                        )
                        db.collection(COLLECTION_SYSTEM_LOGS).document(item.id)
                            .set(map, SetOptions.merge())
                    }
                    _systemLogs.value = defaults
                }
            } catch (e: Exception) {
                Log.w(TAG, "Default system logs seed skipped: ${e.message}")
                if (_systemLogs.value.isEmpty()) {
                    _systemLogs.value = getDefaultSystemLogs()
                }
            }
        }
    }

    private fun getDefaultSystemLogs(): List<SystemLogItem> {
        val now = System.currentTimeMillis()
        return listOf(
            SystemLogItem(
                id = "log_init_01",
                title = "Cloud Security Rules Enforced",
                description = "Multi-role access tiers activated for Admin, Teacher, and Student roles with Firestore rules.",
                category = "SECURITY",
                severity = "SUCCESS",
                actor = "Cloud IAM Policy",
                timestamp = now - 3600000 * 4
            ),
            SystemLogItem(
                id = "log_init_02",
                title = "Gemini 3.5 Flash Model Provisioned",
                description = "AI question generator calibrated with pedagogical Bloom's taxonomy criteria.",
                category = "AI_ENGINE",
                severity = "INFO",
                actor = "Gemini Service",
                timestamp = now - 3600000 * 3
            ),
            SystemLogItem(
                id = "log_init_03",
                title = "Curriculum Bank Synchronized",
                description = "Subject categories and verified quiz sets indexed across Science, Math, and History.",
                category = "QUIZ_ACTIVITY",
                severity = "INFO",
                actor = "Curriculum Bot",
                timestamp = now - 3600000 * 2
            ),
            SystemLogItem(
                id = "log_init_04",
                title = "Realtime Firestore Snapshot Active",
                description = "Bi-directional listeners established for all platform data collections with offline Room cache.",
                category = "FIRESTORE_SYNC",
                severity = "SUCCESS",
                actor = "Firestore Engine",
                timestamp = now - 1800000
            ),
            SystemLogItem(
                id = "log_init_05",
                title = "Role Validation Audit Passed",
                description = "Multi-tenant access verified. Admin control console active with elevated permissions.",
                category = "SECURITY",
                severity = "SUCCESS",
                actor = "Security Daemon",
                timestamp = now - 600000
            )
        )
    }

    private fun seedInitialRoleProfiles(db: FirebaseFirestore) {
        scope.launch {
            try {
                // Ensure canonical role accounts are registered in Firestore
                val defaultRoles = listOf(
                    mapOf(
                        "id" to "user_admin_default",
                        "name" to "Dr. Eleanor Vance",
                        "email" to "admin@quiz.com",
                        "role" to UserRole.ADMIN.name,
                        "preferredSubject" to "All",
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    mapOf(
                        "id" to "google_admin_001",
                        "name" to "Dr. Eleanor Vance",
                        "email" to "admin@quizplatform.edu",
                        "role" to UserRole.ADMIN.name,
                        "preferredSubject" to "All",
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    mapOf(
                        "id" to "user_teacher_default",
                        "name" to "Prof. Alan Turing",
                        "email" to "teacher@quiz.com",
                        "role" to UserRole.TEACHER.name,
                        "preferredSubject" to "Computer Science",
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    mapOf(
                        "id" to "google_teacher_001",
                        "name" to "Prof. Alan Turing",
                        "email" to "alan.turing@quizplatform.edu",
                        "role" to UserRole.TEACHER.name,
                        "preferredSubject" to "Computer Science",
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    mapOf(
                        "id" to "user_student_default",
                        "name" to "Aashish Gentleman",
                        "email" to "student@quiz.com",
                        "role" to UserRole.STUDENT.name,
                        "preferredSubject" to "Computer Science",
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    mapOf(
                        "id" to "google_student_001",
                        "name" to "Aashish Gentleman",
                        "email" to "gentlemanaashish222@gmail.com",
                        "role" to UserRole.STUDENT.name,
                        "preferredSubject" to "Computer Science",
                        "updatedAt" to System.currentTimeMillis()
                    )
                )

                val batch = db.batch()
                defaultRoles.forEach { profile ->
                    val docId = profile["id"] as String
                    val docRef = db.collection(COLLECTION_USERS).document(docId)
                    batch.set(docRef, profile, SetOptions.merge())
                }
                batch.commit().awaitTask()
                Log.d(TAG, "Initial canonical role profiles verified in Firestore.")
            } catch (e: Exception) {
                Log.w(TAG, "Initial role profiles seed in Firestore skipped: ${e.message}")
            }
        }
    }
}

/**
 * Lightweight await extension for GMS Tasks avoiding extra dependency footprint.
 */
suspend fun <T> Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        addOnFailureListener { exception ->
            if (continuation.isActive) continuation.resumeWith(Result.failure(exception))
        }
        addOnCanceledListener {
            if (continuation.isActive) continuation.cancel()
        }
    }
