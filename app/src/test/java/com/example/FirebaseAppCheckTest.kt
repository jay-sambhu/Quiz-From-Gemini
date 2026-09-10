package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.firestore.FirebaseAppCheckManager
import com.example.data.gemini.GeminiApiKeyManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FirebaseAppCheckTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testAppCheckManagerInitialization() {
        val appCheckManager = FirebaseAppCheckManager.getInstance(context)
        assertNotNull(appCheckManager)

        val status = appCheckManager.appCheckStatus.value
        assertNotNull(status.providerName)
        assertTrue(status.providerName.contains("Provider"))
    }

    @Test
    fun testGeminiApiKeyEnvVariableStrategy() {
        val keyManager = GeminiApiKeyManager.getInstance(context)
        val envStatus = keyManager.envSecretStatus.value

        assertNotNull(envStatus)
        assertTrue(envStatus.isGitIgnored)
        assertTrue(envStatus.envSource.contains("Secrets Gradle Plugin"))
        assertTrue(envStatus.securityNotice.contains(".gitignore"))
    }
}
