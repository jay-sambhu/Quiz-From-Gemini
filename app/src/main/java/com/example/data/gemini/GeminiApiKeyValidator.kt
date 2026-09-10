package com.example.data.gemini

import java.util.Properties

sealed class GeminiKeyValidationResult {
    data object Valid : GeminiKeyValidationResult()
    data class Missing(val message: String) : GeminiKeyValidationResult()
    data class Placeholder(val key: String, val message: String) : GeminiKeyValidationResult()

    val isValid: Boolean get() = this is Valid
}

object GeminiApiKeyValidator {

    val DEFAULT_PLACEHOLDERS = setOf(
        "",
        "MY_GEMINI_API_KEY",
        "YOUR_API_KEY",
        "YOUR_API_KEY_HERE",
        "YOUR_GEMINI_API_KEY",
        "PLACEHOLDER",
        "TODO",
        "CHANGE_ME",
        "DEFAULT",
        "your_api_key",
        "your_gemini_api_key"
    )

    /**
     * Validates an individual key string to verify it is not null, blank,
     * or a default placeholder.
     */
    fun validateKey(rawKey: String?): GeminiKeyValidationResult {
        val trimmed = rawKey?.trim()
        if (trimmed.isNullOrEmpty()) {
            return GeminiKeyValidationResult.Missing("GEMINI_API_KEY is missing or empty.")
        }

        if (DEFAULT_PLACEHOLDERS.contains(trimmed) ||
            trimmed.contains("YOUR_API_KEY", ignoreCase = true) ||
            trimmed.contains("MY_GEMINI_API_KEY", ignoreCase = true) ||
            trimmed.equals("PLACEHOLDER", ignoreCase = true)
        ) {
            return GeminiKeyValidationResult.Placeholder(
                key = trimmed,
                message = "GEMINI_API_KEY cannot be default placeholder ('$trimmed')."
            )
        }

        return GeminiKeyValidationResult.Valid
    }

    /**
     * Validates a Java Properties object (such as loaded from local.properties)
     * to ensure GEMINI_API_KEY is defined and valid.
     */
    fun validateProperties(properties: Properties): GeminiKeyValidationResult {
        val key = properties.getProperty("GEMINI_API_KEY")
        return validateKey(key)
    }
}
