package com.example.data.gemini

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/**
 * Manages multiple Gemini API keys and model rotation fallback sequences.
 * Handles rate limit detection (HTTP 429 / RESOURCE_EXHAUSTED) by cooling down
 * exhausted models/keys, seamlessly cycling to alternate keys, and syncing with Firebase.
 */
class GeminiApiKeyManager(private val context: Context? = null) {

    companion object {
        private const val TAG = "GeminiApiKeyManager"
        private const val PREFS_NAME = "gemini_api_key_prefs"
        private const val KEY_STORED_KEYS = "stored_api_keys"
        private const val KEY_ACTIVE_KEY_INDEX = "active_key_index"
        private const val KEY_AUTO_ROTATE = "auto_rotate_keys"

        // Ordered model fallback hierarchy for high availability
        val DEFAULT_MODEL_CHAIN = listOf(
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-1.5-flash-8b",
            "gemini-1.5-pro"
        )

        @Volatile
        private var instance: GeminiApiKeyManager? = null

        fun getInstance(context: Context): GeminiApiKeyManager {
            return instance ?: synchronized(this) {
                instance ?: GeminiApiKeyManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences? = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Cooldown map: key/model -> timestamp until which it is considered rate-limited
    private val modelCooldowns = mutableMapOf<String, Long>()
    private val keyCooldowns = mutableMapOf<String, Long>()

    private val _configuredKeys = MutableStateFlow<List<String>>(emptyList())
    val configuredKeys: StateFlow<List<String>> = _configuredKeys.asStateFlow()

    private val _activeKeyIndex = MutableStateFlow(0)
    val activeKeyIndex: StateFlow<Int> = _activeKeyIndex.asStateFlow()

    private val _activeModelName = MutableStateFlow(DEFAULT_MODEL_CHAIN.first())
    val activeModelName: StateFlow<String> = _activeModelName.asStateFlow()

    private val _rateLimitEventMessage = MutableStateFlow<String?>(null)
    val rateLimitEventMessage: StateFlow<String?> = _rateLimitEventMessage.asStateFlow()

    init {
        loadKeysFromStorage()
    }

    private fun loadKeysFromStorage() {
        val jsonString = prefs?.getString(KEY_STORED_KEYS, null)
        val loaded = mutableListOf<String>()

        if (!jsonString.isNullOrBlank()) {
            try {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val key = jsonArray.optString(i).trim()
                    if (key.isNotBlank() && !loaded.contains(key)) {
                        loaded.add(key)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed reading stored keys: ${e.message}")
            }
        }

        // Check BuildConfig default if list is empty or doesn't contain it
        try {
            val buildConfigKey = BuildConfig.GEMINI_API_KEY
            if (!buildConfigKey.isNullOrBlank() &&
                buildConfigKey != "MY_GEMINI_API_KEY" &&
                !loaded.contains(buildConfigKey)
            ) {
                loaded.add(0, buildConfigKey)
            }
        } catch (_: Exception) {}

        _configuredKeys.value = loaded
        val savedIndex = prefs?.getInt(KEY_ACTIVE_KEY_INDEX, 0) ?: 0
        _activeKeyIndex.value = if (savedIndex in loaded.indices) savedIndex else 0
    }

    private fun persistKeys(keys: List<String>) {
        try {
            val jsonArray = JSONArray()
            keys.forEach { jsonArray.put(it) }
            prefs?.edit()
                ?.putString(KEY_STORED_KEYS, jsonArray.toString())
                ?.putInt(KEY_ACTIVE_KEY_INDEX, _activeKeyIndex.value)
                ?.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error persisting keys: ${e.message}")
        }
    }

    /**
     * Clears all stored keys in-memory and in SharedPreferences.
     */
    fun clearAllKeys() {
        _configuredKeys.value = emptyList()
        _activeKeyIndex.value = 0
        prefs?.edit()?.remove(KEY_STORED_KEYS)?.remove(KEY_ACTIVE_KEY_INDEX)?.apply()
    }

    /**
     * Adds one or multiple API keys. Keys can be separated by commas, spaces, or newlines.
     */
    fun addApiKeys(rawInput: String): Int {
        val candidates = rawInput.split(',', '\n', ' ', ';')
            .map { it.trim() }
            .filter { it.length > 10 } // typical Gemini keys are ~39 chars starting with AIza

        val currentList = _configuredKeys.value.toMutableList()
        var addedCount = 0

        for (candidate in candidates) {
            if (!currentList.contains(candidate)) {
                currentList.add(candidate)
                addedCount++
            }
        }

        if (addedCount > 0) {
            _configuredKeys.value = currentList
            persistKeys(currentList)
            Log.d(TAG, "Added $addedCount API keys. Total configured: ${currentList.size}")
        }
        return addedCount
    }

    /**
     * Removes an API key by index.
     */
    fun removeApiKey(index: Int) {
        val currentList = _configuredKeys.value.toMutableList()
        if (index in currentList.indices) {
            val removed = currentList.removeAt(index)
            keyCooldowns.remove(removed)
            _configuredKeys.value = currentList
            if (_activeKeyIndex.value >= currentList.size) {
                _activeKeyIndex.value = (currentList.size - 1).coerceAtLeast(0)
            }
            persistKeys(currentList)
        }
    }

    /**
     * Sets the preferred primary key.
     */
    fun setActiveKeyIndex(index: Int) {
        if (index in _configuredKeys.value.indices) {
            _activeKeyIndex.value = index
            prefs?.edit()?.putInt(KEY_ACTIVE_KEY_INDEX, index)?.apply()
        }
    }

    /**
     * Retrieves the primary or currently active API key, or empty string if none configured.
     */
    fun getActiveApiKey(): String {
        val keys = _configuredKeys.value
        val index = _activeKeyIndex.value
        return if (index in keys.indices) keys[index] else keys.firstOrNull() ?: ""
    }

    /**
     * Returns all configured keys with active keys prioritized.
     */
    fun getAllApiKeys(): List<String> {
        val list = _configuredKeys.value
        if (list.isEmpty()) return emptyList()
        val activeIdx = _activeKeyIndex.value.coerceIn(0, list.lastIndex)
        val activeKey = list[activeIdx]
        val otherKeys = list.filterIndexed { i, _ -> i != activeIdx }
        return listOf(activeKey) + otherKeys
    }

    /**
     * Marks a model as rate-limited with an automatic cooldown period.
     */
    fun markModelRateLimited(model: String, cooldownDurationMs: Long = 120_000L) {
        val until = System.currentTimeMillis() + cooldownDurationMs
        modelCooldowns[model] = until
        val msg = "Model '$model' reached rate limit. Cooling down for ${cooldownDurationMs / 1000}s..."
        _rateLimitEventMessage.value = msg
        Log.w(TAG, msg)
    }

    /**
     * Marks an API key as rate-limited with cooldown.
     */
    fun markKeyRateLimited(key: String, cooldownDurationMs: Long = 120_000L) {
        val until = System.currentTimeMillis() + cooldownDurationMs
        keyCooldowns[key] = until
        Log.w(TAG, "API Key '${maskKey(key)}' rate-limited for ${cooldownDurationMs / 1000}s.")
    }

    fun isModelRateLimited(model: String): Boolean {
        val expiry = modelCooldowns[model] ?: return false
        return if (System.currentTimeMillis() < expiry) {
            true
        } else {
            modelCooldowns.remove(model)
            false
        }
    }

    fun isKeyRateLimited(key: String): Boolean {
        val expiry = keyCooldowns[key] ?: return false
        return if (System.currentTimeMillis() < expiry) {
            true
        } else {
            keyCooldowns.remove(key)
            false
        }
    }

    /**
     * Returns the ordered list of models for question generation.
     * Ready models appear first, followed by cooling-down models as last resort.
     */
    fun getAvailableModels(): List<String> {
        val (cooling, ready) = DEFAULT_MODEL_CHAIN.partition { isModelRateLimited(it) }
        return ready + cooling
    }

    /**
     * Returns the available API keys with non-rate-limited keys prioritized.
     */
    fun getAvailableApiKeys(): List<String> {
        val all = getAllApiKeys()
        if (all.isEmpty()) return emptyList()
        val (cooling, ready) = all.partition { isKeyRateLimited(it) }
        return ready + cooling
    }

    fun clearCooldowns() {
        modelCooldowns.clear()
        keyCooldowns.clear()
        _rateLimitEventMessage.value = null
    }

    /**
     * Synchronizes keys from Firebase Cloud Firestore into local memory and SharedPreferences.
     */
    fun syncFromCloud(remoteKeys: List<String>, activeIndex: Int = 0) {
        if (remoteKeys.isEmpty()) return
        val currentKeys = _configuredKeys.value
        // Only update if there are meaningful changes
        if (currentKeys != remoteKeys || _activeKeyIndex.value != activeIndex) {
            _configuredKeys.value = remoteKeys
            _activeKeyIndex.value = activeIndex.coerceIn(0, remoteKeys.lastIndex)
            persistKeys(remoteKeys)
            // Prune cooldowns for removed keys
            val remoteSet = remoteKeys.toSet()
            keyCooldowns.keys.retainAll(remoteSet)
            Log.d(TAG, "Synchronized ${remoteKeys.size} keys from Firebase Firestore.")
        }
    }

    /**
     * Cycles to the next available API key in the pool, avoiding currently rate-limited keys if possible.
     * Updates activeKeyIndex, persists to storage, and records event message.
     * Returns the newly activated key, or null if no keys configured.
     */
    fun cycleToNextKey(reason: String = "Rate limit encountered"): String? {
        val keys = _configuredKeys.value
        if (keys.isEmpty()) return null
        if (keys.size == 1) {
            _rateLimitEventMessage.value = "Only 1 key in pool (${maskKey(keys[0])}). $reason"
            return keys[0]
        }

        val currentIndex = _activeKeyIndex.value
        val previousKey = keys.getOrNull(currentIndex) ?: keys.first()

        // Find next key that is not rate-limited, searching sequentially starting from next index
        var candidateIndex = -1
        for (step in 1 until keys.size) {
            val idx = (currentIndex + step) % keys.size
            if (!isKeyRateLimited(keys[idx])) {
                candidateIndex = idx
                break
            }
        }

        // If all keys are rate-limited, simply advance to the next index in round-robin order
        val nextIndex = if (candidateIndex != -1) candidateIndex else (currentIndex + 1) % keys.size
        val newKey = keys[nextIndex]

        setActiveKeyIndex(nextIndex)

        val cycleMsg = "Rotated active key: ${maskKey(previousKey)} ➔ ${maskKey(newKey)} (${reason})"
        _rateLimitEventMessage.value = cycleMsg
        Log.w(TAG, cycleMsg)

        return newKey
    }

    /**
     * Marks a specific API key as rate-limited with cooldown, and immediately cycles to the next key.
     * Returns the newly active key.
     */
    fun markKeyRateLimitedAndCycle(
        key: String,
        cooldownDurationMs: Long = 120_000L,
        reason: String = "Rate limit HTTP 429"
    ): String? {
        markKeyRateLimited(key, cooldownDurationMs)
        return cycleToNextKey(reason = reason)
    }

    /**
     * Tests a single Gemini API key with a lightweight ping to verify validity and measure latency.
     */
    suspend fun testSingleKey(apiKey: String): Result<Long> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val testClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            // Call models endpoint with the key
            val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
            val request = Request.Builder().url(url).get().build()
            val response = testClient.newCall(request).execute()

            val latency = System.currentTimeMillis() - startTime
            if (response.isSuccessful) {
                Result.success(latency)
            } else {
                val body = response.body?.string() ?: ""
                val err = when (response.code) {
                    429 -> "HTTP 429 (Rate Limit / Quota Exceeded)"
                    400 -> "HTTP 400 (Invalid Key Format / Bad Request)"
                    403 -> "HTTP 403 (Forbidden / API Not Enabled)"
                    else -> "HTTP ${response.code}: ${body.take(60)}"
                }
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Helper to mask an API key for safe display in UI (e.g. AIzaSyB...82Zq)
     */
    fun maskKey(key: String): String {
        if (key.length <= 8) return "••••••••"
        return key.take(7) + "..." + key.takeLast(4)
    }
}
