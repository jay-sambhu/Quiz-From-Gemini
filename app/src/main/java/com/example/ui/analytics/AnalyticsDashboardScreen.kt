package com.example.ui.analytics

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.QuizAttemptEntity
import com.example.data.local.entities.UserEntity
import com.example.data.model.UserRole
import com.example.ui.QuizViewModel
import com.example.ui.components.BentoCard
import com.example.ui.components.BentoPillTag
import com.example.ui.components.BentoStatTile
import com.example.ui.theme.*
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AnalyticsMetric(val displayName: String, val unit: String) {
    ACCURACY("Accuracy", "%"),
    SCORE("Score Points", "pts"),
    TIME("Completion Time", "s")
}

enum class AnalyticsTimeRange(val displayName: String, val days: Int) {
    WEEK("7 Days", 7),
    MONTH("30 Days", 30),
    QUARTER("90 Days", 90),
    ALL("All Time", 3650)
}

enum class AnalyticsChartType(val displayName: String) {
    TREND_LINE("Performance Trend"),
    CATEGORY_MASTERY("Subject Mastery"),
    SCORE_DISTRIBUTION("Score Brackets")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboardScreen(
    viewModel: QuizViewModel,
    onNavigateBack: (() -> Unit)? = null,
    onTakeQuiz: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allAttempts by viewModel.allAttempts.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val isCloudConnected by viewModel.isCloudConnected.collectAsState()
    val isFetchingFirestoreAnalytics by viewModel.isFetchingFirestoreAnalytics.collectAsState()
    val analyticsLastSyncedTimestamp by viewModel.analyticsLastSyncedTimestamp.collectAsState()
    val analyticsSyncMessage by viewModel.analyticsSyncMessage.collectAsState()

    // Filters state
    var selectedStudentId by remember {
        mutableStateOf<String?>(
            if (currentUser?.role == UserRole.STUDENT) currentUser?.id else null
        )
    }
    var selectedMetric by remember { mutableStateOf(AnalyticsMetric.ACCURACY) }
    var selectedTimeRange by remember { mutableStateOf(AnalyticsTimeRange.ALL) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedChartType by remember { mutableStateOf(AnalyticsChartType.TREND_LINE) }
    var selectedAttemptForInspection by remember { mutableStateOf<QuizAttemptEntity?>(null) }
    var showBenchmarkComparison by remember { mutableStateOf(true) }

    // Fetch fresh attempts directly from Firestore on launch
    LaunchedEffect(Unit) {
        viewModel.refreshAnalyticsFromFirestore(selectedStudentId)
    }

    // List of student users for filtering
    val studentUsers = remember(allUsers) {
        allUsers.filter { it.role == UserRole.STUDENT }
    }

    // Filter attempts based on student, time range, and category
    val filteredAttempts = remember(
        allAttempts,
        selectedStudentId,
        selectedTimeRange,
        selectedCategoryId
    ) {
        val now = System.currentTimeMillis()
        val cutoffMs = now - (selectedTimeRange.days.toLong() * 24L * 60L * 60L * 1000L)

        allAttempts.filter { attempt ->
            val matchesStudent = selectedStudentId == null || attempt.studentId == selectedStudentId
            val matchesTime = selectedTimeRange == AnalyticsTimeRange.ALL || attempt.completedAt >= cutoffMs
            val matchesCategory = selectedCategoryId == null || attempt.categoryName.equals(selectedCategoryId, ignoreCase = true)
            matchesStudent && matchesTime && matchesCategory
        }.sortedBy { it.completedAt }
    }

    // Class aggregate attempts for benchmark comparison
    val classBenchmarkAttempts = remember(allAttempts, selectedTimeRange, selectedCategoryId) {
        val now = System.currentTimeMillis()
        val cutoffMs = now - (selectedTimeRange.days.toLong() * 24L * 60L * 60L * 1000L)
        allAttempts.filter { attempt ->
            val matchesTime = selectedTimeRange == AnalyticsTimeRange.ALL || attempt.completedAt >= cutoffMs
            val matchesCategory = selectedCategoryId == null || attempt.categoryName.equals(selectedCategoryId, ignoreCase = true)
            matchesTime && matchesCategory
        }.sortedBy { it.completedAt }
    }

    // Summary KPIs calculation
    val totalEvaluated = filteredAttempts.size
    val averageAccuracy = if (filteredAttempts.isNotEmpty()) {
        filteredAttempts.map { it.percentage.toDouble() }.average().toFloat()
    } else 0f

    val passRate = if (filteredAttempts.isNotEmpty()) {
        val passedCount = filteredAttempts.count { it.percentage >= 60f }
        (passedCount.toFloat() / filteredAttempts.size.toFloat()) * 100f
    } else 0f

    val averageTimeSeconds = if (filteredAttempts.isNotEmpty()) {
        filteredAttempts.map { it.timeSpentSeconds }.average().toInt()
    } else 0

    val highestScore = if (filteredAttempts.isNotEmpty()) {
        filteredAttempts.maxOf { it.percentage }
    } else 0f

    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }
    val syncTimeFormatted = remember(analyticsLastSyncedTimestamp) {
        SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(analyticsLastSyncedTimestamp))
    }

    val selectedStudentName = remember(selectedStudentId, studentUsers) {
        if (selectedStudentId == null) {
            "All Students (Class Aggregate)"
        } else {
            studentUsers.firstOrNull { it.id == selectedStudentId }?.name ?: "Selected Student"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Performance Analytics",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            BentoPillTag(
                                text = "COMPOSE-CHARTS",
                                containerColor = BentoPrimary.copy(alpha = 0.15f),
                                contentColor = BentoPrimary,
                                icon = Icons.Default.Insights
                            )
                        }
                        Text(
                            text = "Student Quiz Score Trends & Firestore Cloud Analytics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("analytics_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    // Manual Firestore Refresh Button
                    IconButton(
                        onClick = { viewModel.refreshAnalyticsFromFirestore(selectedStudentId) },
                        enabled = !isFetchingFirestoreAnalytics,
                        modifier = Modifier.testTag("analytics_firestore_refresh_button")
                    ) {
                        if (isFetchingFirestoreAnalytics) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = BentoPrimary
                            )
                        } else {
                            Icon(
                                Icons.Default.CloudSync,
                                contentDescription = "Refresh from Firestore",
                                tint = BentoPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isFetchingFirestoreAnalytics,
            onRefresh = { viewModel.refreshAnalyticsFromFirestore(selectedStudentId) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("analytics_pull_refresh_container")
        ) {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
            ) {
                // 1. Cloud Firestore Real-Time Connection Banner
                item {
                    FirestoreStatusBanner(
                        isCloudConnected = isCloudConnected,
                        lastSyncedTime = syncTimeFormatted,
                        totalAttempts = allAttempts.size,
                        isFetching = isFetchingFirestoreAnalytics,
                        syncMessage = analyticsSyncMessage,
                        onRefreshClick = { viewModel.refreshAnalyticsFromFirestore(selectedStudentId) }
                    )
                }

                // 2. Student Roster Selector (for Teachers, Admins, or comparison)
                item {
                    StudentRosterFilterCard(
                        currentUser = currentUser,
                        studentUsers = studentUsers,
                        selectedStudentId = selectedStudentId,
                        onSelectStudent = { newId ->
                            selectedStudentId = newId
                            viewModel.refreshAnalyticsFromFirestore(newId)
                        }
                    )
                }

                // 3. Interactive Filter Horizon Chips (Time Range, Subject, Metric)
                item {
                    AnalyticsFilterControlsBar(
                        selectedTimeRange = selectedTimeRange,
                        onSelectTimeRange = { selectedTimeRange = it },
                        selectedMetric = selectedMetric,
                        onSelectMetric = { selectedMetric = it },
                        selectedChartType = selectedChartType,
                        onSelectChartType = { selectedChartType = it },
                        categories = allCategories.map { it.name },
                        selectedCategory = selectedCategoryId,
                        onSelectCategory = { selectedCategoryId = it }
                    )
                }

                // 4. Bento KPI Metric Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BentoStatTile(
                            label = "Average Accuracy",
                            value = "${String.format(Locale.US, "%.1f", averageAccuracy)}%",
                            icon = if (averageAccuracy >= 70f) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            accentColor = if (averageAccuracy >= 70f) BentoEmerald else BentoAmber,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("analytics_kpi_accuracy")
                        )
                        BentoStatTile(
                            label = "Evaluated Quizzes",
                            value = "$totalEvaluated",
                            icon = Icons.Default.Quiz,
                            accentColor = BentoPrimary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("analytics_kpi_quizzes")
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BentoStatTile(
                            label = "Pass Rate",
                            value = "${String.format(Locale.US, "%.1f", passRate)}%",
                            icon = Icons.Default.CheckCircle,
                            accentColor = BentoEmerald,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("analytics_kpi_pass_rate")
                        )
                        BentoStatTile(
                            label = "Avg Pacing",
                            value = "${averageTimeSeconds / 60}m ${averageTimeSeconds % 60}s",
                            icon = Icons.Default.Timer,
                            accentColor = BentoViolet,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("analytics_kpi_pacing")
                        )
                    }
                }

                // 5. Main Visualization Card powered by 'compose-charts'
                item {
                    BentoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analytics_chart_container_card"),
                        cornerRadius = 20.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header of Chart
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = when (selectedChartType) {
                                                AnalyticsChartType.TREND_LINE -> "${selectedMetric.displayName} Trend Over Time"
                                                AnalyticsChartType.CATEGORY_MASTERY -> "Subject Mastery Breakdown"
                                                AnalyticsChartType.SCORE_DISTRIBUTION -> "Score Grade Distribution"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        BentoPillTag(
                                            text = selectedTimeRange.displayName,
                                            containerColor = BentoPrimary.copy(alpha = 0.1f),
                                            contentColor = BentoPrimary
                                        )
                                    }
                                    Text(
                                        text = "Target: $selectedStudentName",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (selectedChartType == AnalyticsChartType.TREND_LINE && selectedStudentId != null) {
                                    FilterChip(
                                        selected = showBenchmarkComparison,
                                        onClick = { showBenchmarkComparison = !showBenchmarkComparison },
                                        label = { Text("Compare Class Avg", fontSize = 11.sp) },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(BentoSecondary)
                                            )
                                        },
                                        modifier = Modifier.testTag("benchmark_toggle_chip")
                                    )
                                }
                            }

                            if (filteredAttempts.isEmpty()) {
                                // Empty state for selected filters
                                AnalyticsEmptyState(
                                    selectedStudentName = selectedStudentName,
                                    onTakeQuiz = onTakeQuiz,
                                    onRefresh = { viewModel.refreshAnalyticsFromFirestore(selectedStudentId) }
                                )
                            } else {
                                when (selectedChartType) {
                                    AnalyticsChartType.TREND_LINE -> {
                                        ComposeChartsPerformanceLineChart(
                                            attempts = filteredAttempts,
                                            benchmarkAttempts = if (showBenchmarkComparison && selectedStudentId != null) classBenchmarkAttempts else emptyList(),
                                            metric = selectedMetric,
                                            dayFormat = dayFormat,
                                            onAttemptClick = { selectedAttemptForInspection = it }
                                        )
                                    }
                                    AnalyticsChartType.CATEGORY_MASTERY -> {
                                        ComposeChartsCategoryMasteryColumnChart(
                                            attempts = filteredAttempts
                                        )
                                    }
                                    AnalyticsChartType.SCORE_DISTRIBUTION -> {
                                        ComposeChartsScoreDistributionColumnChart(
                                            attempts = filteredAttempts
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Selected Attempt Inspector Card (if user taps an attempt or data point)
                if (selectedAttemptForInspection != null) {
                    item {
                        AttemptDetailInspectionCard(
                            attempt = selectedAttemptForInspection!!,
                            dateFormat = dateFormat,
                            onClose = { selectedAttemptForInspection = null }
                        )
                    }
                }

                // 7. Chronological Session Roster Timeline (Directly matching chart test points)
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Completed Quiz History (${filteredAttempts.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Synced via Firestore",
                                style = MaterialTheme.typography.labelSmall,
                                color = BentoEmerald,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        filteredAttempts.reversed().take(10).forEach { attempt ->
                            AttemptHistoryListItem(
                                attempt = attempt,
                                isSelected = selectedAttemptForInspection?.id == attempt.id,
                                dateFormat = dateFormat,
                                onClick = {
                                    selectedAttemptForInspection = if (selectedAttemptForInspection?.id == attempt.id) null else attempt
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Cloud Firestore Real-time Connection & Sync Status Banner
 */
@Composable
fun FirestoreStatusBanner(
    isCloudConnected: Boolean,
    lastSyncedTime: String,
    totalAttempts: Int,
    isFetching: Boolean,
    syncMessage: String?,
    onRefreshClick: () -> Unit
) {
    BentoCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("firestore_status_banner"),
        backgroundColor = if (isCloudConnected) BentoEmerald.copy(alpha = 0.08f) else BentoAmber.copy(alpha = 0.08f),
        borderColor = if (isCloudConnected) BentoEmerald.copy(alpha = 0.35f) else BentoAmber.copy(alpha = 0.35f),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isCloudConnected) BentoEmerald.copy(alpha = 0.2f) else BentoAmber.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCloudConnected) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                        contentDescription = "Firestore Status",
                        tint = if (isCloudConnected) BentoEmerald else BentoAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isCloudConnected) "Cloud Firestore Connected" else "Offline Local Cache Mode",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isCloudConnected) BentoEmerald else BentoAmber
                        )
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isCloudConnected) BentoEmerald else BentoAmber)
                        )
                    }
                    Text(
                        text = "Collection: quiz_attempts • $totalAttempts cloud records • Synced $lastSyncedTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onRefreshClick,
                enabled = !isFetching,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("firestore_banner_sync_btn")
            ) {
                if (isFetching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = BentoPrimary
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Sync Cloud Firestore",
                        tint = BentoPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Filter card for selecting which student to visualize or viewing class aggregate
 */
@Composable
fun StudentRosterFilterCard(
    currentUser: UserEntity?,
    studentUsers: List<UserEntity>,
    selectedStudentId: String?,
    onSelectStudent: (String?) -> Unit
) {
    BentoCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("analytics_student_filter_card"),
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Student Scope Selection",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (currentUser?.role != UserRole.STUDENT) {
                    BentoPillTag(
                        text = "${studentUsers.size} Enrolled",
                        containerColor = BentoViolet.copy(alpha = 0.12f),
                        contentColor = BentoViolet
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // All Students Option
                FilterChip(
                    selected = selectedStudentId == null,
                    onClick = { onSelectStudent(null) },
                    label = { Text("All Students (Class)", fontSize = 12.sp, fontWeight = if (selectedStudentId == null) FontWeight.Bold else FontWeight.Normal) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Groups,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedStudentId == null) BentoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BentoPrimary.copy(alpha = 0.15f),
                        selectedLabelColor = BentoPrimary
                    ),
                    modifier = Modifier.testTag("filter_chip_all_students")
                )

                // Current Student / Individual Roster
                studentUsers.forEach { student ->
                    val isSelected = selectedStudentId == student.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectStudent(student.id) },
                        label = {
                            Text(
                                text = student.name.ifBlank { "Student ${student.id.take(4)}" },
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) BentoPrimary else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = student.name.take(1).uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BentoPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = BentoPrimary
                        ),
                        modifier = Modifier.testTag("filter_chip_student_${student.id}")
                    )
                }
            }
        }
    }
}

/**
 * Filter controls bar for time range, metric type, and chart type
 */
@Composable
fun AnalyticsFilterControlsBar(
    selectedTimeRange: AnalyticsTimeRange,
    onSelectTimeRange: (AnalyticsTimeRange) -> Unit,
    selectedMetric: AnalyticsMetric,
    onSelectMetric: (AnalyticsMetric) -> Unit,
    selectedChartType: AnalyticsChartType,
    onSelectChartType: (AnalyticsChartType) -> Unit,
    categories: List<String>,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Chart Engine & Type Selector Tabs
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("analytics_chart_type_selector")
        ) {
            AnalyticsChartType.values().forEachIndexed { index, type ->
                SegmentedButton(
                    selected = selectedChartType == type,
                    onClick = { onSelectChartType(type) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = AnalyticsChartType.values().size)
                ) {
                    Text(text = type.displayName, fontSize = 11.sp, maxLines = 1)
                }
            }
        }

        // Secondary Filters: Metric & Time Range Rows
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Metric:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            AnalyticsMetric.values().forEach { metric ->
                val isSelected = selectedMetric == metric
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectMetric(metric) },
                    label = { Text(metric.displayName, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BentoPrimary.copy(alpha = 0.12f),
                        selectedLabelColor = BentoPrimary
                    ),
                    modifier = Modifier.testTag("filter_metric_${metric.name.lowercase()}")
                )
            }

            Spacer(modifier = Modifier.width(6.dp))
            Box(modifier = Modifier.size(1.dp, 18.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "Range:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            AnalyticsTimeRange.values().forEach { range ->
                val isSelected = selectedTimeRange == range
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectTimeRange(range) },
                    label = { Text(range.displayName, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BentoViolet.copy(alpha = 0.12f),
                        selectedLabelColor = BentoViolet
                    ),
                    modifier = Modifier.testTag("filter_range_${range.name.lowercase()}")
                )
            }

            if (categories.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(1.dp, 18.dp).background(MaterialTheme.colorScheme.outlineVariant))
                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "Subject:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onSelectCategory(null) },
                    label = { Text("All", fontSize = 11.sp) }
                )

                categories.distinct().forEach { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectCategory(if (isSelected) null else cat) },
                        label = { Text(cat, fontSize = 11.sp) }
                    )
                }
            }
        }
    }
}

/**
 * Primary Performance Trend Line Chart using 'compose-charts' library
 */
@Composable
fun ComposeChartsPerformanceLineChart(
    attempts: List<QuizAttemptEntity>,
    benchmarkAttempts: List<QuizAttemptEntity>,
    metric: AnalyticsMetric,
    dayFormat: SimpleDateFormat,
    onAttemptClick: (QuizAttemptEntity) -> Unit
) {
    val labels = remember(attempts) {
        attempts.mapIndexed { index, attempt ->
            try {
                dayFormat.format(Date(attempt.completedAt))
            } catch (e: Exception) {
                "#${index + 1}"
            }
        }
    }

    val primaryLineValues = remember(attempts, metric) {
        attempts.map { attempt ->
            when (metric) {
                AnalyticsMetric.ACCURACY -> attempt.percentage.toDouble()
                AnalyticsMetric.SCORE -> attempt.score.toDouble()
                AnalyticsMetric.TIME -> attempt.timeSpentSeconds.toDouble()
            }
        }
    }

    val primaryLine = remember(primaryLineValues, metric) {
        Line(
            label = "Student ${metric.displayName}",
            values = primaryLineValues,
            color = SolidColor(BentoPrimary),
            firstGradientFillColor = BentoPrimary.copy(alpha = 0.35f),
            secondGradientFillColor = BentoPrimary.copy(alpha = 0.01f),
            drawStyle = DrawStyle.Stroke(width = 3.dp),
            curvedEdges = true,
            dotProperties = DotProperties(
                enabled = true,
                radius = 5.dp,
                color = SolidColor(BentoPrimary),
                strokeWidth = 2.dp,
                strokeColor = SolidColor(Color.White)
            ),
            popupProperties = PopupProperties(
                enabled = true,
                containerColor = BentoPrimary,
                cornerRadius = 8.dp,
                contentHorizontalPadding = 8.dp,
                contentVerticalPadding = 4.dp
            )
        )
    }

    // Benchmark line for class comparison
    val linesList = remember(primaryLine, benchmarkAttempts, metric) {
        val list = mutableListOf(primaryLine)
        if (benchmarkAttempts.isNotEmpty() && benchmarkAttempts.size >= attempts.size) {
            val benchmarkValues = attempts.indices.map { idx ->
                val subList = benchmarkAttempts.take(idx + 1)
                when (metric) {
                    AnalyticsMetric.ACCURACY -> subList.map { it.percentage.toDouble() }.average()
                    AnalyticsMetric.SCORE -> subList.map { it.score.toDouble() }.average()
                    AnalyticsMetric.TIME -> subList.map { it.timeSpentSeconds.toDouble() }.average()
                }
            }
            list.add(
                Line(
                    label = "Class Average Benchmark",
                    values = benchmarkValues,
                    color = SolidColor(BentoSecondary),
                    firstGradientFillColor = BentoSecondary.copy(alpha = 0.15f),
                    secondGradientFillColor = BentoSecondary.copy(alpha = 0.0f),
                    drawStyle = DrawStyle.Stroke(width = 2.dp),
                    curvedEdges = true,
                    dotProperties = DotProperties(
                        enabled = false
                    ),
                    popupProperties = PopupProperties(
                        enabled = true,
                        containerColor = BentoSecondary,
                        cornerRadius = 8.dp
                    )
                )
            )
        }
        list
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Legend row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(BentoPrimary)
                )
                Text("Performance Trajectory", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }

            if (linesList.size > 1) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(BentoSecondary)
                    )
                    Text("Class Average Benchmark", style = MaterialTheme.typography.labelSmall, color = BentoSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Compose-Charts LineChart Rendering
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .testTag("compose_charts_analytics_line_chart")
        ) {
            LineChart(
                data = linesList,
                modifier = Modifier.fillMaxSize(),
                labelProperties = LabelProperties(
                    enabled = true,
                    labels = labels,
                    textStyle = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ),
                indicatorProperties = HorizontalIndicatorProperties(
                    enabled = true,
                    count = IndicatorCount.CountBased(5),
                    textStyle = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ),
                gridProperties = GridProperties(
                    xAxisProperties = GridProperties.AxisProperties(
                        enabled = true,
                        color = SolidColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        thickness = 1.dp
                    ),
                    yAxisProperties = GridProperties.AxisProperties(
                        enabled = true,
                        color = SolidColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        thickness = 1.dp
                    )
                ),
                dividerProperties = DividerProperties(
                    enabled = true,
                    xAxisProperties = LineProperties(
                        enabled = true,
                        color = SolidColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                        thickness = 1.dp
                    ),
                    yAxisProperties = LineProperties(
                        enabled = true,
                        color = SolidColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                        thickness = 1.dp
                    )
                ),
                labelHelperProperties = LabelHelperProperties(enabled = false)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Tappable Quick-Select Chips for individual sessions
        Text(
            text = "Select a test point along the trajectory:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            attempts.forEachIndexed { idx, attempt ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, BentoPrimary.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .clickable { onAttemptClick(attempt) }
                        .testTag("chart_test_point_chip_$idx")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "#${idx + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoPrimary
                        )
                        Text(
                            text = "${attempt.percentage.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Subject Mastery Bar Chart using compose-charts ColumnChart
 */
@Composable
fun ComposeChartsCategoryMasteryColumnChart(
    attempts: List<QuizAttemptEntity>
) {
    val categoryStats = remember(attempts) {
        attempts.groupBy { it.categoryName.ifBlank { "General" } }
            .mapValues { entry ->
                entry.value.map { it.percentage.toDouble() }.average()
            }
    }

    val barsData = remember(categoryStats) {
        categoryStats.entries.mapIndexed { idx, (catName, avgScore) ->
            val color = when (idx % 4) {
                0 -> BentoPrimary
                1 -> BentoSecondary
                2 -> BentoViolet
                else -> BentoEmerald
            }
            Bars(
                label = catName.take(8),
                values = listOf(
                    Bars.Data(
                        label = "$catName (${String.format(Locale.US, "%.0f", avgScore)}%)",
                        value = avgScore,
                        color = SolidColor(color)
                    )
                )
            )
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Average Score (%) across curriculum subjects",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .testTag("compose_charts_category_column_chart")
        ) {
            ColumnChart(
                data = barsData,
                modifier = Modifier.fillMaxSize(),
                indicatorProperties = HorizontalIndicatorProperties(
                    enabled = true,
                    count = IndicatorCount.CountBased(5)
                ),
                gridProperties = GridProperties(
                    xAxisProperties = GridProperties.AxisProperties(
                        enabled = true,
                        color = SolidColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ),
                    yAxisProperties = GridProperties.AxisProperties(
                        enabled = true,
                        color = SolidColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    )
                ),
                labelHelperProperties = LabelHelperProperties(enabled = false)
            )
        }
    }
}

/**
 * Score Distribution Bar Chart using compose-charts ColumnChart
 */
@Composable
fun ComposeChartsScoreDistributionColumnChart(
    attempts: List<QuizAttemptEntity>
) {
    val brackets = remember(attempts) {
        val a = attempts.count { it.percentage >= 90f }
        val b = attempts.count { it.percentage in 80f..89.99f }
        val c = attempts.count { it.percentage in 70f..79.99f }
        val d = attempts.count { it.percentage in 60f..69.99f }
        val f = attempts.count { it.percentage < 60f }
        listOf(
            "A (90+)" to a.toDouble(),
            "B (80-89)" to b.toDouble(),
            "C (70-79)" to c.toDouble(),
            "D (60-69)" to d.toDouble(),
            "F (<60)" to f.toDouble()
        )
    }

    val barsData = remember(brackets) {
        brackets.map { (bracketName, count) ->
            val color = when {
                bracketName.startsWith("A") -> BentoEmerald
                bracketName.startsWith("B") -> BentoPrimary
                bracketName.startsWith("C") -> BentoSecondary
                bracketName.startsWith("D") -> BentoAmber
                else -> BentoRose
            }
            Bars(
                label = bracketName.take(6),
                values = listOf(
                    Bars.Data(
                        label = "$bracketName: ${count.toInt()} tests",
                        value = count,
                        color = SolidColor(color)
                    )
                )
            )
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Total quiz attempts categorized by grade bracket",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .testTag("compose_charts_distribution_column_chart")
        ) {
            ColumnChart(
                data = barsData,
                modifier = Modifier.fillMaxSize(),
                indicatorProperties = HorizontalIndicatorProperties(
                    enabled = true,
                    count = IndicatorCount.CountBased(4)
                ),
                gridProperties = GridProperties(
                    xAxisProperties = GridProperties.AxisProperties(
                        enabled = true,
                        color = SolidColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ),
                    yAxisProperties = GridProperties.AxisProperties(
                        enabled = true,
                        color = SolidColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    )
                ),
                labelHelperProperties = LabelHelperProperties(enabled = false)
            )
        }
    }
}

/**
 * Detailed attempt inspection card displayed when a user clicks a test point
 */
@Composable
fun AttemptDetailInspectionCard(
    attempt: QuizAttemptEntity,
    dateFormat: SimpleDateFormat,
    onClose: () -> Unit
) {
    BentoCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("analytics_attempt_inspection_card"),
        borderColor = BentoPrimary.copy(alpha = 0.5f),
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        cornerRadius = 18.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (attempt.percentage >= 60f) BentoEmerald.copy(alpha = 0.15f)
                                else BentoRose.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (attempt.percentage >= 60f) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (attempt.percentage >= 60f) BentoEmerald else BentoRose,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = attempt.quizTitle.ifBlank { "Quiz Session" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Student: ${attempt.studentName} (${attempt.studentEmail})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Score Result", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${attempt.score} / ${attempt.totalQuestions} (${String.format(Locale.US, "%.1f", attempt.percentage)}%)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (attempt.percentage >= 60f) BentoEmerald else BentoRose
                    )
                }

                Column {
                    Text("Time Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${attempt.timeSpentSeconds / 60}m ${attempt.timeSpentSeconds % 60}s",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Column {
                    Text("Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = attempt.categoryName.ifBlank { "General" },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = BentoPrimary
                    )
                }

                Column {
                    Text("Submitted", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = try { dateFormat.format(Date(attempt.completedAt)) } catch (e: Exception) { "Recent" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Historical attempt row item in the list
 */
@Composable
fun AttemptHistoryListItem(
    attempt: QuizAttemptEntity,
    isSelected: Boolean,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) BentoPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            1.dp,
            if (isSelected) BentoPrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("attempt_item_${attempt.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (attempt.percentage >= 60f) BentoEmerald.copy(alpha = 0.15f)
                            else BentoRose.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${attempt.percentage.toInt()}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (attempt.percentage >= 60f) BentoEmerald else BentoRose
                    )
                }

                Column {
                    Text(
                        text = attempt.quizTitle.ifBlank { "Quiz Session" },
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${attempt.studentName} • ${attempt.categoryName} • ${try { dateFormat.format(Date(attempt.completedAt)) } catch (e: Exception) { "" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            BentoPillTag(
                text = "${attempt.score}/${attempt.totalQuestions}",
                containerColor = BentoPrimary.copy(alpha = 0.12f),
                contentColor = BentoPrimary
            )
        }
    }
}

/**
 * Friendly empty state if no attempts exist for the current filters
 */
@Composable
fun AnalyticsEmptyState(
    selectedStudentName: String,
    onTakeQuiz: (() -> Unit)?,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 16.dp)
            .testTag("analytics_empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(BentoPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Insights,
                contentDescription = null,
                tint = BentoPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        Text(
            text = "No Quiz Attempts Recorded Yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "No completed test attempts match the selected scope ($selectedStudentName) in Cloud Firestore.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("analytics_empty_refresh_btn")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sync Firestore")
            }

            if (onTakeQuiz != null) {
                Button(
                    onClick = onTakeQuiz,
                    colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                    modifier = Modifier.testTag("analytics_empty_take_quiz_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Take a Quiz")
                }
            }
        }
    }
}
