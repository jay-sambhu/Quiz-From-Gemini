package com.example.data.avatar

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.data.gemini.GeminiApiKeyManager
import com.example.data.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Service that orchestrates AI profile avatar generation for Students and Teachers.
 * Integrates with Google Gemini API with fallback to high-resolution procedural generation.
 */
class AiAvatarGeneratorService(
    private val apiKeyManager: GeminiApiKeyManager? = null
) {
    companion object {
        private const val TAG = "AiAvatarGenService"

        private val httpClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .build()

        @Volatile
        private var instance: AiAvatarGeneratorService? = null

        fun getInstance(apiKeyManager: GeminiApiKeyManager? = null): AiAvatarGeneratorService {
            return instance ?: synchronized(this) {
                instance ?: AiAvatarGeneratorService(apiKeyManager).also { instance = it }
            }
        }
    }

    /**
     * Generates a profile avatar based on prompt, style, and user role.
     * Returns the persistent local file URI string pointing to the saved image.
     */
    suspend fun generateAvatar(
        context: Context,
        prompt: String,
        style: String,
        role: UserRole,
        onStatusUpdate: ((String) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val effectivePrompt = prompt.ifBlank {
            if (role == UserRole.TEACHER) "Distinguished professor of science" else "Enthusiastic smart student"
        }

        val candidateKeys = apiKeyManager?.getAllApiKeys()?.filter { it.isNotBlank() } ?: emptyList()

        if (candidateKeys.isNotEmpty()) {
            onStatusUpdate?.invoke("Synthesizing character design with Gemini AI...")
            val imageModels = listOf("gemini-2.5-flash-image", "gemini-3.1-flash-image-preview")

            for (model in imageModels) {
                for (key in candidateKeys) {
                    try {
                        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

                        val fullPrompt = "Generate a centered square 1:1 circular profile avatar of $effectivePrompt in $style style for a ${role.name.lowercase()} user. Clean composition, vibrant lighting, character portrait."

                        val requestJson = JSONObject().apply {
                            val parts = JSONArray().put(JSONObject().put("text", fullPrompt))
                            val content = JSONObject().put("parts", parts)
                            put("contents", JSONArray().put(content))
                        }

                        val request = Request.Builder()
                            .url(endpoint)
                            .addHeader("x-goog-api-key", key)
                            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                            .build()

                        val response = httpClient.newCall(request).execute()
                        val responseBody = response.body?.string() ?: ""

                        if (response.code == 429 || responseBody.contains("RESOURCE_EXHAUSTED", ignoreCase = true)) {
                            apiKeyManager?.markKeyRateLimited(key, 120_000L)
                            continue
                        }

                        if (response.isSuccessful && responseBody.isNotBlank()) {
                            val rootJson = JSONObject(responseBody)
                            val candidates = rootJson.optJSONArray("candidates")
                            if (candidates != null && candidates.length() > 0) {
                                val content = candidates.getJSONObject(0).optJSONObject("content")
                                val partsArray = content?.optJSONArray("parts")
                                if (partsArray != null) {
                                    for (i in 0 until partsArray.length()) {
                                        val part = partsArray.getJSONObject(i)
                                        val inlineData = part.optJSONObject("inlineData")
                                        if (inlineData != null) {
                                            val base64Data = inlineData.optString("data")
                                            if (base64Data.isNotBlank()) {
                                                val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                                                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                                if (bitmap != null) {
                                                    val savedUri = ProceduralAvatarGenerator.saveAvatarToFile(
                                                        context,
                                                        bitmap,
                                                        "ai_avatar_gemini"
                                                    )
                                                    Log.d(TAG, "Successfully generated Gemini AI avatar: $savedUri")
                                                    return@withContext Result.success(savedUri)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Gemini image request error on $model: ${e.message}")
                    }
                }
            }
        }

        // Procedural AI generator fallback (guaranteed instant success & zero crash)
        onStatusUpdate?.invoke("Rendering high-resolution stylized avatar...")
        try {
            val bitmap = ProceduralAvatarGenerator.generateAvatarBitmap(
                prompt = effectivePrompt,
                style = style,
                role = role
            )
            val uri = ProceduralAvatarGenerator.saveAvatarToFile(
                context,
                bitmap,
                "ai_avatar_procedural"
            )
            Log.d(TAG, "Successfully rendered procedural avatar: $uri")
            Result.success(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed generating avatar: ${e.message}", e)
            Result.failure(e)
        }
    }
}
