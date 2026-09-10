package com.example

import com.example.data.gemini.GeminiApiKeyValidator
import com.example.data.gemini.GeminiKeyValidationResult
import org.junit.Assert.*
import org.junit.Test
import java.util.Properties

class GeminiApiKeyBuildCheckTest {

    @Test
    fun testMissingKeyFailsValidation() {
        val props = Properties()
        val result = GeminiApiKeyValidator.validateProperties(props)
        assertTrue("Missing key should fail validation", result is GeminiKeyValidationResult.Missing)
        assertFalse(result.isValid)
    }

    @Test
    fun testEmptyKeyFailsValidation() {
        val props = Properties().apply {
            setProperty("GEMINI_API_KEY", "   ")
        }
        val result = GeminiApiKeyValidator.validateProperties(props)
        assertTrue("Empty key should fail validation", result is GeminiKeyValidationResult.Missing)
        assertFalse(result.isValid)
    }

    @Test
    fun testDefaultPlaceholderFailsValidation() {
        val placeholders = listOf(
            "MY_GEMINI_API_KEY",
            "YOUR_API_KEY",
            "YOUR_API_KEY_HERE",
            "YOUR_GEMINI_API_KEY",
            "PLACEHOLDER",
            "TODO",
            "your_api_key"
        )

        for (ph in placeholders) {
            val props = Properties().apply {
                setProperty("GEMINI_API_KEY", ph)
            }
            val result = GeminiApiKeyValidator.validateProperties(props)
            assertTrue("Placeholder '$ph' should fail validation", result is GeminiKeyValidationResult.Placeholder)
            assertFalse("Placeholder '$ph' should not be valid", result.isValid)
        }
    }

    @Test
    fun testValidKeyPassesValidation() {
        val props = Properties().apply {
            setProperty("GEMINI_API_KEY", "AIzaSyD-valid-test-key-9988776655")
        }
        val result = GeminiApiKeyValidator.validateProperties(props)
        assertTrue("Valid key should pass validation", result is GeminiKeyValidationResult.Valid)
        assertTrue(result.isValid)
    }
}
