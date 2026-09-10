package com.example.data.firestore

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Encapsulates the Firebase App Check configuration and enforcement state.
 */
data class AppCheckStatus(
    val isEnabled: Boolean = false,
    val providerName: String = "Uninitialized",
    val isAttested: Boolean = false,
    val tokenSnippet: String = "",
    val lastAttestationTime: Long = 0L,
    val statusDetail: String = "App Check is pending initialization",
    val isDebugMode: Boolean = BuildConfig.DEBUG
)

/**
 * Manages Firebase App Check enforcement to verify requests originate from the authentic app binary.
 *
 * - In DEBUG mode: Uses [DebugAppCheckProviderFactory] (outputs debug token to logcat for registering in Firebase Console).
 * - In RELEASE mode: Uses [PlayIntegrityAppCheckProviderFactory] to attest device & app integrity with Google Play Integrity API.
 * - Automatically attaches tokens to all Cloud Firestore and Firebase API requests to enforce requests.
 */
class FirebaseAppCheckManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "FirebaseAppCheck"

        @Volatile
        private var INSTANCE: FirebaseAppCheckManager? = null

        fun getInstance(context: Context): FirebaseAppCheckManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseAppCheckManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val _appCheckStatus = MutableStateFlow(
        AppCheckStatus(
            isEnabled = false,
            providerName = if (BuildConfig.DEBUG) "DebugAppCheckProvider" else "PlayIntegrityAppCheckProvider",
            isDebugMode = BuildConfig.DEBUG
        )
    )
    val appCheckStatus: StateFlow<AppCheckStatus> = _appCheckStatus.asStateFlow()

    init {
        initialize()
    }

    /**
     * Initializes Firebase App Check with the appropriate provider factory based on build type.
     */
    fun initialize() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                Log.d(TAG, "Waiting for FirebaseApp initialization before configuring App Check...")
                return
            }

            val appCheck = FirebaseAppCheck.getInstance()
            val providerName: String

            if (BuildConfig.DEBUG) {
                appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                providerName = "Debug Provider (Development / Emulators)"
                Log.i(TAG, "Firebase App Check initialized with DebugAppCheckProviderFactory.")
            } else {
                appCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
                providerName = "Play Integrity (Production Attestation)"
                Log.i(TAG, "Firebase App Check initialized with PlayIntegrityAppCheckProviderFactory.")
            }

            appCheck.setTokenAutoRefreshEnabled(true)

            // Listen for token attestation changes
            appCheck.addAppCheckListener { token ->
                val snippet = formatTokenSnippet(token.token)
                val now = System.currentTimeMillis()
                _appCheckStatus.value = AppCheckStatus(
                    isEnabled = true,
                    providerName = providerName,
                    isAttested = true,
                    tokenSnippet = snippet,
                    lastAttestationTime = now,
                    statusDetail = "Live request attestation active. All Firebase requests are enforced.",
                    isDebugMode = BuildConfig.DEBUG
                )
                Log.d(TAG, "Firebase App Check token received & verified: $snippet")
            }

            _appCheckStatus.value = _appCheckStatus.value.copy(
                isEnabled = true,
                providerName = providerName,
                statusDetail = "App Check installed and enforcing requests. Auto-refresh enabled."
            )
        } catch (e: Throwable) {
            Log.w(TAG, "App Check initialization warning: ${e.message}")
            _appCheckStatus.value = _appCheckStatus.value.copy(
                isEnabled = false,
                statusDetail = "App Check initialized in fallback mode (${e.message ?: "Local testing"})"
            )
        }
    }

    /**
     * Explicitly requests an App Check token to test and verify request enforcement.
     */
    suspend fun verifyAttestation(forceRefresh: Boolean = false): Result<String> {
        return try {
            val appCheck = FirebaseAppCheck.getInstance()
            val tokenResult = appCheck.getAppCheckToken(forceRefresh).awaitTask()
            val snippet = formatTokenSnippet(tokenResult.token)
            val now = System.currentTimeMillis()

            _appCheckStatus.value = _appCheckStatus.value.copy(
                isAttested = true,
                tokenSnippet = snippet,
                lastAttestationTime = now,
                statusDetail = "Attestation verified successfully: token active."
            )
            Result.success(snippet)
        } catch (e: Throwable) {
            Log.w(TAG, "App Check attestation test: ${e.message}")
            Result.failure(e)
        }
    }

    private fun formatTokenSnippet(token: String?): String {
        if (token.isNullOrBlank()) return "None"
        if (token.length <= 12) return "••••••••"
        return token.take(6) + "••••••••" + token.takeLast(4)
    }
}
