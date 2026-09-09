package com.example.ui.teacher

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entities.CategoryEntity
import com.example.data.local.entities.QuestionEntity
import com.example.data.local.entities.QuizSetEntity
import com.example.ui.QuizViewModel
import com.example.ui.components.BentoCard
import com.example.ui.components.BentoPillTag
import com.example.ui.theme.*
import com.example.util.CacheCsvFile
import com.example.util.CsvParseResult
import com.example.util.ParsedCsvQuestion
import com.example.util.QuizCsvHelper
import java.io.File

/**
 * Dialog allowing faculty and teachers to bulk-import quiz questions from CSV files
 * stored in the Android application's cache directory.
 *
 * Supports:
 * 1. Scanning and choosing CSV files from app cache (`context.cacheDir`)
 * 2. Instant sample CSV template generation into cache for zero-friction testing
 * 3. In-app CSV editor for saving custom CSV files into cache
 * 4. RFC-4180 parsing with validation, error breakdown, and question preview
 * 5. Publishing as a brand-new Quiz Set or appending to an existing Quiz Set
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportQuizCsvModalDialog(
    categories: List<CategoryEntity>,
    targetQuizSet: QuizSetEntity? = null,
    availableQuizzes: List<QuizSetEntity> = emptyList(),
    viewModel: QuizViewModel,
    onDismiss: () -> Unit,
    onImportSuccess: (quizTitle: String, importedCount: Int) -> Unit
) {
    val context = LocalContext.current

    // Cache files state
    var cacheFiles by remember { mutableStateOf<List<CacheCsvFile>>(emptyList()) }
    var selectedCacheFile by remember { mutableStateOf<File?>(null) }
    var parseResult by remember { mutableStateOf<CsvParseResult?>(null) }
    var isParsing by remember { mutableStateOf(false) }

    // Active subtab: 0 = "Select Cache File", 1 = "Create / Paste in Cache", 2 = "Preview Questions"
    var activeTab by remember { mutableIntStateOf(0) }

    // New Quiz Metadata fields (used when targetQuizSet is null)
    var destinationMode by remember { mutableIntStateOf(if (targetQuizSet != null) 1 else 0) } // 0 = New Quiz, 1 = Existing Quiz
    var selectedExistingQuiz by remember { mutableStateOf(targetQuizSet ?: availableQuizzes.firstOrNull()) }
    var newQuizTitle by remember { mutableStateOf("") }
    var newQuizDescription by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()) }
    var difficulty by remember { mutableStateOf("Medium") }
    var durationMinutes by remember { mutableIntStateOf(10) }
    var passPercentage by remember { mutableIntStateOf(60) }
    var tagsInput by remember { mutableStateOf("") }

    // Custom CSV Editor State
    var customFileNameInput by remember { mutableStateOf("custom_quiz_import.csv") }
    var customCsvContentInput by remember { mutableStateOf(QuizCsvHelper.getSampleCsvContent()) }

    // Function to reload files from cache
    val refreshCacheFiles = {
        val files = QuizCsvHelper.listCacheCsvFiles(context)
        cacheFiles = files
        if (selectedCacheFile == null && files.isNotEmpty()) {
            selectedCacheFile = files.first().file
            val res = QuizCsvHelper.parseCsvFile(files.first().file)
            parseResult = res
            if (newQuizTitle.isBlank() && res.isSuccessful) {
                newQuizTitle = files.first().name.removeSuffix(".csv").replace('_', ' ').replaceFirstChar { it.uppercase() }
            }
        }
    }

    LaunchedEffect(Unit) {
        // Automatically make sure a sample CSV exists in cache if none present
        val existing = QuizCsvHelper.listCacheCsvFiles(context)
        if (existing.isEmpty()) {
            val sample = QuizCsvHelper.createOrGetSampleCsvInCache(context)
            selectedCacheFile = sample
        }
        refreshCacheFiles()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 16.dp)
                .testTag("import_quiz_csv_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Dialog Header
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BentoPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                tint = BentoPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Import Quiz from Cache",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Bulk upload formatted questions via CSV stored in app cache",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("import_quiz_dialog_close")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs: 1. Cache Files | 2. CSV Editor in Cache | 3. Preview & Config
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = BentoPrimary
                        )
                    }
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Cache Files (${cacheFiles.size})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("tab_cache_files")
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Cache Editor", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("tab_csv_editor")
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = {
                            Text(
                                text = "Questions (${parseResult?.questions?.size ?: 0})",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        },
                        icon = { Icon(Icons.Default.Preview, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("tab_preview_config")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Active Tab Content
                Box(modifier = Modifier.weight(1f)) {
                    when (activeTab) {
                        0 -> {
                            // Cache Files List & Actions
                            CacheFilesSection(
                                cacheFiles = cacheFiles,
                                selectedFile = selectedCacheFile,
                                parseResult = parseResult,
                                onSelectFile = { file ->
                                    selectedCacheFile = file
                                    isParsing = true
                                    val res = QuizCsvHelper.parseCsvFile(file)
                                    parseResult = res
                                    isParsing = false
                                    if (newQuizTitle.isBlank() && res.isSuccessful) {
                                        newQuizTitle = file.name.removeSuffix(".csv").replace('_', ' ').replaceFirstChar { it.uppercase() }
                                    }
                                },
                                onGenerateSampleCsv = {
                                    val sampleFile = QuizCsvHelper.createOrGetSampleCsvInCache(context, forceRefresh = true)
                                    selectedCacheFile = sampleFile
                                    refreshCacheFiles()
                                    parseResult = QuizCsvHelper.parseCsvFile(sampleFile)
                                    Toast.makeText(context, "Sample quiz CSV generated in cache!", Toast.LENGTH_SHORT).show()
                                },
                                onDeleteFile = { file ->
                                    QuizCsvHelper.deleteCacheFile(file)
                                    if (selectedCacheFile?.absolutePath == file.absolutePath) {
                                        selectedCacheFile = null
                                        parseResult = null
                                    }
                                    refreshCacheFiles()
                                },
                                onProceedToPreview = {
                                    activeTab = 2
                                }
                            )
                        }
                        1 -> {
                            // In-App CSV Cache Editor
                            CacheEditorSection(
                                fileName = customFileNameInput,
                                onFileNameChange = { customFileNameInput = it },
                                csvContent = customCsvContentInput,
                                onCsvContentChange = { customCsvContentInput = it },
                                onResetToTemplate = {
                                    customCsvContentInput = QuizCsvHelper.getSampleCsvContent()
                                },
                                onSaveToCacheAndParse = {
                                    val savedFile = QuizCsvHelper.writeCsvToCache(context, customFileNameInput, customCsvContentInput)
                                    selectedCacheFile = savedFile
                                    refreshCacheFiles()
                                    val res = QuizCsvHelper.parseCsvFile(savedFile)
                                    parseResult = res
                                    activeTab = 2
                                    Toast.makeText(context, "Saved to cache (${savedFile.name})!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        2 -> {
                            // Preview Questions & Destination Quiz Settings
                            PreviewAndConfigSection(
                                parseResult = parseResult,
                                categories = categories,
                                targetQuizSet = targetQuizSet,
                                availableQuizzes = availableQuizzes,
                                destinationMode = destinationMode,
                                onDestinationModeChange = { destinationMode = it },
                                selectedExistingQuiz = selectedExistingQuiz,
                                onSelectExistingQuiz = { selectedExistingQuiz = it },
                                newQuizTitle = newQuizTitle,
                                onNewQuizTitleChange = { newQuizTitle = it },
                                newQuizDescription = newQuizDescription,
                                onNewQuizDescriptionChange = { newQuizDescription = it },
                                selectedCategory = selectedCategory,
                                onSelectCategory = { selectedCategory = it },
                                difficulty = difficulty,
                                onDifficultyChange = { difficulty = it },
                                durationMinutes = durationMinutes,
                                onDurationChange = { durationMinutes = it },
                                passPercentage = passPercentage,
                                onPassPercentageChange = { passPercentage = it },
                                tagsInput = tagsInput,
                                onTagsInputChange = { tagsInput = it }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("import_quiz_cancel_button")
                    ) {
                        Text("Cancel")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (activeTab < 2 && parseResult?.isSuccessful == true) {
                            OutlinedButton(
                                onClick = { activeTab = 2 },
                                modifier = Modifier.testTag("import_quiz_next_preview_btn")
                            ) {
                                Text("Preview (${parseResult?.questions?.size})")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }

                        val canImport = parseResult?.isSuccessful == true && (
                                destinationMode == 1 && selectedExistingQuiz != null ||
                                        destinationMode == 0 && newQuizTitle.isNotBlank() && selectedCategory != null
                                )

                        Button(
                            onClick = {
                                val questions = parseResult?.questions ?: emptyList()
                                if (questions.isEmpty()) return@Button

                                if (destinationMode == 1 && selectedExistingQuiz != null) {
                                    // Append to existing quiz
                                    val targetQuiz = selectedExistingQuiz!!
                                    val entities = questions.map { it.toQuestionEntity(targetQuiz.id) }
                                    viewModel.bulkImportQuestions(
                                        quizSetId = targetQuiz.id,
                                        questions = entities,
                                        onSuccess = { count ->
                                            onImportSuccess(targetQuiz.title, count)
                                            onDismiss()
                                        }
                                    )
                                } else if (destinationMode == 0 && selectedCategory != null) {
                                    // Create brand new quiz set from CSV
                                    val entities = questions.map { it.toQuestionEntity("") }
                                    viewModel.createManualQuiz(
                                        title = newQuizTitle.trim(),
                                        description = newQuizDescription.trim().ifBlank { "Imported from CSV (${parseResult?.fileName ?: "cache"})" },
                                        categoryId = selectedCategory!!.id,
                                        categoryName = selectedCategory!!.name,
                                        durationMinutes = durationMinutes,
                                        passPercentage = passPercentage,
                                        difficulty = difficulty,
                                        questions = entities,
                                        tags = tagsInput
                                    )
                                    onImportSuccess(newQuizTitle.trim(), entities.size)
                                    onDismiss()
                                }
                            },
                            enabled = canImport,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BentoEmerald,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("import_quiz_confirm_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (destinationMode == 1) {
                                    "Import ${parseResult?.questions?.size ?: 0} Questions"
                                } else {
                                    "Publish Quiz (${parseResult?.questions?.size ?: 0} Qs)"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Section 1: Browse, Select, and Generate CSV files in the application cache.
 */
@Composable
private fun CacheFilesSection(
    cacheFiles: List<CacheCsvFile>,
    selectedFile: File?,
    parseResult: CsvParseResult?,
    onSelectFile: (File) -> Unit,
    onGenerateSampleCsv: () -> Unit,
    onDeleteFile: (File) -> Unit,
    onProceedToPreview: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Cache Info Banner
        BentoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = BentoCyan.copy(alpha = 0.08f),
            borderColor = BentoCyan.copy(alpha = 0.35f),
            cornerRadius = 16.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = BentoCyan, modifier = Modifier.size(28.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "App Cache Directory Storage",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = BentoCyan
                    )
                    Text(
                        text = "CSV files stored in cacheDir/quiz_imports/ can be loaded and imported directly without external dependencies.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = onGenerateSampleCsv,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = BentoCyan.copy(alpha = 0.2f),
                        contentColor = BentoCyan
                    ),
                    modifier = Modifier.testTag("btn_generate_sample_csv")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sample Template", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // List of detected CSV files in Cache
        Text(
            text = "Detected CSV Files in Cache (${cacheFiles.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (cacheFiles.isEmpty()) {
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.FolderOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No CSV files found in cache", fontWeight = FontWeight.SemiBold)
                    Text("Tap 'Sample Template' above or use the 'Cache Editor' tab to generate a CSV file.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            cacheFiles.forEach { item ->
                val isSelected = selectedFile?.absolutePath == item.file.absolutePath
                BentoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectFile(item.file) }
                        .testTag("cache_csv_item_${item.name}"),
                    borderColor = if (isSelected) BentoPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    backgroundColor = if (isSelected) BentoPrimary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface,
                    cornerRadius = 14.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectFile(item.file) },
                            modifier = Modifier.testTag("radio_csv_${item.name}")
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BentoPillTag(
                                    text = item.formattedSize,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = item.formattedDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { onDeleteFile(item.file) },
                            modifier = Modifier.testTag("delete_csv_${item.name}")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = BentoRose)
                        }
                    }
                }
            }
        }

        // Active Parse Summary if a file is selected
        if (parseResult != null) {
            val res = parseResult
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = if (res.isSuccessful) BentoEmerald.copy(alpha = 0.08f) else BentoRose.copy(alpha = 0.08f),
                borderColor = if (res.isSuccessful) BentoEmerald.copy(alpha = 0.4f) else BentoRose.copy(alpha = 0.4f),
                cornerRadius = 16.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = if (res.isSuccessful) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (res.isSuccessful) BentoEmerald else BentoRose
                            )
                            Text(
                                text = if (res.isSuccessful) "Ready: ${res.questions.size} Questions Validated" else "Parsing Errors Detected",
                                fontWeight = FontWeight.Bold,
                                color = if (res.isSuccessful) BentoEmerald else BentoRose
                            )
                        }

                        if (res.isSuccessful) {
                            FilledTonalButton(
                                onClick = onProceedToPreview,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = BentoEmerald.copy(alpha = 0.2f),
                                    contentColor = BentoEmerald
                                ),
                                modifier = Modifier.testTag("btn_proceed_to_preview")
                            ) {
                                Text("Configure & Import", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    if (res.warningsOrErrors.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            res.warningsOrErrors.take(4).forEach { issue ->
                                Text(
                                    text = "• $issue",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BentoRose
                                )
                            }
                            if (res.warningsOrErrors.size > 4) {
                                Text(
                                    text = "...and ${res.warningsOrErrors.size - 4} more warnings.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BentoRose.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Section 2: Write, paste, or tweak CSV text directly and save to the app's cache.
 */
@Composable
private fun CacheEditorSection(
    fileName: String,
    onFileNameChange: (String) -> Unit,
    csvContent: String,
    onCsvContentChange: (String) -> Unit,
    onResetToTemplate: () -> Unit,
    onSaveToCacheAndParse: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = fileName,
                onValueChange = onFileNameChange,
                label = { Text("Cache File Name") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_cache_filename")
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onResetToTemplate,
                modifier = Modifier.testTag("btn_reset_csv_template")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sample Template", fontSize = 12.sp)
            }
        }

        Text(
            text = "CSV Content Format:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Question,Option A,Option B,Option C,Option D,Correct Option,Explanation",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = BentoViolet
        )

        OutlinedTextField(
            value = csvContent,
            onValueChange = onCsvContentChange,
            label = { Text("CSV Data (Comma Separated Values)") },
            placeholder = { Text("Paste or type CSV lines here...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .testTag("input_csv_content"),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
        )

        Button(
            onClick = onSaveToCacheAndParse,
            colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_save_csv_to_cache")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save to Cache & Validate CSV", fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Section 3: Questions list preview and destination Quiz Set configuration.
 */
@Composable
private fun PreviewAndConfigSection(
    parseResult: CsvParseResult?,
    categories: List<CategoryEntity>,
    targetQuizSet: QuizSetEntity?,
    availableQuizzes: List<QuizSetEntity>,
    destinationMode: Int, // 0 = New Quiz, 1 = Existing Quiz
    onDestinationModeChange: (Int) -> Unit,
    selectedExistingQuiz: QuizSetEntity?,
    onSelectExistingQuiz: (QuizSetEntity) -> Unit,
    newQuizTitle: String,
    onNewQuizTitleChange: (String) -> Unit,
    newQuizDescription: String,
    onNewQuizDescriptionChange: (String) -> Unit,
    selectedCategory: CategoryEntity?,
    onSelectCategory: (CategoryEntity) -> Unit,
    difficulty: String,
    onDifficultyChange: (String) -> Unit,
    durationMinutes: Int,
    onDurationChange: (Int) -> Unit,
    passPercentage: Int,
    onPassPercentageChange: (Int) -> Unit,
    tagsInput: String,
    onTagsInputChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Destination Target Mode (New Quiz vs Add to Existing Quiz)
        if (targetQuizSet == null && availableQuizzes.isNotEmpty()) {
            TabRow(selectedTabIndex = destinationMode) {
                Tab(
                    selected = destinationMode == 0,
                    onClick = { onDestinationModeChange(0) },
                    text = { Text("Publish as New Quiz Set") },
                    modifier = Modifier.testTag("tab_dest_new_quiz")
                )
                Tab(
                    selected = destinationMode == 1,
                    onClick = { onDestinationModeChange(1) },
                    text = { Text("Add to Existing Quiz (${availableQuizzes.size})") },
                    modifier = Modifier.testTag("tab_dest_existing_quiz")
                )
            }
        }

        if (destinationMode == 1) {
            // Existing Quiz Selection
            Text("Select Target Quiz to Append Questions:", fontWeight = FontWeight.SemiBold)
            availableQuizzes.forEach { quiz ->
                val isSelected = selectedExistingQuiz?.id == quiz.id
                BentoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectExistingQuiz(quiz) },
                    borderColor = if (isSelected) BentoPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    backgroundColor = if (isSelected) BentoPrimary.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface,
                    cornerRadius = 14.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectExistingQuiz(quiz) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = quiz.title, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${quiz.categoryName} • ${quiz.difficulty} • ${quiz.durationMinutes} mins",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            // New Quiz Configuration Form
            Text("Quiz Set Configuration:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = newQuizTitle,
                onValueChange = onNewQuizTitleChange,
                label = { Text("Quiz Title *") },
                placeholder = { Text("e.g., General Cell Biology & Physiology") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_new_quiz_title")
            )

            OutlinedTextField(
                value = newQuizDescription,
                onValueChange = onNewQuizDescriptionChange,
                label = { Text("Description (Optional)") },
                placeholder = { Text("Comprehensive assessment imported via cache CSV") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_new_quiz_desc")
            )

            Text("Subject Category *:")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory?.id == cat.id,
                        onClick = { onSelectCategory(cat) },
                        label = { Text(cat.name, fontSize = 12.sp) },
                        modifier = Modifier.testTag("category_chip_${cat.id}")
                    )
                }
            }

            Text("Difficulty Complexity:")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Easy", "Medium", "Hard").forEach { diff ->
                    FilterChip(
                        selected = difficulty.equals(diff, ignoreCase = true),
                        onClick = { onDifficultyChange(diff) },
                        label = { Text(diff) },
                        modifier = Modifier.testTag("difficulty_chip_$diff")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = durationMinutes.toString(),
                    onValueChange = { str -> onDurationChange(str.toIntOrNull() ?: 10) },
                    label = { Text("Duration (Mins)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = passPercentage.toString(),
                    onValueChange = { str -> onPassPercentageChange(str.toIntOrNull() ?: 60) },
                    label = { Text("Pass Mark (%)") },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = tagsInput,
                onValueChange = onTagsInputChange,
                label = { Text("Curriculum Tags (Comma-separated)") },
                placeholder = { Text("Biology, Science, STEM, Exam Prep") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Questions Preview Section
        val questions = parseResult?.questions ?: emptyList()
        Text(
            text = "Parsed Questions Preview (${questions.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (questions.isEmpty()) {
            Text(
                text = "No questions parsed. Select a valid CSV file in the 'Cache Files' tab.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            questions.forEachIndexed { idx, q ->
                BentoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preview_question_card_$idx"),
                    cornerRadius = 14.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Question ${idx + 1} (Row ${q.rowNumber})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = BentoPrimary
                            )
                            BentoPillTag(
                                text = "Answer: ${q.correctOptionLetter}",
                                containerColor = BentoEmerald.copy(alpha = 0.15f),
                                contentColor = BentoEmerald,
                                icon = Icons.Default.CheckCircle
                            )
                        }

                        Text(
                            text = q.questionText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        // 4 Multiple Choice Options Preview
                        val options = listOf("A" to q.optionA, "B" to q.optionB, "C" to q.optionC, "D" to q.optionD)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            options.forEachIndexed { optIdx, (letter, text) ->
                                val isCorrect = optIdx == q.correctOptionIndex
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCorrect) BentoEmerald.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = if (isCorrect) BorderStroke(1.dp, BentoEmerald) else null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "$letter.",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCorrect) BentoEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = text,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isCorrect) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Correct",
                                                tint = BentoEmerald,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (q.explanation.isNotBlank()) {
                            Text(
                                text = "Explanation: ${q.explanation}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
