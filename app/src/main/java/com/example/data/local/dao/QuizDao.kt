package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.*
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {

    // --- Users ---
    @Query("SELECT * FROM users ORDER BY registeredAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = :role")
    fun getUsersByRole(role: UserRole): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET role = :role WHERE id = :userId")
    suspend fun updateUserRole(userId: String, role: UserRole)

    @Query("UPDATE users SET preferredSubject = :subject, emailNotificationsEnabled = :notificationsEnabled WHERE id = :userId")
    suspend fun updateUserPreferences(userId: String, subject: String, notificationsEnabled: Boolean)

    @Query("UPDATE users SET photoUrl = :photoUrl WHERE id = :userId")
    suspend fun updateUserPhotoUrl(userId: String, photoUrl: String)


    // --- Categories ---
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategory(categoryId: String)


    // --- Quiz Sets ---
    @Query("SELECT * FROM quiz_sets ORDER BY createdAt DESC")
    fun getAllQuizSets(): Flow<List<QuizSetEntity>>

    @Query("SELECT * FROM quiz_sets WHERE categoryId = :categoryId ORDER BY createdAt DESC")
    fun getQuizSetsByCategory(categoryId: String): Flow<List<QuizSetEntity>>

    @Query("SELECT * FROM quiz_sets WHERE id = :quizSetId LIMIT 1")
    suspend fun getQuizSetById(quizSetId: String): QuizSetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizSet(quizSet: QuizSetEntity)

    @Query("DELETE FROM quiz_sets WHERE id = :quizSetId")
    suspend fun deleteQuizSet(quizSetId: String)


    // --- Questions ---
    @Query("SELECT * FROM questions")
    fun getAllQuestions(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE quizSetId = :quizSetId")
    fun getQuestionsForQuizSetFlow(quizSetId: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE quizSetId = :quizSetId")
    suspend fun getQuestionsForQuizSet(quizSetId: String): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE id = :questionId LIMIT 1")
    suspend fun getQuestionById(questionId: String): QuestionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity)

    @Query("DELETE FROM questions WHERE id = :questionId")
    suspend fun deleteQuestionById(questionId: String)

    @Query("DELETE FROM questions WHERE quizSetId = :quizSetId")
    suspend fun deleteQuestionsForQuizSet(quizSetId: String)


    // --- Attempts ---
    @Query("SELECT * FROM quiz_attempts ORDER BY completedAt DESC")
    fun getAllAttempts(): Flow<List<QuizAttemptEntity>>

    @Query("SELECT * FROM quiz_attempts WHERE studentId = :studentId ORDER BY completedAt DESC")
    fun getAttemptsForStudent(studentId: String): Flow<List<QuizAttemptEntity>>

    @Query("SELECT * FROM quiz_attempts WHERE id = :attemptId LIMIT 1")
    suspend fun getAttemptById(attemptId: String): QuizAttemptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: QuizAttemptEntity)


    // --- Notification Logs ---
    @Query("SELECT * FROM notification_logs ORDER BY sentAt DESC")
    fun getAllNotificationLogs(): Flow<List<NotificationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationLog(log: NotificationLogEntity)
}
