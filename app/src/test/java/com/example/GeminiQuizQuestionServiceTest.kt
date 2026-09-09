package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.gemini.GeminiApiKeyManager
import com.example.data.gemini.GeminiQuizQuestionService
import com.example.data.gemini.QuizDifficulty
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeminiQuizQuestionServiceTest {

    private lateinit var apiKeyManager: GeminiApiKeyManager
    private lateinit var questionService: GeminiQuizQuestionService

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        apiKeyManager = GeminiApiKeyManager(context)
        apiKeyManager.clearAllKeys()
        apiKeyManager.clearCooldowns()
        questionService = GeminiQuizQuestionService(apiKeyManager)
    }

    @Test
    fun testApiKeyManagerAddAndMaskKeys() {
        val input = "test_gemini_key_alpha_1234, test_gemini_key_beta_5678\ntest_gemini_key_gamma_9012"
        val count = apiKeyManager.addApiKeys(input)
        assertEquals(3, count)

        val keys = apiKeyManager.getAvailableApiKeys()
        assertEquals(3, keys.size)

        val masked = apiKeyManager.maskKey(keys[0])
        assertTrue(masked.startsWith("••••••••"))
        assertTrue(masked.endsWith("1234"))

        // Test removal
        apiKeyManager.removeApiKey(1)
        assertEquals(2, apiKeyManager.getAvailableApiKeys().size)
    }

    @Test
    fun testModelRotationAndRateLimitCooldown() {
        val models = apiKeyManager.getAvailableModels()
        assertFalse(models.isEmpty())
        val firstModel = models.first()

        assertFalse(apiKeyManager.isModelRateLimited(firstModel))

        // Mark first model as rate-limited
        apiKeyManager.markModelRateLimited(firstModel)
        assertTrue(apiKeyManager.isModelRateLimited(firstModel))

        // Rotated available models should now place another model at the front
        val updatedModels = apiKeyManager.getAvailableModels()
        assertNotEquals(firstModel, updatedModels.first())

        // Clearing cooldowns restores first model
        apiKeyManager.clearCooldowns()
        assertFalse(apiKeyManager.isModelRateLimited(firstModel))
        assertEquals(firstModel, apiKeyManager.getAvailableModels().first())
    }

    @Test
    fun testGenerateQuizQuestionsFallback() = runBlocking {
        // Without keys, it should generate high-quality QuizQuestion objects safely
        val questions = questionService.generateQuizQuestions(
            topic = "Jetpack Compose State",
            difficultyLevel = "Hard",
            count = 3,
            category = "Android",
            tag = "#compose"
        )

        assertEquals(3, questions.size)
        questions.forEach { q ->
            assertTrue(q.id.isNotBlank())
            assertTrue(q.questionText.isNotBlank())
            assertEquals(4, q.options.size)
            assertTrue("Correct answer '${q.correctAnswer}' should be in options", q.options.contains(q.correctAnswer))
            assertEquals("Android", q.category)
            assertEquals("#compose", q.tag)
            assertTrue(q.explanation.isNotBlank())
        }
    }

    @Test
    fun testGenerateQuizQuestionsWithReport() = runBlocking {
        val report = questionService.generateQuizQuestionsWithReport(
            topic = "Clean Architecture",
            difficultyLevel = "Easy",
            count = 2,
            category = "Software Engineering",
            tag = "#architecture"
        )

        assertEquals(2, report.questions.size)
        assertTrue(report.isFallbackGenerated) // Since no remote key is provided in unit test
        assertTrue(report.failoverLogs.isNotEmpty())
        assertTrue(report.durationMs >= 0)
    }

    @Test
    fun testAllDifficultiesProduceValidQuestions() = runBlocking {
        listOf("Easy", "Medium", "Hard").forEach { diff ->
            val questions = questionService.generateQuizQuestions(
                topic = "Kotlin Coroutines",
                difficultyLevel = diff,
                count = 2
            )
            assertEquals(2, questions.size)
            assertTrue(questions.all { it.options.size >= 2 })
            assertTrue(questions.all { it.options.contains(it.correctAnswer) })
        }
    }
}
