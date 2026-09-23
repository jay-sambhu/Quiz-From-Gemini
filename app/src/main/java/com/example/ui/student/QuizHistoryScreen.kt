package com.example.ui.student

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import java.util.concurrent.TimeUnit

enum class QuizHistorySortOrder(val label: String) {
    NEWEST("Newest Completed First"),
    OLDEST("Oldest Completed First"),
    HIGHEST_SCORE("Highest Score (%)"),
    LOWEST_SCORE("Lowest Score (%)")
}

/**
 * Quiz History Screen for Students.
 *
 * Retrieves and displays a list of past quizzes taken by the student from Cloud Firestore
 * (quiz_attempts collection), including their scores and completion dates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizHistoryScreen(
    viewModel: QuizViewModel,
    onBack: () -> Unit,
    onRetakeQuiz: ((QuizSetEntity) -> Unit)? = null,
    onTakeQuiz: (() -> Unit)? = null
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allAttempts by viewModel.allAttempts.collectAsState()
    val allQuizSets by viewModel.allQuizSets.collectAsState()
    val isFetchingFirestoreHistory by viewModel.isFetchingFirestoreHistory.collectAsState()
    val firestoreHistoryLastSynced by viewModel.firestoreHistoryLastSynced.collectAsState()
    val isCloudConnected by viewModel.isCloudConnected.collectAsState()

    // Retrieve past quizzes and scores from Cloud Firestore upon entering screen
    LaunchedEffect(currentUser?.id) {
        if (currentUser != null) {
            viewModel.refreshStudentPastHistoryFromFirestore()
        }
    }

    // Lookup map for quiz sets
    val quizSetMap = remember(allQuizSets) {
        allQuizSets.associateBy { it.id }
    }

    // Filter to signed-in student's attempts
    val studentAttempts = remember(allAttempts, currentUser) {
        val studentId = currentUser?.id ?: ""
        allAttempts.filter { it.studentId == studentId }
    }

    // Search and filter states
    var searchQuery by remember { mutableStateOf("") }
    var selectedResultFilter by remember { mutableStateOf("ALL") } // ALL, PASSED, FAILED
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var selectedDifficultyFilter by remember { mutableStateOf("ALL") } // ALL, Easy, Medium, Hard
    var selectedSortOrder by remember { mutableStateOf(QuizHistorySortOrder.NEWEST) }

    // Unique categories from student attempts
    val availableCategories = remember(studentAttempts) {
        studentAttempts.map { it.categoryName }.filter { it.isNotBlank() }.distinct()
    }

    // Filtered and sorted attempts list
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

        // Search by quiz title or category
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.quizTitle.lowercase().contains(q) ||
                        it.categoryName.lowercase().contains(q)
            }
        }

        // Result status filter (Passed >= 60%, Failed < 60%)
        when (selectedResultFilter) {
            "PASSED" -> list = list.filter { it.percentage >= 60f }
            "FAILED" -> list = list.filter { it.percentage < 60f }
        }

        // Subject / Category filter
        if (selectedCategoryFilter != "ALL") {
            list = list.filter { it.categoryName.equals(selectedCategoryFilter, ignoreCase = true) }
        }

        // Difficulty filter
        if (selectedDifficultyFilter != "ALL") {
            list = list.filter { attempt ->
                val diff = quizSetMap[attempt.quizSetId]?.difficulty ?: "Medium"
                diff.equals(selectedDifficultyFilter, ignoreCase = true)
            }
        }

        // Sort ordering
        when (selectedSortOrder) {
            QuizHistorySortOrder.NEWEST -> list.sortedByDescending { it.completedAt }
            QuizHistorySortOrder.OLDEST -> list.sortedBy { it.completedAt }
            QuizHistorySortOrder.HIGHEST_SCORE -> list.sortedWith(
                compareByDescending<QuizAttemptEntity> { it.percentage }
                    .thenByDescending { it.completedAt }
            )
            QuizHistorySortOrder.LOWEST_SCORE -> list.sortedWith(
                compareBy<QuizAttemptEntity> { it.percentage }
                    .thenByDescending { it.completedAt }
            )
        }
    }

    // Summary KPIs
    val passedCount = remember(studentAttempts) { studentAttempts.count { it.percentage >= 60f } }
    val failedCount = remember(studentAttempts) { studentAttempts.count { it.percentage < 60f } }
    val avgScore = remember(studentAttempts) {
        if (studentAttempts.isNotEmpty()) studentAttempts.map { it.percentage }.average().toFloat() else 0f
    }
    val totalTimeSeconds = remember(studentAttempts) { studentAttempts.sumOf { it.timeSpentSeconds } }

    Scaffold(
        modifier = Modifier.testTag("quiz_history_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Quiz History",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Past quizzes, scores & completion dates from Firestore",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("quiz_history_back_btn")
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
                        modifier = Modifier.testTag("quiz_history_sync_btn")
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isFetchingFirestoreHistory,
            onRefresh = { viewModel.refreshStudentPastHistoryFromFirestore() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Cloud Firestore Sync Status Banner
                item {
                    QuizHistoryFirestoreSyncBanner(
                        isCloudConnected = isCloudConnected,
                        isFetching = isFetchingFirestoreHistory,
                        lastSyncedTimestamp = firestoreHistoryLastSynced,
                        attemptsCount = studentAttempts.size,
                        onSyncNow = { viewModel.refreshStudentPastHistoryFromFirestore() }
                    )
                }

                // 2. High-Level Performance KPI Tiles
                if (studentAttempts.isNotEmpty()) {
                    item {
                        QuizHistorySummaryTiles(
                            totalQuizzes = studentAttempts.size,
                            avgScore = avgScore,
                            passedCount = passedCount,
                            failedCount = failedCount,
                            totalTimeSeconds = totalTimeSeconds
                        )
                    }
                }

                // 3. Search Bar, Result Filters, Category Chips & Sort Order
                if (studentAttempts.isNotEmpty()) {
                    item {
                        QuizHistoryFilterBar(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            selectedResultFilter = selectedResultFilter,
                            onResultFilterChange = { selectedResultFilter = it },
                            selectedCategoryFilter = selectedCategoryFilter,
                            onCategoryFilterChange = { selectedCategoryFilter = it },
                            selectedDifficultyFilter = selectedDifficultyFilter,
                            onDifficultyFilterChange = { selectedDifficultyFilter = it },
                            selectedSortOrder = selectedSortOrder,
                            onSortOrderChange = { selectedSortOrder = it },
                            availableCategories = availableCategories,
                            totalCount = studentAttempts.size,
                            passedCount = passedCount,
                            failedCount = failedCount,
                            quizSetMap = quizSetMap,
                            studentAttempts = studentAttempts
                        )
                    }
                }

                // 4. Past Quizzes List Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (filteredAttempts.isEmpty() && studentAttempts.isNotEmpty()) {
                                "No Matching Quizzes Found"
                            } else {
                                "Past Quizzes Taken (${filteredAttempts.size})"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (filteredAttempts.isNotEmpty()) {
                            Text(
                                text = selectedSortOrder.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 5. Empty State or List of Attempt Cards
                if (studentAttempts.isEmpty()) {
                    item {
                        QuizHistoryEmptyState(
                            onTakeFirstQuiz = {
                                onTakeQuiz?.invoke() ?: onBack()
                            }
                        )
                    }
                } else if (filteredAttempts.isEmpty()) {
                    item {
                        BentoCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.FilterListOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No Quizzes Match Filters",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Try adjusting your search keywords or clearing the active filters.",
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
                                    Text("Reset Filters")
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        items = filteredAttempts,
                        key = { _, attempt -> attempt.id }
                    ) { index, attempt ->
                        val targetQuiz = quizSetMap[attempt.quizSetId]
                        QuizHistoryAttemptItemCard(
                            attempt = attempt,
                            targetQuiz = targetQuiz,
                            viewModel = viewModel,
                            onRetakeQuiz = onRetakeQuiz,
                            itemIndex = index + 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cloud Firestore Sync Banner
 */
@Composable
private fun QuizHistoryFirestoreSyncBanner(
    isCloudConnected: Boolean,
    isFetching: Boolean,
    lastSyncedTimestamp: Long,
    attemptsCount: Int,
    onSyncNow: () -> Unit
) {
    val relativeTimeText = remember(lastSyncedTimestamp) {
        val diff = System.currentTimeMillis() - lastSyncedTimestamp
        when {
            diff < 60_000L -> "Just now"
            diff < 3600_000L -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
            else -> SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(lastSyncedTimestamp))
        }
    }

    Surface(
        color = if (isCloudConnected) BentoEmerald.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isCloudConnected) BentoEmerald.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isCloudConnected) BentoEmerald.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCloudConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (isCloudConnected) BentoEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Cloud Firestore History",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            color = BentoEmerald.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "quiz_attempts",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BentoEmerald,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "$attemptsCount attempts synced • Last update: $relativeTimeText",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TextButton(
                onClick = onSyncNow,
                enabled = !isFetching,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                if (isFetching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = BentoPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text("Sync Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * High-Level Performance KPI Tiles
 */
@Composable
private fun QuizHistorySummaryTiles(
    totalQuizzes: Int,
    avgScore: Float,
    passedCount: Int,
    failedCount: Int,
    totalTimeSeconds: Int
) {
    val passRate = if (totalQuizzes > 0) (passedCount.toFloat() / totalQuizzes.toFloat()) * 100f else 0f
    val totalMins = totalTimeSeconds / 60

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BentoStatTile(
                label = "Quizzes Completed",
                value = "$totalQuizzes",
                icon = Icons.AutoMirrored.Filled.FactCheck,
                accentColor = BentoPrimary,
                subtitle = "$passedCount passed, $failedCount practice",
                modifier = Modifier.weight(1f)
            )

            BentoStatTile(
                label = "Average Score",
                value = "${String.format(Locale.getDefault(), "%.1f", avgScore)}%",
                icon = Icons.Default.Stars,
                accentColor = if (avgScore >= 75f) BentoEmerald else BentoAmber,
                subtitle = when {
                    avgScore >= 90f -> "Grade: A (Mastery)"
                    avgScore >= 80f -> "Grade: B (Proficient)"
                    avgScore >= 70f -> "Grade: C (Good)"
                    avgScore >= 60f -> "Grade: D (Passing)"
                    else -> "Grade: F (Needs Focus)"
                },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BentoStatTile(
                label = "Pass Rate",
                value = "${String.format(Locale.getDefault(), "%.0f", passRate)}%",
                icon = Icons.Default.CheckCircle,
                accentColor = BentoEmerald,
                subtitle = "$passedCount of $totalQuizzes passed",
                modifier = Modifier.weight(1f)
            )

            BentoStatTile(
                label = "Total Study Time",
                value = if (totalMins > 60) "${totalMins / 60}h ${totalMins % 60}m" else "${totalMins}m",
                icon = Icons.Default.Timer,
                accentColor = BentoViolet,
                subtitle = "Active assessment duration",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Filter and Search Controls Bar
 */
@Composable
private fun QuizHistoryFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedResultFilter: String,
    onResultFilterChange: (String) -> Unit,
    selectedCategoryFilter: String,
    onCategoryFilterChange: (String) -> Unit,
    selectedDifficultyFilter: String,
    onDifficultyFilterChange: (String) -> Unit,
    selectedSortOrder: QuizHistorySortOrder,
    onSortOrderChange: (QuizHistorySortOrder) -> Unit,
    availableCategories: List<String>,
    totalCount: Int,
    passedCount: Int,
    failedCount: Int,
    quizSetMap: Map<String, QuizSetEntity>,
    studentAttempts: List<QuizAttemptEntity>
) {
    var showSortDropdown by remember { mutableStateOf(false) }

    val easyCount = remember(studentAttempts, quizSetMap) {
        studentAttempts.count { (quizSetMap[it.quizSetId]?.difficulty ?: "Medium").equals("Easy", ignoreCase = true) }
    }
    val mediumCount = remember(studentAttempts, quizSetMap) {
        studentAttempts.count { (quizSetMap[it.quizSetId]?.difficulty ?: "Medium").equals("Medium", ignoreCase = true) }
    }
    val hardCount = remember(studentAttempts, quizSetMap) {
        studentAttempts.count { (quizSetMap[it.quizSetId]?.difficulty ?: "Medium").equals("Hard", ignoreCase = true) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Search Input & Sort Button
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
                    .testTag("quiz_history_search_input"),
                placeholder = { Text("Search quiz title or topic...", style = MaterialTheme.typography.bodyMedium) },
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
                    onClick = { showSortDropdown = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort past quizzes"
                    )
                }

                DropdownMenu(
                    expanded = showSortDropdown,
                    onDismissRequest = { showSortDropdown = false }
                ) {
                    QuizHistorySortOrder.values().forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.label) },
                            onClick = {
                                onSortOrderChange(order)
                                showSortDropdown = false
                            },
                            leadingIcon = {
                                if (selectedSortOrder == order) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = BentoPrimary)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Result Filter Chips (All, Passed, Needs Practice)
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

        // Difficulty Chips (All, Easy, Medium, Hard)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedDifficultyFilter == "ALL",
                onClick = { onDifficultyFilterChange("ALL") },
                label = { Text("All Levels") },
                shape = RoundedCornerShape(12.dp)
            )

            FilterChip(
                selected = selectedDifficultyFilter.equals("Easy", ignoreCase = true),
                onClick = {
                    onDifficultyFilterChange(if (selectedDifficultyFilter.equals("Easy", ignoreCase = true)) "ALL" else "Easy")
                },
                label = { Text("Easy ($easyCount)") },
                shape = RoundedCornerShape(12.dp)
            )

            FilterChip(
                selected = selectedDifficultyFilter.equals("Medium", ignoreCase = true),
                onClick = {
                    onDifficultyFilterChange(if (selectedDifficultyFilter.equals("Medium", ignoreCase = true)) "ALL" else "Medium")
                },
                label = { Text("Medium ($mediumCount)") },
                shape = RoundedCornerShape(12.dp)
            )

            FilterChip(
                selected = selectedDifficultyFilter.equals("Hard", ignoreCase = true),
                onClick = {
                    onDifficultyFilterChange(if (selectedDifficultyFilter.equals("Hard", ignoreCase = true)) "ALL" else "Hard")
                },
                label = { Text("Hard ($hardCount)") },
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Category / Subject Chips
        if (availableCategories.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subject:",
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

                availableCategories.forEach { cat ->
                    val isSelected = selectedCategoryFilter.equals(cat, ignoreCase = true)
                    SuggestionChip(
                        onClick = { onCategoryFilterChange(cat) },
                        label = { Text(cat) },
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
 * Individual Past Quiz Attempt Card
 * Displays the quiz title, scores, and completion dates prominently.
 */
@Composable
private fun QuizHistoryAttemptItemCard(
    attempt: QuizAttemptEntity,
    targetQuiz: QuizSetEntity?,
    viewModel: QuizViewModel,
    onRetakeQuiz: ((QuizSetEntity) -> Unit)?,
    itemIndex: Int
) {
    var isExpanded by remember { mutableStateOf(false) }
    var questionsList by remember { mutableStateOf<List<QuestionEntity>>(emptyList()) }
    var isLoadingQuestions by remember { mutableStateOf(false) }

    val userAnswersMap = remember(attempt.userAnswersJson) {
        parseUserAnswers(attempt.userAnswersJson)
    }

    val isPassed = attempt.percentage >= 60f
    val mins = attempt.timeSpentSeconds / 60
    val secs = attempt.timeSpentSeconds % 60
    val durationText = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val formattedCompletionDate = remember(attempt.completedAt) { dateFormat.format(Date(attempt.completedAt)) }

    val relativeTimeTag = remember(attempt.completedAt) {
        val diff = System.currentTimeMillis() - attempt.completedAt
        when {
            diff < 3600_000L -> "Today"
            diff < 86400_000L -> "Today"
            diff < 172800_000L -> "Yesterday"
            else -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
        }
    }

    // Lazy-load questions when expanded
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

    BentoCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quiz_history_card_${attempt.id}"),
        cornerRadius = 20.dp,
        borderColor = if (isPassed) BentoEmerald.copy(alpha = 0.35f) else BentoRose.copy(alpha = 0.35f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Category Badge, Difficulty Badge, Firestore Tag & Completion Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BentoPillTag(
                        text = "#$itemIndex",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        icon = Icons.Default.Tag
                    )

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
                    BentoPillTag(
                        text = difficulty,
                        containerColor = diffBg,
                        contentColor = diffColor,
                        icon = when (difficulty.lowercase()) {
                            "easy" -> Icons.Default.CheckCircle
                            "hard" -> Icons.Default.Bolt
                            else -> Icons.Default.Speed
                        }
                    )
                }

                // Completion Date Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Completion Date",
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formattedCompletionDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("quiz_history_date_${attempt.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quiz Title & Primary Score Layout
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
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Score: ${attempt.score} / ${attempt.totalQuestions}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("quiz_history_score_${attempt.id}")
                        )
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = relativeTimeTag,
                            style = MaterialTheme.typography.bodySmall,
                            color = BentoPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Score Badge Pill
                Surface(
                    color = if (isPassed) BentoEmerald.copy(alpha = 0.15f) else BentoRose.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isPassed) BentoEmerald.copy(alpha = 0.35f) else BentoRose.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${String.format(Locale.getDefault(), "%.0f", attempt.percentage)}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isPassed) BentoEmerald else BentoRose
                        )
                        Text(
                            text = if (isPassed) "PASSED" else "NEEDS WORK",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = if (isPassed) BentoEmerald else BentoRose
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Score Linear Progress Indicator
            LinearProgressIndicator(
                progress = { (attempt.percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (isPassed) BentoEmerald else BentoRose,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Review Questions Expansion & Retake Quiz
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { isExpanded = !isExpanded },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isExpanded) "Hide Questions" else "Review Questions",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (targetQuiz != null && onRetakeQuiz != null) {
                    FilledTonalButton(
                        onClick = { onRetakeQuiz(targetQuiz) },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = BentoPrimary.copy(alpha = 0.12f),
                            contentColor = BentoPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Retake Quiz",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Expandable Question Review Section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    if (isLoadingQuestions) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    } else if (evaluations.isEmpty()) {
                        Text(
                            text = "Question review details unavailable for this attempt.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        Text(
                            text = "Detailed Question Breakdown:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        evaluations.forEach { evaluation ->
                            QuizHistoryQuestionRow(evaluation = evaluation)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Detailed Question Row showing chosen vs correct option
 */
@Composable
private fun QuizHistoryQuestionRow(evaluation: QuestionEvaluation) {
    val q = evaluation.question
    val options = listOf(q.optionA, q.optionB, q.optionC, q.optionD)

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (evaluation.isCorrect) BentoEmerald.copy(alpha = 0.3f) else BentoRose.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (evaluation.isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (evaluation.isCorrect) BentoEmerald else BentoRose,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Q${evaluation.questionIndex + 1}. ${q.questionText}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            val chosenText = options.getOrNull(evaluation.selectedOptionIndex) ?: "Skipped"
            val correctText = options.getOrNull(q.correctOptionIndex) ?: "N/A"

            Text(
                text = "Your Answer: $chosenText",
                fontSize = 12.sp,
                color = if (evaluation.isCorrect) BentoEmerald else BentoRose,
                fontWeight = FontWeight.Medium
            )

            if (!evaluation.isCorrect) {
                Text(
                    text = "Correct Answer: $correctText",
                    fontSize = 12.sp,
                    color = BentoEmerald,
                    fontWeight = FontWeight.Bold
                )
            }

            if (q.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Explanation: ${q.explanation}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

/**
 * Empty State when no quizzes have been completed yet
 */
@Composable
private fun QuizHistoryEmptyState(
    onTakeFirstQuiz: () -> Unit
) {
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(BentoPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HistoryEdu,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = BentoPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Quiz History Found",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "You haven't taken any quizzes yet. Take your first quiz to track your scores, completion dates, and study progress synced with Cloud Firestore.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onTakeFirstQuiz,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                modifier = Modifier.testTag("quiz_history_take_first_btn")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Take Your First Quiz", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun parseUserAnswers(json: String): Map<String, Int> {
    if (json.isBlank() || json == "{}") return emptyMap()
    val map = mutableMapOf<String, Int>()
    try {
        val obj = JSONObject(json)
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = obj.getInt(key)
        }
    } catch (_: Exception) {
    }
    return map
}
