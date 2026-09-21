import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Properties
import java.io.FileInputStream

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.quizapp.qzkxmp"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to load from local.properties if present, or fallback to .env
secrets {
  propertiesFileName = if (rootProject.file("local.properties").exists()) "local.properties" else ".env"
  defaultPropertiesFileName = if (rootProject.file("local.defaults.properties").exists()) "local.defaults.properties" else ".env.example"
}

/**
 * Build-time check task: Verifies that GEMINI_API_KEY is properly loaded from local.properties
 * and fails the build if the key is missing or is the default placeholder.
 * Uses typed properties for full Gradle Configuration Cache compatibility.
 */
abstract class VerifyGeminiApiKeyTask : DefaultTask() {
  @get:Internal
  abstract val localPropertiesFile: RegularFileProperty

  @get:Internal
  abstract val aiStudioAgent: Property<Boolean>

  @get:Internal
  abstract val explicitTask: Property<Boolean>

  @TaskAction
  fun verify() {
    val localPropsFile = localPropertiesFile.orNull?.asFile
    val isAiStudio = aiStudioAgent.getOrElse(false)
    val isExplicit = explicitTask.getOrElse(false)

    if (localPropsFile == null || !localPropsFile.exists()) {
      if (isExplicit || !isAiStudio) {
        throw GradleException(
          "BUILD FAILED: 'local.properties' file not found at ${localPropsFile?.absolutePath ?: "project root"}.\n" +
          "Please create 'local.properties' and configure GEMINI_API_KEY=<your_api_key>."
        )
      } else {
        logger.lifecycle("AI Studio container environment: 'local.properties' not found; using Secrets panel / environment credentials.")
        return
      }
    }

    val properties = Properties().apply {
      localPropsFile.inputStream().use { load(it) }
    }

    val apiKey = properties.getProperty("GEMINI_API_KEY")?.trim()

    val defaultPlaceholders = setOf(
      "",
      "MY_GEMINI_API_KEY",
      "YOUR_API_KEY",
      "YOUR_API_KEY_HERE",
      "YOUR_GEMINI_API_KEY",
      "PLACEHOLDER",
      "TODO",
      "CHANGE_ME",
      "your_api_key",
      "your_gemini_api_key",
      "DEFAULT"
    )

    if (apiKey.isNullOrEmpty()) {
      throw GradleException(
        "BUILD FAILED: 'GEMINI_API_KEY' is missing or empty in 'local.properties'.\n" +
        "Please provide a valid GEMINI_API_KEY in local.properties."
      )
    }

    if (defaultPlaceholders.contains(apiKey) ||
        apiKey.contains("YOUR_API_KEY", ignoreCase = true) ||
        apiKey.contains("MY_GEMINI_API_KEY", ignoreCase = true) ||
        apiKey.equals("PLACEHOLDER", ignoreCase = true)
    ) {
      throw GradleException(
        "BUILD FAILED: GEMINI_API_KEY in 'local.properties' is set to default placeholder ('$apiKey').\n" +
        "Please replace it with a valid, functional Gemini API key."
      )
    }

    logger.lifecycle("BUILD SUCCESS: GEMINI_API_KEY successfully loaded and verified from local.properties.")
  }
}

val verifyGeminiApiKey = tasks.register<VerifyGeminiApiKeyTask>("verifyGeminiApiKey") {
  group = "verification"
  description = "Verifies that GEMINI_API_KEY is properly loaded from local.properties and fails the build if missing or default placeholder."
  localPropertiesFile.set(rootProject.file("local.properties"))
  aiStudioAgent.set(System.getenv("AI_STUDIO_AGENT") == "1")
  explicitTask.set(
    gradle.startParameter.taskNames.any { name ->
      name.contains("verifyGeminiApiKey", ignoreCase = true) ||
      name.contains("checkGeminiApiKey", ignoreCase = true) ||
      name.contains("validateGeminiApiKey", ignoreCase = true)
    }
  )
}

tasks.register("checkGeminiApiKey") {
  group = "verification"
  description = "Alias for verifyGeminiApiKey"
  dependsOn(verifyGeminiApiKey)
}

tasks.register("validateGeminiApiKey") {
  group = "verification"
  description = "Alias for verifyGeminiApiKey"
  dependsOn(verifyGeminiApiKey)
}

tasks.named("preBuild") {
  dependsOn(verifyGeminiApiKey)
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.compose.charts)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  implementation(libs.firebase.auth)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.firebase.appcheck)
  implementation(libs.firebase.appcheck.playintegrity)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
