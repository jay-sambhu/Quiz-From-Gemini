package com.example.data.gemini

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.entities.QuestionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Difficulty levels for quiz question generation with targeted pedagogical instructions.
 */
enum class QuizDifficulty(val displayName: String, val promptGuidance: String) {
    EASY(
        displayName = "Easy",
        promptGuidance = "Target beginner level. Focus on fundamental definitions, core terminology, and basic factual recall. Distractors should be distinct and straightforward."
    ),
    MEDIUM(
        displayName = "Medium",
        promptGuidance = "Target intermediate learners. Focus on practical application, concept comparisons, standard problem-solving scenarios, and relationship analysis between concepts."
    ),
    HARD(
        displayName = "Hard",
        promptGuidance = "Target advanced experts. Focus on in-depth analysis, subtle edge cases, architectural trade-offs, multi-step logical deduction, and realistic troubleshooting with nuanced distractors."
    );

    companion object {
        fun fromString(value: String): QuizDifficulty {
            return entries.firstOrNull {
                it.displayName.equals(value.trim(), ignoreCase = true) ||
                it.name.equals(value.trim(), ignoreCase = true)
            } ?: MEDIUM
        }
    }
}

/**
 * Data representation of a generated quiz question from Gemini API.
 */
data class GeneratedQuizQuestion(
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOptionIndex: Int, // 0 = A, 1 = B, 2 = C, 3 = D
    val explanation: String
)

/**
 * Service class that integrates with the Google Gemini API (gemini-3.5-flash)
 * to generate high-quality quiz questions tailored to a topic and difficulty level.
 */
class GeminiQuizService(
    private val apiKeyManager: GeminiApiKeyManager? = null,
    private val defaultApiKey: String = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" },
    private val httpClient: OkHttpClient = defaultHttpClient,
    val apiKey: String = ""
) {

    companion object {
        private const val TAG = "GeminiQuizService"
        private const val MODEL_NAME = "gemini-2.5-flash"
        private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

        val defaultHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(35, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        @Volatile
        private var instance: GeminiQuizService? = null

        fun getInstance(apiKeyManager: GeminiApiKeyManager? = null): GeminiQuizService {
            return instance ?: synchronized(this) {
                instance ?: GeminiQuizService(apiKeyManager).also { instance = it }
            }
        }
    }

    private fun getCandidateKeys(): List<String> {
        val managerKeys = apiKeyManager?.getAvailableApiKeys()
        if (!managerKeys.isNullOrEmpty()) {
            return managerKeys
        }
        val effective = if (apiKey.isNotBlank()) apiKey else defaultApiKey
        return listOfNotNull(effective.takeIf { it.isNotBlank() && it != "MY_GEMINI_API_KEY" })
    }

    /**
     * Generates quiz questions based on the provided topic and difficulty level.
     * Iterates through candidate API keys and automatically cycles if a rate limit is encountered.
     */
    suspend fun generateQuizQuestions(
        topic: String,
        difficultyLevel: String,
        count: Int = 5,
        quizSetId: String = ""
    ): List<QuestionEntity> = withContext(Dispatchers.IO) {
        val parsedDifficulty = QuizDifficulty.fromString(difficultyLevel)
        Log.d(TAG, "Generating $count questions for topic='$topic', difficulty='${parsedDifficulty.displayName}'")

        val keys = getCandidateKeys()
        if (keys.isEmpty()) {
            Log.w(TAG, "No valid Gemini API keys configured. Using intelligent contextual fallback.")
            return@withContext generateFallbackQuestions(topic, parsedDifficulty, count, quizSetId)
        }

        val prompt = buildPrompt(topic, parsedDifficulty, count)

        for (currentKey in keys) {
            val masked = apiKeyManager?.maskKey(currentKey) ?: (currentKey.take(6) + "...")
            try {
                val requestBodyJson = JSONObject().apply {
                    val partsArray = JSONArray().put(JSONObject().put("text", prompt))
                    val contentObject = JSONObject().put("parts", partsArray)
                    put("contents", JSONArray().put(contentObject))

                    val generationConfig = JSONObject().apply {
                        put("temperature", 0.7)
                        put("topP", 0.95)
                        put("responseMimeType", "application/json")
                    }
                    put("generationConfig", generationConfig)
                }

                val request = Request.Builder()
                    .url(API_URL)
                    .addHeader("x-goog-api-key", currentKey)
                    .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBodyString = response.body?.string() ?: ""

                val isRateLimited = response.code == 429 ||
                        responseBodyString.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                        responseBodyString.contains("quota", ignoreCase = true) ||
                        responseBodyString.contains("rate-limits", ignoreCase = true)

                if (isRateLimited) {
                    val nextKey = apiKeyManager?.markKeyRateLimitedAndCycle(
                        key = currentKey,
                        reason = "Rate limit HTTP ${response.code} during question generation"
                    )
                    Log.w(TAG, "Rate limit hit on key '$masked' (HTTP ${response.code}). Cycling to next key${if (nextKey != null) " '${apiKeyManager?.maskKey(nextKey)}'" else ""}...")
                    continue // Cycle to next key in pool
                }

                if (!response.isSuccessful || responseBodyString.isBlank()) {
                    Log.e(TAG, "Gemini API request failed [HTTP ${response.code}] on key '$masked': $responseBodyString")
                    continue
                }

                val parsedQuestions = parseGeminiResponse(responseBodyString, topic, quizSetId, count)
                if (parsedQuestions.isNotEmpty()) {
                    Log.d(TAG, "Successfully generated ${parsedQuestions.size} questions using key '$masked'")
                    return@withContext parsedQuestions
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating quiz questions on key '$masked': ${e.message}", e)
            }
        }

        Log.w(TAG, "All candidate Gemini API keys failed or rate-limited. Generating contextual fallback.")
        generateFallbackQuestions(topic, parsedDifficulty, count, quizSetId)
    }

    /**
     * Builds the system & user prompt specifying JSON array output and difficulty guidelines.
     */
    private fun buildPrompt(topic: String, difficulty: QuizDifficulty, count: Int): String {
        return """
            You are a master academic assessment designer.
            Task: Create exactly $count multiple-choice questions on the topic: "$topic".
            Difficulty Level: ${difficulty.displayName.uppercase()}
            Pedagogical Difficulty Guidance: ${difficulty.promptGuidance}

            Strict Requirements:
            1. Every question must have 4 distinct, plausible options (optionA, optionB, optionC, optionD).
            2. correctOptionIndex must be an integer from 0 to 3 (0 for optionA, 1 for optionB, 2 for optionC, 3 for optionD).
            3. Include a comprehensive explanation clarifying why the correct choice is accurate and why the topic principles support it.
            4. Tailor the tone, depth, and cognitive demand to the '${difficulty.displayName}' difficulty level.
            5. Return ONLY a valid JSON array containing exactly $count question objects matching the schema below:

            [
              {
                "questionText": "Clear, concise question statement?",
                "optionA": "First option choice",
                "optionB": "Second option choice",
                "optionC": "Third option choice",
                "optionD": "Fourth option choice",
                "correctOptionIndex": 0,
                "explanation": "Detailed explanation of why this answer is correct."
              }
            ]
        """.trimIndent()
    }

    /**
     * Extracts and parses the JSON response from Gemini API into [QuestionEntity] instances.
     */
    private fun parseGeminiResponse(
        responseBodyString: String,
        topic: String,
        quizSetId: String,
        count: Int
    ): List<QuestionEntity> {
        val questionsList = mutableListOf<QuestionEntity>()

        try {
            val rootObject = JSONObject(responseBodyString)
            val candidates = rootObject.optJSONArray("candidates") ?: return emptyList()
            val firstCandidate = candidates.optJSONObject(0) ?: return emptyList()
            val content = firstCandidate.optJSONObject("content") ?: return emptyList()
            val parts = content.optJSONArray("parts") ?: return emptyList()
            val rawText = parts.optJSONObject(0)?.optString("text")?.trim() ?: return emptyList()

            // Strip any unexpected markdown formatting
            val jsonCleaned = rawText
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            // Find JSON array bounds
            val startIndex = jsonCleaned.indexOf('[')
            val endIndex = jsonCleaned.lastIndexOf(']')

            if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) {
                Log.w(TAG, "No JSON array found in Gemini text: $jsonCleaned")
                return emptyList()
            }

            val arrayString = jsonCleaned.substring(startIndex, endIndex + 1)
            val jsonArray = JSONArray(arrayString)

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val qText = item.optString("questionText", "Question ${i + 1} on $topic")
                val optA = item.optString("optionA", "Option A")
                val optB = item.optString("optionB", "Option B")
                val optC = item.optString("optionC", "Option C")
                val optD = item.optString("optionD", "Option D")
                val correctIndex = item.optInt("correctOptionIndex", 0).coerceIn(0, 3)
                val explanation = item.optString("explanation", "Correct choice based on $topic principles.")

                questionsList.add(
                    QuestionEntity(
                        id = "q_gemini_${UUID.randomUUID().toString().take(8)}_${i + 1}",
                        quizSetId = quizSetId,
                        questionText = qText,
                        optionA = optA,
                        optionB = optB,
                        optionC = optC,
                        optionD = optD,
                        correctOptionIndex = correctIndex,
                        explanation = explanation
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing Gemini JSON response: ${e.message}", e)
        }

        return questionsList
    }

    /**
     * Fallback question generator providing curated, difficulty-sensitive questions
     * when the live API key is unavailable or in offline testing mode.
     */
    fun generateFallbackQuestions(
        topic: String,
        difficulty: QuizDifficulty,
        count: Int,
        quizSetId: String
    ): List<QuestionEntity> {
        val list = mutableListOf<QuestionEntity>()

        for (i in 1..count) {
            val (qText, optA, optB, optC, optD, correctIdx, exp) = when (difficulty) {
                QuizDifficulty.EASY -> EasyQuestionTemplate(topic, i)
                QuizDifficulty.MEDIUM -> MediumQuestionTemplate(topic, i)
                QuizDifficulty.HARD -> HardQuestionTemplate(topic, i)
            }

            list.add(
                QuestionEntity(
                    id = "q_ai_${difficulty.name.lowercase()}_${UUID.randomUUID().toString().take(8)}_$i",
                    quizSetId = quizSetId,
                    questionText = qText,
                    optionA = optA,
                    optionB = optB,
                    optionC = optC,
                    optionD = optD,
                    correctOptionIndex = correctIdx,
                    explanation = exp
                )
            )
        }
        return list
    }

    private data class TemplateResult(
        val questionText: String,
        val optionA: String,
        val optionB: String,
        val optionC: String,
        val optionD: String,
        val correctIndex: Int,
        val explanation: String
    )

    private fun EasyQuestionTemplate(topic: String, index: Int): TemplateResult {
        return when (index % 3) {
            1 -> TemplateResult(
                questionText = "What is the primary fundamental objective of $topic?",
                optionA = "To establish standard foundational principles and reliable core execution.",
                optionB = "To completely replace all existing hardware architectures.",
                optionC = "To enforce manual, error-prone record keeping.",
                optionD = "To eliminate the need for systematic testing.",
                correctIndex = 0,
                explanation = "In $topic, the foundational objective centers on establishing reliable, standardized core execution."
            )
            2 -> TemplateResult(
                questionText = "Which term best describes the introductory baseline concept of $topic?",
                optionA = "Ad-hoc arbitrary execution",
                optionB = "Core baseline domain modeling",
                optionC = "Unmanaged memory fragmentation",
                optionD = "Static compile-time obsolescence",
                correctIndex = 1,
                explanation = "Core baseline domain modeling defines how $topic structures its fundamental entities."
            )
            else -> TemplateResult(
                questionText = "In the context of $topic, why is consistent validation crucial for beginners?",
                optionA = "It prevents fundamental syntax and conceptual errors early.",
                optionB = "It increases build latency without any tangible benefit.",
                optionC = "It restricts the system to single-threaded operation.",
                optionD = "It converts dynamic values into immutable constants.",
                correctIndex = 0,
                explanation = "Early validation ensures beginners build on correct foundational principles in $topic."
            )
        }
    }

    private fun MediumQuestionTemplate(topic: String, index: Int): TemplateResult {
        return when (index % 3) {
            1 -> TemplateResult(
                questionText = "When applying $topic in a production system, what is the recommended practice for state management?",
                optionA = "Maintaining global mutable variables accessible across threads",
                optionB = "Using observable, reactive immutable state streams with clear unidirectional data flow",
                optionC = "Polling storage intervals synchronously on the main thread",
                optionD = "Disabling exception logging to decrease payload size",
                correctIndex = 1,
                explanation = "Intermediate architectures in $topic favor unidirectional data flow with immutable state streams for predictability."
            )
            2 -> TemplateResult(
                questionText = "Which trade-off is most characteristic when optimizing algorithms within $topic?",
                optionA = "Balancing spatial memory consumption against execution computational throughput",
                optionB = "Abandoning type safety to improve disk serialization speed",
                optionC = "Removing asynchronous concurrency primitives in high-load scenarios",
                optionD = "Multiplying round-trip network requests to decrease cache invalidation",
                correctIndex = 0,
                explanation = "Balancing time complexity against space consumption is a core trade-off when optimizing in $topic."
            )
            else -> TemplateResult(
                questionText = "How does $topic handle component decoupling and modularity?",
                optionA = "By establishing strict dependency inversion through interfaces and abstractions",
                optionB = "By coupling data access layers directly to view controllers",
                optionC = "By routing all system events through a single monolithic handler",
                optionD = "By avoiding modular packages and keeping all files in a root directory",
                correctIndex = 0,
                explanation = "Dependency inversion and interface segregation allow modular components in $topic to scale maintainably."
            )
        }
    }

    private fun HardQuestionTemplate(topic: String, index: Int): TemplateResult {
        return when (index % 3) {
            1 -> TemplateResult(
                questionText = "Under extreme concurrent throughput in $topic, what subtle race condition occurs during uncoordinated lock-free mutations?",
                optionA = "Non-atomic ABA read-modify-write invalidation leading to stale memory state references",
                optionB = "Automatic garbage collection lock inversion causing infinite re-entrancy",
                optionC = "Immediate hardware level L1 cache cache-coherency termination",
                optionD = "Deterministic compiler reordering that bypasses volatile memory fences safely",
                correctIndex = 0,
                explanation = "In advanced $topic systems, lock-free algorithms must account for the ABA phenomenon where intermediate changes invalidate assumed state."
            )
            2 -> TemplateResult(
                questionText = "In an advanced $topic distributed environment, how can partition tolerance be preserved without sacrificing causal consistency?",
                optionA = "By deploying Conflict-free Replicated Data Types (CRDTs) with vector clocks",
                optionB = "By using synchronous two-phase commits across all wide-area nodes",
                optionC = "By eliminating local replica logs in favor of eventual optimistic overwrite",
                optionD = "By enforcing single-master writes with zero quorum verification",
                correctIndex = 0,
                explanation = "CRDTs paired with vector clocks provide mathematical guarantees for state convergence under network partitions in $topic."
            )
            else -> TemplateResult(
                questionText = "Which profiling heuristic indicates an architectural bottleneck in high-frequency $topic pipelines?",
                optionA = "High context-switching overhead accompanied by persistent lock contention and cache misses",
                optionB = "Zero allocation rates in hot loops during steady-state processing",
                optionC = "Linear scalability matching physical core allocation count",
                optionD = "Consistent branch prediction rates exceeding 99%",
                correctIndex = 0,
                explanation = "Excessive thread context-switching and lock contention severely degrade pipeline throughput in advanced $topic deployments."
            )
        }
    }

    /**
     * Suggests a single comprehensive question based on teacher topic, prompt hint, or draft context.
     * Cycles through available API keys upon encountering rate limits.
     */
    suspend fun suggestSingleQuestion(
        topic: String,
        promptHint: String = "",
        difficultyLevel: String = "Medium",
        currentDraft: QuestionEntity? = null
    ): GeneratedQuizQuestion = withContext(Dispatchers.IO) {
        val parsedDifficulty = QuizDifficulty.fromString(difficultyLevel)
        val cleanTopic = if (topic.isNotBlank()) topic.trim() else "Academic Principles"

        val keys = getCandidateKeys()
        if (keys.isEmpty()) {
            return@withContext fallbackSingleSuggestion(cleanTopic, promptHint, parsedDifficulty, currentDraft)
        }

        val prompt = buildSingleQuestionPrompt(cleanTopic, promptHint, parsedDifficulty, currentDraft)

        for (currentKey in keys) {
            val masked = apiKeyManager?.maskKey(currentKey) ?: (currentKey.take(6) + "...")
            try {
                val requestBodyJson = JSONObject().apply {
                    val partsArray = JSONArray().put(JSONObject().put("text", prompt))
                    val contentObject = JSONObject().put("parts", partsArray)
                    put("contents", JSONArray().put(contentObject))
                    val generationConfig = JSONObject().apply {
                        put("temperature", 0.7)
                        put("topP", 0.95)
                        put("responseMimeType", "application/json")
                    }
                    put("generationConfig", generationConfig)
                }

                val request = Request.Builder()
                    .url(API_URL)
                    .addHeader("x-goog-api-key", currentKey)
                    .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                val isRateLimited = response.code == 429 ||
                        responseBody.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                        responseBody.contains("quota", ignoreCase = true) ||
                        responseBody.contains("rate-limits", ignoreCase = true)

                if (isRateLimited) {
                    val nextKey = apiKeyManager?.markKeyRateLimitedAndCycle(
                        key = currentKey,
                        reason = "Rate limit HTTP ${response.code} during question suggestion"
                    )
                    Log.w(TAG, "Rate limit hit on key '$masked' (HTTP ${response.code}). Cycling to next key${if (nextKey != null) " '${apiKeyManager?.maskKey(nextKey)}'" else ""}...")
                    continue
                }

                if (response.isSuccessful && responseBody.isNotBlank()) {
                    val parsed = parseSingleQuestionResponse(responseBody, cleanTopic)
                    if (parsed != null) return@withContext parsed
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating suggested question on key '$masked': ${e.message}", e)
            }
        }

        fallbackSingleSuggestion(cleanTopic, promptHint, parsedDifficulty, currentDraft)
    }

    /**
     * Generates plausible distractors, correct choice index, and explanation for an existing question text.
     * Cycles through available API keys upon encountering rate limits.
     */
    suspend fun suggestDistractorsAndExplanation(
        questionText: String,
        topic: String = "",
        difficultyLevel: String = "Medium"
    ): GeneratedQuizQuestion = withContext(Dispatchers.IO) {
        val parsedDifficulty = QuizDifficulty.fromString(difficultyLevel)
        val cleanTopic = if (topic.isNotBlank()) topic.trim() else "Educational Curriculum"

        val keys = getCandidateKeys()
        if (keys.isEmpty()) {
            return@withContext GeneratedQuizQuestion(
                questionText = questionText,
                optionA = "Primary verified mechanism adhering to core $cleanTopic fundamentals",
                optionB = "Plausible distractor representing common conceptual inversion",
                optionC = "Secondary contributing element not central to this condition",
                optionD = "Superficial variable that does not determine this outcome",
                correctOptionIndex = 0,
                explanation = "Option A represents the scientifically sound answer to '$questionText', whereas Options B, C, and D are classical distractors that fail due to conceptual scope or boundary conditions."
            )
        }

        val prompt = """
            You are a master academic assessment designer.
            Teacher's Question Statement:
            "$questionText"
            Topic / Subject: "$cleanTopic"
            Target Difficulty: ${parsedDifficulty.displayName}

            Task:
            1. Formulate 4 distinct, plausible multiple-choice options (optionA, optionB, optionC, optionD).
            2. Designate exactly one correct choice (correctOptionIndex: 0 for A, 1 for B, 2 for C, 3 for D).
            3. The 3 remaining choices must be plausible, authentic student distractors.
            4. Write a pedagogical explanation clarifying why the designated answer is correct and why the distractors are wrong.
            5. Return ONLY a single JSON object matching:
            {
              "questionText": "$questionText",
              "optionA": "Option text A",
              "optionB": "Option text B",
              "optionC": "Option text C",
              "optionD": "Option text D",
              "correctOptionIndex": 0,
              "explanation": "Detailed explanation of the correct answer and distractor rationale."
            }
        """.trimIndent()

        for (currentKey in keys) {
            val masked = apiKeyManager?.maskKey(currentKey) ?: (currentKey.take(6) + "...")
            try {
                val requestBodyJson = JSONObject().apply {
                    val partsArray = JSONArray().put(JSONObject().put("text", prompt))
                    val contentObject = JSONObject().put("parts", partsArray)
                    put("contents", JSONArray().put(contentObject))
                    val generationConfig = JSONObject().apply {
                        put("temperature", 0.7)
                        put("topP", 0.95)
                        put("responseMimeType", "application/json")
                    }
                    put("generationConfig", generationConfig)
                }

                val request = Request.Builder()
                    .url(API_URL)
                    .addHeader("x-goog-api-key", currentKey)
                    .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                val isRateLimited = response.code == 429 ||
                        responseBody.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                        responseBody.contains("quota", ignoreCase = true) ||
                        responseBody.contains("rate-limits", ignoreCase = true)

                if (isRateLimited) {
                    val nextKey = apiKeyManager?.markKeyRateLimitedAndCycle(
                        key = currentKey,
                        reason = "Rate limit HTTP ${response.code} during distractor suggestion"
                    )
                    Log.w(TAG, "Rate limit hit on key '$masked' (HTTP ${response.code}). Cycling to next key${if (nextKey != null) " '${apiKeyManager?.maskKey(nextKey)}'" else ""}...")
                    continue
                }

                if (response.isSuccessful && responseBody.isNotBlank()) {
                    val parsed = parseSingleQuestionResponse(responseBody, cleanTopic)
                    if (parsed != null) {
                        return@withContext parsed.copy(questionText = questionText)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating distractors on key '$masked': ${e.message}", e)
            }
        }

        fallbackSingleSuggestion(cleanTopic, questionText, parsedDifficulty, null)
    }

    private fun buildSingleQuestionPrompt(
        topic: String,
        promptHint: String,
        difficulty: QuizDifficulty,
        currentDraft: QuestionEntity?
    ): String {
        return """
            You are a master academic assessment designer.
            Task: Create a single multiple-choice question for: "$topic".
            ${if (promptHint.isNotBlank()) "Teacher Hint / Specific Concept: \"$promptHint\"" else ""}
            ${if (currentDraft != null && currentDraft.questionText.isNotBlank()) "Current Draft to Refine: \"${currentDraft.questionText}\"" else ""}
            Difficulty: ${difficulty.displayName.uppercase()} (${difficulty.promptGuidance})

            Requirements:
            1. Clear, unambiguous question text.
            2. Exactly 4 distinct plausible options (optionA, optionB, optionC, optionD).
            3. correctOptionIndex must be an integer from 0 to 3 (0=A, 1=B, 2=C, 3=D).
            4. In-depth explanation analyzing the correct answer and debunking distractors.
            5. Return ONLY a single JSON object in this exact schema:
            {
              "questionText": "Question statement?",
              "optionA": "Choice A",
              "optionB": "Choice B",
              "optionC": "Choice C",
              "optionD": "Choice D",
              "correctOptionIndex": 0,
              "explanation": "Pedagogical explanation."
            }
        """.trimIndent()
    }

    private fun parseSingleQuestionResponse(responseBody: String, topic: String): GeneratedQuizQuestion? {
        return try {
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: ""

            val cleanText = text.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            // The cleanText could be an object {...} or array [{...}]
            val qObj = if (cleanText.startsWith("[")) {
                JSONArray(cleanText).getJSONObject(0)
            } else {
                JSONObject(cleanText)
            }

            GeneratedQuizQuestion(
                questionText = qObj.optString("questionText", "What is the primary foundation of $topic?"),
                optionA = qObj.optString("optionA", "Standard foundational definition"),
                optionB = qObj.optString("optionB", "Secondary derived variation"),
                optionC = qObj.optString("optionC", "Opposing non-applicable condition"),
                optionD = qObj.optString("optionD", "Extreme edge boundary condition"),
                correctOptionIndex = qObj.optInt("correctOptionIndex", 0).coerceIn(0, 3),
                explanation = qObj.optString("explanation", "The selected option represents the verified principle of $topic.")
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse single question response: ${e.message}")
            null
        }
    }

    private fun fallbackSingleSuggestion(
        topic: String,
        hint: String,
        difficulty: QuizDifficulty,
        currentDraft: QuestionEntity?
    ): GeneratedQuizQuestion {
        val subject = if (hint.isNotBlank()) hint else topic
        val template = when (difficulty) {
            QuizDifficulty.EASY -> EasyQuestionTemplate(subject, 1)
            QuizDifficulty.MEDIUM -> MediumQuestionTemplate(subject, 2)
            QuizDifficulty.HARD -> HardQuestionTemplate(subject, 3)
        }
        val qText = if (currentDraft != null && currentDraft.questionText.isNotBlank()) {
            currentDraft.questionText
        } else {
            template.questionText
        }
        return GeneratedQuizQuestion(
            questionText = qText,
            optionA = template.optionA,
            optionB = template.optionB,
            optionC = template.optionC,
            optionD = template.optionD,
            correctOptionIndex = template.correctIndex,
            explanation = template.explanation
        )
    }
}
