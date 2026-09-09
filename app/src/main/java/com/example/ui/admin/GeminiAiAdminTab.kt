package com.example.ui.admin

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.gemini.GeminiApiKeyManager
import com.example.data.model.QuizQuestion
import com.example.ui.QuizViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Admin API Configuration Screen:
 * - Persists and manages multiple Gemini API keys in Firebase Firestore.
 * - Dynamic rate limit detection & automatic key cycling failover engine.
 * - Live key validity ping tests with latency benchmarks.
 * - Interactive AI Question Generator test console with full telemetry.
 */
@Composable
fun GeminiAiAdminTab(viewModel: QuizViewModel) {
    ApiConfigurationScreen(viewModel = viewModel)
}

@Composable
fun ApiConfigurationScreen(viewModel: QuizViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val configuredKeys by viewModel.configuredApiKeys.collectAsState()
    val activeKeyIndex by viewModel.activeApiKeyIndex.collectAsState()
    val rateLimitMessage by viewModel.rateLimitEventMessage.collectAsState()
    val isTesting by viewModel.isTestingAiGeneration.collectAsState()
    val lastReport by viewModel.lastAiGenerationReport.collectAsState()

    // Firebase Cloud Sync State
    val isCloudConnected by viewModel.isCloudConnected.collectAsState()
    val isApiKeySyncedWithCloud by viewModel.isApiKeySyncedWithCloud.collectAsState()
    val cloudApiKeyCount by viewModel.cloudApiKeyCount.collectAsState()
    val lastApiKeyCloudSyncTime by viewModel.lastApiKeyCloudSyncTime.collectAsState()
    val isSyncingKeysWithFirebase by viewModel.isSyncingKeysWithFirebase.collectAsState()

    var keyInputText by remember { mutableStateOf("") }
    var autoSyncToFirebase by remember { mutableStateOf(true) }

    // State for individual key visibility & test results
    val revealedKeys = remember { mutableStateMapOf<Int, Boolean>() }
    val keyTestResults = remember { mutableStateMapOf<String, String>() }
    val keyTestingInProgress = remember { mutableStateMapOf<String, Boolean>() }

    // Test console state
    var testTopic by remember { mutableStateOf("Kotlin Coroutines & Flow") }
    var testDifficulty by remember { mutableStateOf("Medium") }
    var testCount by remember { mutableIntStateOf(3) }
    var testCategory by remember { mutableStateOf("Mobile Development") }
    var testTag by remember { mutableStateOf("#kotlin") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("api_configuration_screen")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 0. Screen Header ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BentoPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = BentoPrimary, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            text = "API Configuration",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cloud Firebase Gemini Pool & Dynamic Rate-Limit Failover Engine",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // --- 1. Firebase Cloud Storage & Key Synchronization Card ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (isApiKeySyncedWithCloud) BentoEmerald.copy(alpha = 0.5f) else BentoPrimary.copy(alpha = 0.3f)
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("firebase_cloud_keys_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isCloudConnected) BentoEmerald.copy(alpha = 0.15f) else BentoAmber.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCloudConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (isCloudConnected) BentoEmerald else BentoAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Firebase Firestore Key Storage",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "system_config / gemini_api_keys",
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Cloud Status Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isApiKeySyncedWithCloud) BentoEmerald.copy(alpha = 0.15f) else BentoPrimary.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isApiKeySyncedWithCloud) BentoEmerald else BentoPrimary)
                                )
                                Text(
                                    text = if (isApiKeySyncedWithCloud) "Firebase Synced" else "Local Ready",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isApiKeySyncedWithCloud) BentoEmerald else BentoPrimary
                                )
                            }
                        }
                    }

                    // Metadata Metrics Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$cloudApiKeyCount",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BentoPrimary
                            )
                            Text(
                                text = "Keys in Cloud",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        VerticalDivider(modifier = Modifier.height(32.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val syncTimeFormatted = remember(lastApiKeyCloudSyncTime) {
                                if (lastApiKeyCloudSyncTime > 0) {
                                    SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(lastApiKeyCloudSyncTime))
                                } else {
                                    "Pending"
                                }
                            }
                            Text(
                                text = syncTimeFormatted,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Last Cloud Sync",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        VerticalDivider(modifier = Modifier.height(32.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isCloudConnected) "Online" else "Offline",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCloudConnected) BentoEmerald else BentoRose
                            )
                            Text(
                                text = "Firestore Link",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Cloud Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveApiKeysToFirebase { success ->
                                    val msg = if (success) "Saved keys to Firebase Firestore!" else "Failed saving to Firebase"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isSyncingKeysWithFirebase,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_keys_to_firebase_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary)
                        ) {
                            if (isSyncingKeysWithFirebase) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Syncing...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Save to Firebase", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.pullApiKeysFromFirebase { success, count ->
                                    val msg = if (success) "Synced $count key(s) from Firebase" else "No keys found in Firebase"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isSyncingKeysWithFirebase,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sync_from_firebase_btn")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sync from Cloud", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // --- 2. Live Rate Limit Event & Auto-Cycling Banner ---
        if (rateLimitMessage != null) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoAmber.copy(alpha = 0.15f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BentoAmber)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rate_limit_alert_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Loop, contentDescription = null, tint = BentoAmber, modifier = Modifier.size(24.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dynamic Key Rotation Active",
                                fontWeight = FontWeight.Bold,
                                color = BentoAmber,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = rateLimitMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        TextButton(
                            onClick = { viewModel.clearRateLimitCooldowns() },
                            modifier = Modifier.testTag("clear_cooldowns_btn")
                        ) {
                            Text("Reset", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- 3. Add Gemini API Keys Card ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_gemini_keys_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(BentoPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = BentoPrimary, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(
                                text = "Add Gemini API Keys",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Add multiple keys for load balancing & instant quota failover",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Key input field (supports single or multiple keys separated by comma, space, or newline)
                    OutlinedTextField(
                        value = keyInputText,
                        onValueChange = { keyInputText = it },
                        label = { Text("Gemini API Key(s)") },
                        placeholder = { Text("Paste AIzaSy... (supports comma, space, or newline separated keys)") },
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_gemini_key_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = false,
                        maxLines = 3
                    )

                    // Auto-sync to Firebase checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Checkbox(
                                checked = autoSyncToFirebase,
                                onCheckedChange = { autoSyncToFirebase = it }
                            )
                            Text(
                                text = "Auto-save to Firebase Cloud",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = {
                                if (keyInputText.isNotBlank()) {
                                    val count = viewModel.addAdminGeminiApiKeys(
                                        rawInput = keyInputText,
                                        syncToFirebase = autoSyncToFirebase
                                    )
                                    keyInputText = ""
                                    Toast.makeText(context, "Added $count key(s) to pool", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = keyInputText.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("admin_save_gemini_key_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add to Pool", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- 4. Configured API Keys Pool (Firebase Managed) ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("configured_keys_pool_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Configured Key Pool",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BentoPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${configuredKeys.size} Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Manual Cycle Key Button
                        if (configuredKeys.size > 1) {
                            OutlinedButton(
                                onClick = {
                                    val cycledKey = viewModel.cycleGeminiApiKeyManually()
                                    if (cycledKey != null) {
                                        Toast.makeText(
                                            context,
                                            "Rotated to: ${viewModel.geminiApiKeyManager.maskKey(cycledKey)}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("cycle_active_key_btn")
                            ) {
                                Icon(Icons.Default.Loop, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Cycle Key", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (configuredKeys.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No API keys configured. Please add keys above or sync from Firebase Cloud.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        configuredKeys.forEachIndexed { index, rawKey ->
                            val isActive = index == activeKeyIndex
                            val isRateLimited = viewModel.geminiApiKeyManager.isKeyRateLimited(rawKey)
                            val isRevealed = revealedKeys[index] ?: false
                            val displayText = if (isRevealed) rawKey else viewModel.geminiApiKeyManager.maskKey(rawKey)
                            val testResult = keyTestResults[rawKey]
                            val isTestingKey = keyTestingInProgress[rawKey] ?: false

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) BentoPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                                ),
                                border = if (isActive) CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(BentoPrimary)
                                ) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("api_key_item_$index")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = if (isActive) BentoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = displayText,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.SemiBold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.padding(top = 2.dp)
                                                ) {
                                                    if (isActive) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = BentoPrimary.copy(alpha = 0.2f)
                                                        ) {
                                                            Text(
                                                                text = "PRIMARY ACTIVE",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = BentoPrimary,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                    if (isApiKeySyncedWithCloud) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = BentoCyan.copy(alpha = 0.2f)
                                                        ) {
                                                            Text(
                                                                text = "FIREBASE SYNCED",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = BentoCyan,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                    if (isRateLimited) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = BentoAmber.copy(alpha = 0.2f)
                                                        ) {
                                                            Text(
                                                                text = "RATE-LIMITED",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = BentoAmber,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Quick icons: Reveal & Copy
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            IconButton(
                                                onClick = { revealedKeys[index] = !isRevealed },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = "Toggle Visibility",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(rawKey))
                                                    Toast.makeText(context, "Copied key to clipboard", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ContentCopy,
                                                    contentDescription = "Copy Key",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    // Key Actions & Test Status Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Ping / Test Key Button
                                        OutlinedButton(
                                            onClick = {
                                                keyTestingInProgress[rawKey] = true
                                                keyTestResults.remove(rawKey)
                                                viewModel.testSingleGeminiApiKey(rawKey) { success, latencyMs, msg ->
                                                    keyTestingInProgress[rawKey] = false
                                                    keyTestResults[rawKey] = msg
                                                }
                                            },
                                            enabled = !isTestingKey,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("test_key_btn_$index")
                                        ) {
                                            if (isTestingKey) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(12.dp),
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text("Testing...", fontSize = 11.sp)
                                            } else {
                                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Ping Key", fontSize = 11.sp)
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (!isActive) {
                                                TextButton(
                                                    onClick = {
                                                        viewModel.setActiveAdminGeminiApiKey(index, syncToFirebase = true)
                                                    },
                                                    modifier = Modifier.testTag("set_active_key_$index")
                                                ) {
                                                    Text("Make Primary", fontSize = 11.sp)
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    viewModel.removeAdminGeminiApiKey(index, syncToFirebase = true)
                                                },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .testTag("delete_key_$index")
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Remove Key",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Ping Test Result Banner
                                    if (testResult != null) {
                                        val isSuccessResult = testResult.contains("Valid", ignoreCase = true)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSuccessResult) BentoEmerald.copy(alpha = 0.12f) else BentoRose.copy(alpha = 0.12f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isSuccessResult) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                                    contentDescription = null,
                                                    tint = if (isSuccessResult) BentoEmerald else BentoRose,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = testResult,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isSuccessResult) BentoEmerald else BentoRose
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 5. Dynamic Failover Engine Architecture Card ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(BentoCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = BentoCyan, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(
                                text = "Rate-Limit Cycling Architecture",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Automatic failover sequence when HTTP 429 / Quota Limit occurs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Failover step diagram
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FailoverStepRow(
                            stepNumber = "1",
                            title = "Key-Level Automatic Cycling",
                            desc = "When a key hits HTTP 429, it enters a 120s cooldown and the engine instantly switches to the next available API key in the pool."
                        )
                        HorizontalDivider()
                        FailoverStepRow(
                            stepNumber = "2",
                            title = "Model Fallback Hierarchy",
                            desc = "If all keys are rate-limited on the primary model, the engine falls back to secondary models (gemini-2.5-flash ➔ gemini-2.5-pro ➔ gemini-2.0-flash)."
                        )
                        HorizontalDivider()
                        FailoverStepRow(
                            stepNumber = "3",
                            title = "Local Fine-Tuned Engine (Zero Downtime)",
                            desc = "If external APIs are completely exhausted, curriculum-aligned questions are generated locally without disrupting user assessments."
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.clearRateLimitCooldowns() },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reset All Cooldowns", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // --- 6. Interactive AI Generation Test Console ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_ai_test_console_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(BentoViolet.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BentoViolet, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(
                                text = "Test AI Question Generator",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Verify question generation, active key usage, and failover telemetry",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = testTopic,
                        onValueChange = { testTopic = it },
                        label = { Text("Topic") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_ai_topic_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Difficulty selection
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Difficulty", style = MaterialTheme.typography.labelSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("Easy", "Medium", "Hard").forEach { diff ->
                                    FilterChip(
                                        selected = testDifficulty == diff,
                                        onClick = { testDifficulty = diff },
                                        label = { Text(diff, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        // Question Count
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Count", style = MaterialTheme.typography.labelSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(1, 3, 5).forEach { count ->
                                    FilterChip(
                                        selected = testCount == count,
                                        onClick = { testCount = count },
                                        label = { Text("$count Qs", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = testCategory,
                            onValueChange = { testCategory = it },
                            label = { Text("Category") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = testTag,
                            onValueChange = { testTag = it },
                            label = { Text("Tag") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.testGenerateQuizQuestions(
                                topic = testTopic,
                                difficulty = testDifficulty,
                                count = testCount,
                                category = testCategory,
                                tag = testTag
                            )
                        },
                        enabled = !isTesting && testTopic.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_test_generate_ai_btn")
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Generating & Rotating Keys...")
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Run Question Generation Test", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Telemetry & Results
                    if (lastReport != null) {
                        val report = lastReport!!
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Model: ${report.modelUsed}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Key: ${report.keyUsedMasked}",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = BentoCyan
                                    )
                                    Text(
                                        text = "${report.durationMs}ms",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (report.failoverLogs.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = BentoAmber.copy(alpha = 0.15f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = "Failover Events Handled (${report.failoverLogs.size}):",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = BentoAmber
                                            )
                                            report.failoverLogs.forEach { log ->
                                                Text(text = "• $log", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = "Generated ${report.questions.size} QuizQuestion objects successfully.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )

                                // Batch save to Firestore button
                                OutlinedButton(
                                    onClick = {
                                        report.questions.forEach { q ->
                                            viewModel.saveQuizQuestion(q)
                                        }
                                        Toast.makeText(context, "Questions saved to Firestore", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_save_tested_questions_btn")
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Save All Questions to Firestore", fontSize = 12.sp)
                                }
                            }
                        }

                        // Preview of generated QuizQuestion cards
                        report.questions.forEachIndexed { qIdx, question ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("tested_question_card_$qIdx")
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Q${qIdx + 1}: ${question.questionText}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = BentoPrimary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = question.tag,
                                                fontSize = 10.sp,
                                                color = BentoPrimary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    question.options.forEachIndexed { optIdx, opt ->
                                        val isCorrect = opt.equals(question.correctAnswer, ignoreCase = true)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isCorrect) BentoEmerald.copy(alpha = 0.12f) else Color.Transparent)
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "${('A' + optIdx)}.",
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCorrect) BentoEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = opt,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isCorrect) BentoEmerald else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isCorrect) {
                                                Spacer(Modifier.weight(1f))
                                                Text(
                                                    text = "✓ Correct",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BentoEmerald
                                                )
                                            }
                                        }
                                    }

                                    if (question.explanation.isNotBlank()) {
                                        Text(
                                            text = "Explanation: ${question.explanation}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FailoverStepRow(
    stepNumber: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(BentoPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = BentoPrimary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
