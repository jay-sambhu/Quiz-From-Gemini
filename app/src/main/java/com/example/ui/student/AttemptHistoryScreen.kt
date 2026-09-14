package com.example.ui.student

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.QuestionEntity
import com.example.data.local.entities.QuizAttemptEntity
import com.example.data.local.entities.QuizSetEntity
import com.example.ui.QuizViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Past Quiz History & Review Screen for Students
 *
 * Displays all completed quizzes with:
 * - High-level student performance analytics (Quizzes completed, average score, pass rate, study time)
 * - Search by quiz title/category & filter chips (All, Passed, Needs Practice, Categories)
 * - Sorting controls (Newest first, Highest score, Lowest score)
 * - Detailed attempt breakdown cards with accuracy progress indicators
 * - Question-by-question breakdown showing student's chosen option vs correct answer
 * - Quick filtering for questions (All, Incorrect, Correct) to focus on mistakes
 * - Comprehensive explanations and learning takeaways
 * - Direct option to retake quizzes for mastery
 */
/**
 * Past History Screen for Students.
 *
 * Retrieves completed quizzes and scores from Cloud Firestore (quiz_attempts collection)
 * and displays them in a chronological list with comprehensive performance analytics:
 * - Direct retrieval & live synchronization with Firestore
 * - Chronological list ordering (Newest & Oldest first)
 * - Clear score breakdowns (Score, Total Questions, Percentage, Pass/Fail)
 * - Question-by-question evaluations and learning takeaways
 * - Direct option to retake quizzes for mastery
 */
@Composable
fun PastHistoryScreen(
    viewModel: QuizViewModel,
    onBack: () -> Unit,
    onRetakeQuiz: ((QuizSetEntity) -> Unit)? = null
) {
    AttemptHistoryScreen(
        viewModel = viewModel,
        onBack = onBack,
        onRetakeQuiz = onRetakeQuiz
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttemptHistoryScreen(
    viewModel: QuizViewModel,
    onBack: () -> Unit,
    onRetakeQuiz: ((QuizSetEntity) -> Unit)? = null
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allAttempts by viewModel.allAttempts.collectAsState()
    val allQuizSets by viewModel.allQuizSets.collectAsState()
    val isFetchingFirestoreHistory by viewModel.isFetchingFirestoreHistory.collectAsState()
    val firestoreHistoryLastSynced by viewModel.firestoreHistoryLastSynced.collectAsState()
    val isCloudConnected by viewModel.isCloudConnected.collectAsState()

    // Retrieve latest completed quizzes and scores directly from Cloud Firestore upon opening screen
    LaunchedEffect(currentUser?.id) {
        if (currentUser != null) {
            viewModel.refreshStudentPastHistoryFromFirestore()
        }
    }

    // Map quiz sets by id for fast difficulty and metadata lookup
    val quizSetMap = remember(allQuizSets) {
        allQuizSets.associateBy { it.id }
    }

    // Filter to current student's attempts
    val studentAttempts = remember(allAttempts, currentUser) {
        val studentId = currentUser?.id ?: ""
        allAttempts.filter { it.studentId == studentId }
    }

    // Search and filter states
    var searchQuery by remember { mutableStateOf("") }
    var selectedResultFilter by remember { mutableStateOf("ALL") } // ALL, PASSED, FAILED
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var selectedDifficultyFilter by remember { mutableStateOf("ALL") } // ALL, Easy, Medium, Hard
    var selectedSortOrder by remember { mutableStateOf(SortOrder.NEWEST) }

    // Unique categories from student's past attempts
    val availableCategories = remember(studentAttempts) {
        studentAttempts.map { it.categoryName }.filter { it.isNotBlank() }.distinct()
    }

    // Difficulty breakdown counts from past attempts
    val easyCount = remember(studentAttempts, quizSetMap) {
        studentAttempts.count { (quizSetMap[it.quizSetId]?.difficulty ?: "Medium").equals("Easy", ignoreCase = true) }
    }
    val mediumCount = remember(studentAttempts, quizSetMap) {
        studentAttempts.count { (quizSetMap[it.quizSetId]?.difficulty ?: "Medium").equals("Medium", ignoreCase = true) }
    }
    val hardCount = remember(studentAttempts, quizSetMap) {
        studentAttempts.count { (quizSetMap[it.quizSetId]?.difficulty ?: "Medium").equals("Hard", ignoreCase = true) }
    }

    // Filtered & sorted attempts
    val filteredAttempts = remember(
        studentAttempts,
        quizSetMap,
        searchQuery,
        selectedResultFilter,
        selectedCategoryFilter,
        selectedDifficultyFilter,
        selectedSortOrder
    ) {
        var list = studentAttempts

        // Search query filter
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.trim().lowercase()
            list = list.filter {
                it.quizTitle.lowercase().contains(query) ||
                        it.categoryName.lowercase().contains(query)
            }
        }

        // Result filter
        when (selectedResultFilter) {
            "PASSED" -> list = list.filter { it.percentage >= 60f }
            "FAILED" -> list = list.filter { it.percentage < 60f }
        }

        // Category filter
        if (selectedCategoryFilter != "ALL") {
            list = list.filter { it.categoryName.equals(selectedCategoryFilter, ignoreCase = true) }
        }

        // Difficulty filter (Easy, Medium, Hard)
        if (selectedDifficultyFilter != "ALL") {
            list = list.filter { attempt ->
                val diff = quizSetMap[attempt.quizSetId]?.difficulty ?: "Medium"
                diff.equals(selectedDifficultyFilter, ignoreCase = true)
            }
        }

        // Sort order
        when (selectedSortOrder) {
            SortOrder.NEWEST -> list.sortedByDescending { it.completedAt }
            SortOrder.OLDEST -> list.sortedBy { it.completedAt }
            SortOrder.HIGHEST_SCORE -> list.sortedByDescending { it.percentage }
            SortOrder.LOWEST_SCORE -> list.sortedBy { it.percentage }
        }
    }

    // Aggregated stats
    val passedCount = remember(studentAttempts) { studentAttempts.count { it.percentage >= 60f } }
    val failedCount = remember(studentAttempts) { studentAttempts.count { it.percentage < 60f } }
    val avgScore = remember(studentAttempts) {
        if (studentAttempts.isNotEmpty()) studentAttempts.map { it.percentage }.average().toFloat() else 0f
    }
    val totalTimeSeconds = remember(studentAttempts) { studentAttempts.sumOf { it.timeSpentSeconds } }

    Scaffold(
        modifier = Modifier.testTag("past_history_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Past History",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Completed Quizzes & Scores from Firestore",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("attempt_history_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard"
                        )
                    }
                },
                actions = {
                    // Manual Firestore Refresh Action
                    IconButton(
                        onClick = { viewModel.refreshStudentPastHistoryFromFirestore() },
                        enabled = !isFetchingFirestoreHistory,
                        modifier = Modifier.testTag("past_history_sync_btn")
                    ) {
                        if (isFetchingFirestoreHistory) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = BentoPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh from Firestore",
                                tint = BentoPrimary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BentoPrimary.copy(alpha = 0.12f),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.FactCheck,
                                contentDescription = null,
                                tint = BentoPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${studentAttempts.size} Completed",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = BentoPrimary
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (studentAttempts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Firestore status banner even when empty
                PastHistoryFirestoreStatusBar(
                    isSyncing = isFetchingFirestoreHistory,
                    isCloudConnected = isCloudConnected,
                    lastSyncedTime = firestoreHistoryLastSynced,
                    totalRecords = studentAttempts.size,
                    onSyncNow = { viewModel.refreshStudentPastHistoryFromFirestore() }
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    NoQuizResultsEmptyState(
                        isCard = true,
                        customTitle = "No Quiz History Found",
                        customDescription = "No completed quiz attempts found in Firestore for your account. Complete a quiz to see your scores and full answers in this chronological list.",
                        onTakeQuiz = onBack,
                        testTag = "attempt_history_empty_state"
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
            ) {
                // 1. Cloud Firestore Sync Status Banner
                item {
                    PastHistoryFirestoreStatusBar(
                        isSyncing = isFetchingFirestoreHistory,
                        isCloudConnected = isCloudConnected,
                        lastSyncedTime = firestoreHistoryLastSynced,
                        totalRecords = studentAttempts.size,
                        onSyncNow = { viewModel.refreshStudentPastHistoryFromFirestore() }
                    )
                }

                // 2. High-Level Performance Metrics Header
                item {
                    PastHistoryMetricsHeader(
                        totalAttempts = studentAttempts.size,
                        avgScore = avgScore,
                        passedCount = passedCount,
                        failedCount = failedCount,
                        totalTimeSeconds = totalTimeSeconds
                    )
                }

                // 3. Search & Filter Bar
                item {
                    PastHistoryFilterSection(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        selectedResultFilter = selectedResultFilter,
                        onResultFilterChange = { selectedResultFilter = it },
                        totalCount = studentAttempts.size,
                        passedCount = passedCount,
                        failedCount = failedCount,
                        availableCategories = availableCategories,
                        selectedCategoryFilter = selectedCategoryFilter,
                        onCategoryFilterChange = { selectedCategoryFilter = it },
                        selectedDifficultyFilter = selectedDifficultyFilter,
                        onDifficultyFilterChange = { selectedDifficultyFilter = it },
                        easyCount = easyCount,
                        mediumCount = mediumCount,
                        hardCount = hardCount,
                        selectedSortOrder = selectedSortOrder,
                        onSortOrderChange = { selectedSortOrder = it }
                    )
                }

                // 4. Chronological Section Title & Results Count
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Chronological Quiz History (${filteredAttempts.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (selectedSortOrder) {
                                    SortOrder.NEWEST -> "Sorted newest to oldest"
                                    SortOrder.OLDEST -> "Sorted oldest to newest"
                                    SortOrder.HIGHEST_SCORE -> "Sorted by highest score"
                                    SortOrder.LOWEST_SCORE -> "Sorted by lowest score"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (filteredAttempts.size != studentAttempts.size) {
                            TextButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedResultFilter = "ALL"
                                    selectedCategoryFilter = "ALL"
                                    selectedDifficultyFilter = "ALL"
                                }
                            ) {
                                Text("Reset Filters", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // 5. Empty search results if filtered list is empty
                if (filteredAttempts.isEmpty()) {
                    item {
                        BentoCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("filtered_empty_card"),
                            cornerRadius = 16.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.FilterAltOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No assessments match your criteria",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Try clearing search keywords or choosing 'All' filters.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedButton(
                                    onClick = {
                                        searchQuery = ""
                                        selectedResultFilter = "ALL"
                                        selectedCategoryFilter = "ALL"
                                        selectedDifficultyFilter = "ALL"
                                    }
                                ) {
                                    Text("Clear All Filters")
                                }
                            }
                        }
                    }
                } else {
                    // 6. Chronological List Items with Detailed Answer Breakdown
                    itemsIndexed(filteredAttempts, key = { _, it -> it.id }) { index, attempt ->
                        val targetQuiz = allQuizSets.find { it.id == attempt.quizSetId }
                        PastHistoryAttemptCard(
                            attempt = attempt,
                            viewModel = viewModel,
                            targetQuiz = targetQuiz,
                            onRetakeQuiz = onRetakeQuiz,
                            chronologicalIndex = if (selectedSortOrder == SortOrder.NEWEST) (filteredAttempts.size - index) else (index + 1)
                        )
                    }
                }
            }
        }
    }
}

enum class SortOrder {
    NEWEST,
    OLDEST,
    HIGHEST_SCORE,
    LOWEST_SCORE
}

/**
 * Cloud Firestore sync & retrieval status bar for student past history
 */
@Composable
fun PastHistoryFirestoreStatusBar(
    isSyncing: Boolean,
    isCloudConnected: Boolean,
    lastSyncedTime: Long,
    totalRecords: Int,
    onSyncNow: () -> Unit
) {
    BentoCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("past_history_firestore_status"),
        backgroundColor = if (isCloudConnected) BentoPrimary.copy(alpha = 0.05f) else BentoAmber.copy(alpha = 0.06f),
        borderColor = if (isCloudConnected) BentoPrimary.copy(alpha = 0.2f) else BentoAmber.copy(alpha = 0.25f),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isCloudConnected) BentoEmerald.copy(alpha = 0.15f) else BentoAmber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSyncing) Icons.Default.Sync else if (isCloudConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = "Cloud Firestore",
                        tint = if (isCloudConnected) BentoEmerald else BentoAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Cloud Firestore: quiz_attempts",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isCloudConnected) BentoEmerald.copy(alpha = 0.15f) else BentoAmber.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isCloudConnected) "Live" else "Cached",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCloudConnected) BentoEmerald else BentoAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isSyncing) {
                            "Retrieving completed quizzes & scores..."
                        } else if (lastSyncedTime > 0L) {
                            val timeStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(lastSyncedTime))
                            "$totalRecords completed quizzes synced • Refreshed $timeStr"
                        } else {
                            "$totalRecords completed quizzes retrieved from Firestore"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedButton(
                onClick = onSyncNow,
                enabled = !isSyncing,
                modifier = Modifier
                    .height(36.dp)
                    .testTag("past_history_sync_now_btn"),
                contentPadding = PaddingValues(horizontal = 10.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = BentoPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Syncing", fontSize = 11.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Performance Analytics Header for Student History
 */
@Composable
fun PastHistoryMetricsHeader(
    totalAttempts: Int,
    avgScore: Float,
    passedCount: Int,
    failedCount: Int,
    totalTimeSeconds: Int
) {
    val totalMins = totalTimeSeconds / 60
    val totalSecs = totalTimeSeconds % 60
    val timeFormatted = if (totalMins > 0) "${totalMins}m ${totalSecs}s" else "${totalSecs}s"
    val passRatePercentage = if (totalAttempts > 0) (passedCount.toFloat() / totalAttempts.toFloat()) * 100f else 0f

    BentoCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_metrics_header"),
        backgroundColor = BentoPrimary.copy(alpha = 0.05f),
        borderColor = BentoPrimary.copy(alpha = 0.2f),
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BentoPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = BentoPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Learning Progress Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Aggregated across all submitted tests",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                BentoPillTag(
                    text = "${String.format("%.0f", passRatePercentage)}% Pass Rate",
                    containerColor = if (passRatePercentage >= 60f) BentoEmerald.copy(alpha = 0.15f) else BentoRose.copy(alpha = 0.15f),
                    contentColor = if (passRatePercentage >= 60f) BentoEmerald else BentoRose,
                    icon = if (passRatePercentage >= 60f) Icons.Default.Verified else Icons.Default.Warning
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3-Column Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Average Score
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${String.format("%.0f", avgScore)}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (avgScore >= 60f) BentoEmerald else BentoRose
                        )
                        Text(
                            text = "Avg Score",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Pass / Fail Ratio
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$passedCount",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = BentoEmerald
                            )
                            Text(
                                text = " / ",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$failedCount",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = BentoRose
                            )
                        }
                        Text(
                            text = "Passed / Failed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Time Invested
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = timeFormatted,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BentoCyan
                        )
                        Text(
                            text = "Time Spent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Filter and Search Controls for History
 */
@Composable
fun PastHistoryFilterSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedResultFilter: String,
    onResultFilterChange: (String) -> Unit,
    totalCount: Int,
    passedCount: Int,
    failedCount: Int,
    availableCategories: List<String>,
    selectedCategoryFilter: String,
    onCategoryFilterChange: (String) -> Unit,
    selectedDifficultyFilter: String,
    onDifficultyFilterChange: (String) -> Unit,
    easyCount: Int,
    mediumCount: Int,
    hardCount: Int,
    selectedSortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Search bar with sort button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("history_search_input"),
                placeholder = { Text("Search by quiz title or topic...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Box {
                OutlinedIconButton(
                    onClick = { showSortMenu = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort past quizzes"
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Newest Completed First") },
                        onClick = {
                            onSortOrderChange(SortOrder.NEWEST)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (selectedSortOrder == SortOrder.NEWEST) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = BentoPrimary)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Oldest Completed First") },
                        onClick = {
                            onSortOrderChange(SortOrder.OLDEST)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (selectedSortOrder == SortOrder.OLDEST) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = BentoPrimary)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Highest Score (%)") },
                        onClick = {
                            onSortOrderChange(SortOrder.HIGHEST_SCORE)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (selectedSortOrder == SortOrder.HIGHEST_SCORE) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = BentoPrimary)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Lowest Score (%)") },
                        onClick = {
                            onSortOrderChange(SortOrder.LOWEST_SCORE)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (selectedSortOrder == SortOrder.LOWEST_SCORE) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = BentoPrimary)
                            }
                        }
                    )
                }
            }
        }

        // Result Status Filter Chips (All | Passed | Failed)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedResultFilter == "ALL",
                onClick = { onResultFilterChange("ALL") },
                label = { Text("All ($totalCount)") },
                shape = RoundedCornerShape(12.dp)
            )

            FilterChip(
                selected = selectedResultFilter == "PASSED",
                onClick = { onResultFilterChange("PASSED") },
                label = { Text("Passed ($passedCount)") },
                leadingIcon = {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = BentoEmerald
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BentoEmerald.copy(alpha = 0.15f),
                    selectedLabelColor = BentoEmerald
                ),
                shape = RoundedCornerShape(12.dp)
            )

            FilterChip(
                selected = selectedResultFilter == "FAILED",
                onClick = { onResultFilterChange("FAILED") },
                label = { Text("Needs Practice ($failedCount)") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = BentoRose
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BentoRose.copy(alpha = 0.15f),
                    selectedLabelColor = BentoRose
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Difficulty / Complexity Filter Chips (All | Easy | Medium | Hard)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Difficulty:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp)
            )

            FilterChip(
                selected = selectedDifficultyFilter == "ALL",
                onClick = { onDifficultyFilterChange("ALL") },
                label = { Text("All ($totalCount)") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("filter_chip_difficulty_all")
            )

            FilterChip(
                selected = selectedDifficultyFilter.equals("Easy", ignoreCase = true),
                onClick = {
                    onDifficultyFilterChange(
                        if (selectedDifficultyFilter.equals("Easy", ignoreCase = true)) "ALL" else "Easy"
                    )
                },
                label = { Text("Easy ($easyCount)") },
                leadingIcon = {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = BentoEmerald
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BentoEmerald.copy(alpha = 0.15f),
                    selectedLabelColor = BentoEmerald
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("filter_chip_difficulty_easy")
            )

            FilterChip(
                selected = selectedDifficultyFilter.equals("Medium", ignoreCase = true),
                onClick = {
                    onDifficultyFilterChange(
                        if (selectedDifficultyFilter.equals("Medium", ignoreCase = true)) "ALL" else "Medium"
                    )
                },
                label = { Text("Medium ($mediumCount)") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = BentoAmber
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BentoAmber.copy(alpha = 0.15f),
                    selectedLabelColor = BentoAmber
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("filter_chip_difficulty_medium")
            )

            FilterChip(
                selected = selectedDifficultyFilter.equals("Hard", ignoreCase = true),
                onClick = {
                    onDifficultyFilterChange(
                        if (selectedDifficultyFilter.equals("Hard", ignoreCase = true)) "ALL" else "Hard"
                    )
                },
                label = { Text("Hard ($hardCount)") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = BentoRose
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BentoRose.copy(alpha = 0.15f),
                    selectedLabelColor = BentoRose
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("filter_chip_difficulty_hard")
            )
        }

        // Category Filter Chips (if multiple categories available)
        if (availableCategories.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp)
                )

                SuggestionChip(
                    onClick = { onCategoryFilterChange("ALL") },
                    label = { Text("All Subjects") },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (selectedCategoryFilter == "ALL") BentoViolet.copy(alpha = 0.15f) else Color.Transparent
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (selectedCategoryFilter == "ALL") BentoViolet else MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                availableCategories.forEach { category ->
                    val isSelected = selectedCategoryFilter.equals(category, ignoreCase = true)
                    SuggestionChip(
                        onClick = { onCategoryFilterChange(category) },
                        label = { Text(category) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isSelected) BentoViolet.copy(alpha = 0.15f) else Color.Transparent
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) BentoViolet else MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }
    }
}

/**
 * Individual Attempt Card with Detailed Breakdown Expansion
 */
@Composable
fun PastHistoryAttemptCard(
    attempt: QuizAttemptEntity,
    viewModel: QuizViewModel,
    targetQuiz: QuizSetEntity?,
    onRetakeQuiz: ((QuizSetEntity) -> Unit)?,
    chronologicalIndex: Int? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    var questionsList by remember { mutableStateOf<List<QuestionEntity>>(emptyList()) }
    var isLoadingQuestions by remember { mutableStateOf(false) }

    // Parse user answers JSON
    val userAnswersMap = remember(attempt.userAnswersJson) {
        parseUserAnswers(attempt.userAnswersJson)
    }

    val isPassed = attempt.percentage >= 60f
    val mins = attempt.timeSpentSeconds / 60
    val secs = attempt.timeSpentSeconds % 60
    val durationText = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(attempt.completedAt) { dateFormat.format(Date(attempt.completedAt)) }

    // Load questions when card is expanded
    LaunchedEffect(attempt.quizSetId, isExpanded) {
        if (isExpanded && questionsList.isEmpty()) {
            isLoadingQuestions = true
            try {
                questionsList = viewModel.repository.getQuestionsForQuizSet(attempt.quizSetId)
            } finally {
                isLoadingQuestions = false
            }
        }
    }

    // Evaluate questions against user answers
    val evaluations = remember(questionsList, userAnswersMap) {
        questionsList.mapIndexed { idx, q ->
            val chosen = userAnswersMap[q.id]
                ?: userAnswersMap[idx.toString()]
                ?: userAnswersMap["q_$idx"]
                ?: -1

            val isSkipped = chosen == -1
            val isCorrect = !isSkipped && chosen == q.correctOptionIndex

            QuestionEvaluation(
                question = q,
                questionIndex = idx,
                selectedOptionIndex = chosen,
                isCorrect = isCorrect,
                isSkipped = isSkipped
            )
        }
    }

    val correctCount = remember(evaluations, attempt.score) {
        if (evaluations.isNotEmpty()) evaluations.count { it.isCorrect } else attempt.score
    }
    val incorrectCount = remember(evaluations, attempt.totalQuestions, attempt.score) {
        if (evaluations.isNotEmpty()) evaluations.count { !it.isCorrect && !it.isSkipped } else (attempt.totalQuestions - attempt.score).coerceAtLeast(0)
    }
    val skippedCount = remember(evaluations) {
        if (evaluations.isNotEmpty()) evaluations.count { it.isSkipped } else 0
    }

    BentoCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("past_history_card_${attempt.id}"),
        cornerRadius = 20.dp,
        borderColor = if (isPassed) BentoEmerald.copy(alpha = 0.35f) else BentoRose.copy(alpha = 0.35f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Category Badge, Difficulty Badge, Chronological Index & Completed Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (chronologicalIndex != null) {
                        BentoPillTag(
                            text = "#$chronologicalIndex",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            icon = Icons.Default.Tag
                        )
                    }

                    if (attempt.categoryName.isNotBlank()) {
                        BentoPillTag(
                            text = attempt.categoryName,
                            containerColor = BentoViolet.copy(alpha = 0.12f),
                            contentColor = BentoViolet,
                            icon = Icons.Default.Category
                        )
                    }

                    val difficulty = targetQuiz?.difficulty ?: "Medium"
                    val (diffColor, diffBg) = when (difficulty.lowercase()) {
                        "easy" -> BentoEmerald to BentoEmerald.copy(alpha = 0.12f)
                        "hard" -> BentoRose to BentoRose.copy(alpha = 0.12f)
                        else -> BentoAmber to BentoAmber.copy(alpha = 0.12f)
                    }
                    val diffIcon = when (difficulty.lowercase()) {
                        "easy" -> Icons.Default.CheckCircle
                        "hard" -> Icons.Default.Bolt
                        else -> Icons.Default.Speed
                    }
                    BentoPillTag(
                        text = difficulty,
                        containerColor = diffBg,
                        contentColor = diffColor,
                        icon = diffIcon
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CloudDone,
                        contentDescription = "Firestore Verified",
                        modifier = Modifier.size(13.dp),
                        tint = BentoEmerald
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Firestore",
                        style = MaterialTheme.typography.labelSmall,
                        color = BentoEmerald,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Event,
                        contentDescription = "Completion Date",
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("past_history_date_${attempt.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quiz Title & Score Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = attempt.quizTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("past_history_title_${attempt.id}")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Time spent: $durationText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Score pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isPassed) BentoEmerald.copy(alpha = 0.15f) else BentoRose.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, if (isPassed) BentoEmerald.copy(alpha = 0.35f) else BentoRose.copy(alpha = 0.35f)),
                    modifier = Modifier.testTag("past_history_score_pill_${attempt.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${attempt.score} / ${attempt.totalQuestions}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isPassed) BentoEmerald else BentoRose,
                            modifier = Modifier.testTag("past_history_score_${attempt.id}")
                        )
                        Text(
                            text = "${String.format("%.0f", attempt.percentage)}% • ${if (isPassed) "Passed" else "Failed"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isPassed) BentoEmerald else BentoRose,
                            modifier = Modifier.testTag("past_history_percentage_${attempt.id}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Accuracy Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { (attempt.percentage / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isPassed) BentoEmerald else BentoRose,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Correct / Incorrect / Skipped Counters Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Correct Count Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BentoEmerald.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = BentoEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$correctCount Correct",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoEmerald
                        )
                    }
                }

                // Incorrect Count Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BentoRose.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = null,
                            tint = BentoRose,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$incorrectCount Incorrect",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoRose
                        )
                    }
                }

                // Skipped (if any)
                if (skippedCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BentoAmber.copy(alpha = 0.12f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = null,
                                tint = BentoAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$skippedCount Skipped",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BentoAmber
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row: Review Breakdown Toggle & Retake Quiz
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("toggle_review_btn_${attempt.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isExpanded) BentoViolet.copy(alpha = 0.08f) else Color.Transparent
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isExpanded) BentoViolet else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.AutoMirrored.Filled.FactCheck,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isExpanded) BentoViolet else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isExpanded) "Hide Breakdown" else "Review Answers",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isExpanded) BentoViolet else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (targetQuiz != null && onRetakeQuiz != null) {
                    Button(
                        onClick = { onRetakeQuiz(targetQuiz) },
                        modifier = Modifier.testTag("retake_btn_${attempt.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Retake",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Expanded Question-by-Question Breakdown
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    if (isLoadingQuestions) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Loading question breakdown...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (evaluations.isEmpty()) {
                        Text(
                            text = "No questions registered for this quiz set.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        DetailedQuestionBreakdownView(evaluations = evaluations)
                    }
                }
            }
        }
    }
}

/**
 * Evaluation data holder for a single question attempt
 */
data class QuestionEvaluation(
    val question: QuestionEntity,
    val questionIndex: Int,
    val selectedOptionIndex: Int,
    val isCorrect: Boolean,
    val isSkipped: Boolean
)

/**
 * Sub-view for viewing question breakdown with fast filtering (All, Incorrect, Correct)
 */
@Composable
fun DetailedQuestionBreakdownView(evaluations: List<QuestionEvaluation>) {
    var questionFilter by remember { mutableStateOf("ALL") } // ALL, INCORRECT, CORRECT

    val incorrectCount = evaluations.count { !it.isCorrect && !it.isSkipped }
    val correctCount = evaluations.count { it.isCorrect }

    val displayedEvaluations = remember(evaluations, questionFilter) {
        when (questionFilter) {
            "INCORRECT" -> evaluations.filter { !it.isCorrect }
            "CORRECT" -> evaluations.filter { it.isCorrect }
            else -> evaluations
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Question Sub-filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Breakdown:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FilterChip(
                selected = questionFilter == "ALL",
                onClick = { questionFilter = "ALL" },
                label = { Text("All (${evaluations.size})", style = MaterialTheme.typography.labelSmall) },
                shape = RoundedCornerShape(10.dp)
            )

            if (incorrectCount > 0) {
                FilterChip(
                    selected = questionFilter == "INCORRECT",
                    onClick = { questionFilter = "INCORRECT" },
                    label = { Text("Mistakes ($incorrectCount)", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = BentoRose,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BentoRose.copy(alpha = 0.15f),
                        selectedLabelColor = BentoRose
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            FilterChip(
                selected = questionFilter == "CORRECT",
                onClick = { questionFilter = "CORRECT" },
                label = { Text("Correct ($correctCount)", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = BentoEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BentoEmerald.copy(alpha = 0.15f),
                    selectedLabelColor = BentoEmerald
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }

        // Render each question review item
        displayedEvaluations.forEach { eval ->
            QuestionReviewItem(eval = eval)
        }
    }
}

/**
 * Detailed single question review item
 * Highlights:
 * - Student's choice vs Correct choice
 * - Explanations and educational insight
 */
@Composable
fun QuestionReviewItem(eval: QuestionEvaluation) {
    val q = eval.question
    val isCorrect = eval.isCorrect
    val isSkipped = eval.isSkipped
    val selectedIndex = eval.selectedOptionIndex
    val correctIndex = q.correctOptionIndex

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(
            1.dp,
            if (isCorrect) BentoEmerald.copy(alpha = 0.3f)
            else if (isSkipped) BentoAmber.copy(alpha = 0.3f)
            else BentoRose.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("question_review_${eval.questionIndex + 1}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Question # and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question ${eval.questionIndex + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isCorrect) {
                    BentoPillTag(
                        text = "CORRECT (+1)",
                        containerColor = BentoEmerald.copy(alpha = 0.15f),
                        contentColor = BentoEmerald,
                        icon = Icons.Default.CheckCircle
                    )
                } else if (isSkipped) {
                    BentoPillTag(
                        text = "UNANSWERED",
                        containerColor = BentoAmber.copy(alpha = 0.15f),
                        contentColor = BentoAmber,
                        icon = Icons.AutoMirrored.Filled.HelpOutline
                    )
                } else {
                    BentoPillTag(
                        text = "INCORRECT",
                        containerColor = BentoRose.copy(alpha = 0.15f),
                        contentColor = BentoRose,
                        icon = Icons.Default.Cancel
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Question Text
            Text(
                text = q.questionText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Options A, B, C, D Breakdown
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (optIndex in 0..3) {
                    val optionText = q.getOption(optIndex)
                    if (optionText.isNotBlank()) {
                        val isUserChoice = (optIndex == selectedIndex)
                        val isCorrectChoice = (optIndex == correctIndex)

                        val optionBgColor = when {
                            isUserChoice && isCorrectChoice -> BentoEmerald.copy(alpha = 0.12f)
                            isUserChoice && !isCorrectChoice -> BentoRose.copy(alpha = 0.12f)
                            !isUserChoice && isCorrectChoice -> BentoEmerald.copy(alpha = 0.08f)
                            else -> MaterialTheme.colorScheme.surface
                        }

                        val optionBorderColor = when {
                            isUserChoice && isCorrectChoice -> BentoEmerald
                            isUserChoice && !isCorrectChoice -> BentoRose
                            !isUserChoice && isCorrectChoice -> BentoEmerald.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        }

                        val letterChar = ('A'.code + optIndex).toChar()

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = optionBgColor,
                            border = BorderStroke(1.dp, optionBorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Option Letter Badge (A, B, C, D)
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isUserChoice && isCorrectChoice -> BentoEmerald
                                                isUserChoice && !isCorrectChoice -> BentoRose
                                                !isUserChoice && isCorrectChoice -> BentoEmerald.copy(alpha = 0.2f)
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = letterChar.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isUserChoice -> Color.White
                                            !isUserChoice && isCorrectChoice -> BentoEmerald
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = optionText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isUserChoice || isCorrectChoice) FontWeight.SemiBold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                // Status Indicator on Right
                                when {
                                    isUserChoice && isCorrectChoice -> {
                                        BentoPillTag(
                                            text = "Your Answer",
                                            containerColor = BentoEmerald.copy(alpha = 0.15f),
                                            contentColor = BentoEmerald,
                                            icon = Icons.Default.CheckCircle
                                        )
                                    }
                                    isUserChoice && !isCorrectChoice -> {
                                        BentoPillTag(
                                            text = "Your Answer",
                                            containerColor = BentoRose.copy(alpha = 0.15f),
                                            contentColor = BentoRose,
                                            icon = Icons.Default.Cancel
                                        )
                                    }
                                    !isUserChoice && isCorrectChoice -> {
                                        BentoPillTag(
                                            text = "Correct Answer",
                                            containerColor = BentoEmerald.copy(alpha = 0.12f),
                                            contentColor = BentoEmerald,
                                            icon = Icons.Default.Check
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Explanation Section
            if (q.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BentoViolet.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, BentoViolet.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = BentoViolet,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Explanation & Key Concept",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BentoViolet
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = q.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper to extract option string by index
 */
fun QuestionEntity.getOption(index: Int): String {
    return when (index) {
        0 -> optionA
        1 -> optionB
        2 -> optionC
        3 -> optionD
        else -> ""
    }
}

fun QuestionEntity.getOptionText(index: Int): String {
    return when (index) {
        0 -> "Option A: $optionA"
        1 -> "Option B: $optionB"
        2 -> "Option C: $optionC"
        else -> "Option D: $optionD"
    }
}

/**
 * Helper to safely parse userAnswersJson mapping
 */
fun parseUserAnswers(json: String?): Map<String, Int> {
    if (json.isNullOrBlank()) return emptyMap()
    return try {
        val trimmed = json.trim()
        if (!trimmed.startsWith("{")) return emptyMap()
        val jsonObject = JSONObject(trimmed)
        val result = mutableMapOf<String, Int>()
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result[key] = jsonObject.optInt(key, -1)
        }
        result
    } catch (e: Exception) {
        emptyMap()
    }
}
