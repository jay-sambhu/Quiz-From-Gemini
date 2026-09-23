package com.example.ui.student.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.CategoryEntity
import com.example.data.local.entities.QuizAttemptEntity
import com.example.ui.components.BentoCard
import com.example.ui.components.BentoPillTag
import com.example.ui.theme.*
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Metric options for visual performance tracking
 */
enum class StudentPerformanceMetric(val label: String, val unit: String) {
    ACCURACY("Accuracy", "%"),
    SCORE("Raw Score", "pts"),
    TIME("Time Spent", "s")
}

/**
 * Time horizon filters for trend analysis
 */
enum class StudentTimeFilter(val label: String, val days: Int) {
    ALL("All Time", 0),
    DAYS_7("7 Days", 7),
    DAYS_30("30 Days", 30),
    DAYS_90("90 Days", 90)
}

/**
 * Chart visualization modes supported by Compose-Charts
 */
enum class StudentChartViewMode(val label: String) {
    SCORE_PROGRESSION("Score Progression"),
    SUBJECT_MASTERY("Subject Mastery"),
    GRADE_DISTRIBUTION("Grade Brackets")
}

/**
 * Comprehensive Student Performance Dashboard powered by the Compose-Charts library.
 * Visualizes quiz score trajectory over time with interactive LineChart and ColumnChart,
 * class average benchmark comparisons, metric switching, horizon filtering,
 * KPI summary tiles, attempt inspections, and real-time Firestore cloud synchronization.
 */
@Composable
fun StudentPerformanceDashboard(
    studentAttempts: List<QuizAttemptEntity>,
    allClassAttempts: List<QuizAttemptEntity> = emptyList(),
    categories: List<CategoryEntity> = emptyList(),
    isCloudConnected: Boolean = true,
    lastSyncedTime: String = "Just now",
    isFetching: Boolean = false,
    onRefreshSync: () -> Unit = {},
    onTakeQuiz: (() -> Unit)? = null,
    onRetakeQuiz: ((QuizAttemptEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Chronologically sorted student attempts (oldest to newest for trajectory over time)
    val chronologicalStudentAttempts = remember(studentAttempts) {
        studentAttempts.sortedBy { it.completedAt }
    }

    var selectedMetric by remember { mutableStateOf(StudentPerformanceMetric.ACCURACY) }
    var selectedTimeFilter by remember { mutableStateOf(StudentTimeFilter.ALL) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var selectedViewMode by remember { mutableStateOf(StudentChartViewMode.SCORE_PROGRESSION) }
    var showBenchmarkComparison by remember { mutableStateOf(true) }

    // Filter student attempts based on time range and subject category
    val filteredStudentAttempts = remember(
        chronologicalStudentAttempts,
        selectedTimeFilter,
        selectedCategoryFilter
    ) {
        val now = System.currentTimeMillis()
        val cutoffMs = now - (selectedTimeFilter.days.toLong() * 24L * 60L * 60L * 1000L)
        chronologicalStudentAttempts.filter { attempt ->
            val matchesTime = selectedTimeFilter == StudentTimeFilter.ALL || attempt.completedAt >= cutoffMs
            val matchesCategory = selectedCategoryFilter == null || attempt.categoryName.equals(selectedCategoryFilter, ignoreCase = true)
            matchesTime && matchesCategory
        }
    }

    // Filter class aggregate attempts for benchmark comparison
    val filteredClassAttempts = remember(
        allClassAttempts,
        selectedTimeFilter,
        selectedCategoryFilter
    ) {
        val now = System.currentTimeMillis()
        val cutoffMs = now - (selectedTimeFilter.days.toLong() * 24L * 60L * 60L * 1000L)
        allClassAttempts.filter { attempt ->
            val matchesTime = selectedTimeFilter == StudentTimeFilter.ALL || attempt.completedAt >= cutoffMs
            val matchesCategory = selectedCategoryFilter == null || attempt.categoryName.equals(selectedCategoryFilter, ignoreCase = true)
            matchesTime && matchesCategory
        }.sortedBy { it.completedAt }
    }

    // Selected attempt for detailed card inspection
    var selectedAttemptForInspection by remember(filteredStudentAttempts) {
        mutableStateOf(filteredStudentAttempts.lastOrNull())
    }

    // KPI Metrics Calculation
    val totalQuizzes = filteredStudentAttempts.size
    val averageAccuracy = if (filteredStudentAttempts.isNotEmpty()) {
        filteredStudentAttempts.map { it.percentage.toDouble() }.average().toFloat()
    } else 0f

    val passRate = if (filteredStudentAttempts.isNotEmpty()) {
        val passedCount = filteredStudentAttempts.count { it.percentage >= 60f }
        (passedCount.toFloat() / filteredStudentAttempts.size.toFloat()) * 100f
    } else 0f

    val highestScore = if (filteredStudentAttempts.isNotEmpty()) {
        filteredStudentAttempts.maxOf { it.percentage }
    } else 0f

    val averageTimeSeconds = if (filteredStudentAttempts.isNotEmpty()) {
        filteredStudentAttempts.map { it.timeSpentSeconds }.average().toInt()
    } else 0

    // Trajectory trend delta (comparing latest attempt vs first attempt in range)
    val scoreTrendDelta = remember(filteredStudentAttempts) {
        if (filteredStudentAttempts.size >= 2) {
            val first = filteredStudentAttempts.first().percentage
            val latest = filteredStudentAttempts.last().percentage
            latest - first
        } else 0f
    }

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("student_performance_dashboard_container"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Cloud Firestore Real-time Sync Banner
        BentoCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("student_dashboard_firestore_banner"),
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
                            contentDescription = "Cloud Status",
                            tint = if (isCloudConnected) BentoEmerald else BentoAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isCloudConnected) "Cloud Firestore Synchronized" else "Offline Local Cache",
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
                            text = "Collection: quiz_attempts • ${studentAttempts.size} records • Last updated $lastSyncedTime",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onRefreshSync,
                    enabled = !isFetching,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("student_dashboard_sync_refresh_btn")
                ) {
                    if (isFetching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
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

        // 2. High-Level Summary KPI Tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StudentKpiTile(
                label = "Average Accuracy",
                value = "${String.format(Locale.US, "%.0f", averageAccuracy)}%",
                subtext = if (filteredStudentAttempts.size >= 2) {
                    if (scoreTrendDelta >= 0) "+${String.format(Locale.US, "%.1f", scoreTrendDelta)}% trend"
                    else "${String.format(Locale.US, "%.1f", scoreTrendDelta)}% trend"
                } else "All attempts",
                valueColor = if (averageAccuracy >= 75f) BentoEmerald else if (averageAccuracy >= 60f) BentoPrimary else BentoAmber,
                icon = if (scoreTrendDelta >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                modifier = Modifier
                    .weight(1f)
                    .testTag("student_kpi_avg_accuracy")
            )

            StudentKpiTile(
                label = "Quizzes Taken",
                value = "$totalQuizzes",
                subtext = "${filteredStudentAttempts.count { it.percentage >= 60f }} passed",
                valueColor = BentoPrimary,
                icon = Icons.Default.Assessment,
                modifier = Modifier
                    .weight(1f)
                    .testTag("student_kpi_total_quizzes")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StudentKpiTile(
                label = "Pass Rate",
                value = "${String.format(Locale.US, "%.0f", passRate)}%",
                subtext = "Passing mark >= 60%",
                valueColor = if (passRate >= 70f) BentoEmerald else BentoRose,
                icon = Icons.Default.CheckCircle,
                modifier = Modifier
                    .weight(1f)
                    .testTag("student_kpi_pass_rate")
            )

            StudentKpiTile(
                label = "Avg Pacing Time",
                value = "${averageTimeSeconds / 60}m ${averageTimeSeconds % 60}s",
                subtext = "High: ${String.format(Locale.US, "%.0f", highestScore)}%",
                valueColor = BentoViolet,
                icon = Icons.Default.Timer,
                modifier = Modifier
                    .weight(1f)
                    .testTag("student_kpi_pacing_time")
            )
        }

        // 3. Interactive Filter Horizon & Chart Mode Controls Bar
        BentoCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("student_dashboard_chart_controls_card"),
            cornerRadius = 18.dp,
            padding = 14.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // View Mode Switcher: LineChart vs Category Mastery vs Grade Brackets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Visualization View",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        StudentChartViewMode.values().forEach { mode ->
                            val isSelected = selectedViewMode == mode
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier
                                    .clickable { selectedViewMode = mode }
                                    .testTag("view_mode_${mode.name.lowercase()}")
                            ) {
                                Text(
                                    text = mode.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BentoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Time Horizon Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Time Horizon",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StudentTimeFilter.values().forEach { filter ->
                            val isSelected = selectedTimeFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTimeFilter = filter },
                                label = { Text(filter.label, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BentoPrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = BentoPrimary
                                ),
                                modifier = Modifier.testTag("time_filter_${filter.name.lowercase()}")
                            )
                        }
                    }
                }

                // Metric Toggle (only when in Score Progression view)
                if (selectedViewMode == StudentChartViewMode.SCORE_PROGRESSION) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Plotted Metric",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StudentPerformanceMetric.values().forEach { metric ->
                                val isSelected = selectedMetric == metric
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedMetric = metric },
                                    label = { Text(metric.label, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BentoViolet.copy(alpha = 0.15f),
                                        selectedLabelColor = BentoViolet
                                    ),
                                    modifier = Modifier.testTag("metric_filter_${metric.name.lowercase()}")
                                )
                            }
                        }
                    }

                    // Benchmark Comparison Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = null,
                                tint = BentoSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Compare with Class Average Baseline",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = showBenchmarkComparison,
                            onCheckedChange = { showBenchmarkComparison = it },
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("benchmark_toggle_switch")
                        )
                    }
                }

                // Curriculum Category Filters
                val availableCategories = remember(categories, studentAttempts) {
                    val names = linkedSetOf<String>()
                    studentAttempts.forEach { if (it.categoryName.isNotBlank()) names.add(it.categoryName) }
                    categories.forEach { if (it.name.isNotBlank()) names.add(it.name) }
                    names.toList()
                }

                if (availableCategories.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Filter by Subject / Course:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedCategoryFilter == null,
                                onClick = { selectedCategoryFilter = null },
                                label = { Text("All Subjects", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("subject_filter_all")
                            )

                            availableCategories.forEach { catName ->
                                val isSelected = selectedCategoryFilter == catName
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategoryFilter = if (isSelected) null else catName },
                                    label = { Text(catName, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("subject_filter_${catName.lowercase().replace(" ", "_")}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Main Compose-Charts Data Visualizations Container
        if (filteredStudentAttempts.isEmpty()) {
            StudentEmptyDashboardState(
                onTakeQuiz = onTakeQuiz,
                onRefresh = onRefreshSync
            )
        } else {
            BentoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_compose_charts_card"),
                cornerRadius = 22.dp,
                padding = 16.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header for chart section
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
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(BentoPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (selectedViewMode) {
                                        StudentChartViewMode.SCORE_PROGRESSION -> Icons.Default.Timeline
                                        StudentChartViewMode.SUBJECT_MASTERY -> Icons.Default.BarChart
                                        StudentChartViewMode.GRADE_DISTRIBUTION -> Icons.Default.Equalizer
                                    },
                                    contentDescription = null,
                                    tint = BentoPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = when (selectedViewMode) {
                                        StudentChartViewMode.SCORE_PROGRESSION -> "Score Trajectory Over Time"
                                        StudentChartViewMode.SUBJECT_MASTERY -> "Subject Mastery Breakdown"
                                        StudentChartViewMode.GRADE_DISTRIBUTION -> "Score Grade Distribution"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Powered by Compose-Charts • ${filteredStudentAttempts.size} attempts plotted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        BentoPillTag(
                            text = selectedMetric.label,
                            containerColor = BentoPrimary.copy(alpha = 0.12f),
                            contentColor = BentoPrimary
                        )
                    }

                    // Render Selected Compose-Charts Visualization
                    when (selectedViewMode) {
                        StudentChartViewMode.SCORE_PROGRESSION -> {
                            StudentScoreProgressionLineChart(
                                attempts = filteredStudentAttempts,
                                classAttempts = filteredClassAttempts,
                                metric = selectedMetric,
                                showBenchmark = showBenchmarkComparison,
                                dayFormat = dayFormat,
                                onAttemptClick = { selectedAttemptForInspection = it }
                            )
                        }
                        StudentChartViewMode.SUBJECT_MASTERY -> {
                            StudentCategoryMasteryColumnChart(
                                attempts = filteredStudentAttempts
                            )
                        }
                        StudentChartViewMode.GRADE_DISTRIBUTION -> {
                            StudentScoreDistributionColumnChart(
                                attempts = filteredStudentAttempts
                            )
                        }
                    }
                }
            }

            // 5. Interactive Attempt Session Inspector Card
            selectedAttemptForInspection?.let { attempt ->
                StudentAttemptDetailInspectionCard(
                    attempt = attempt,
                    dateFormat = dateFormat,
                    onClose = { selectedAttemptForInspection = null },
                    onRetake = onRetakeQuiz
                )
            }

            // 6. Strengths, Weaknesses, and Learning Trajectory Insights
            StudentLearningTrajectoryInsightsCard(
                attempts = filteredStudentAttempts,
                averageAccuracy = averageAccuracy,
                trendDelta = scoreTrendDelta
            )
        }
    }
}

/**
 * Score Progression LineChart using ir.ehsannarmani.compose_charts.LineChart
 */
@Composable
fun StudentScoreProgressionLineChart(
    attempts: List<QuizAttemptEntity>,
    classAttempts: List<QuizAttemptEntity>,
    metric: StudentPerformanceMetric,
    showBenchmark: Boolean,
    dayFormat: SimpleDateFormat,
    onAttemptClick: (QuizAttemptEntity) -> Unit
) {
    val isDark = isSystemInDarkTheme()

    // Map student attempt values according to selected metric
    val studentValues = remember(attempts, metric) {
        attempts.map { attempt ->
            when (metric) {
                StudentPerformanceMetric.ACCURACY -> attempt.percentage.toDouble()
                StudentPerformanceMetric.SCORE -> attempt.score.toDouble()
                StudentPerformanceMetric.TIME -> attempt.timeSpentSeconds.toDouble()
            }
        }
    }

    // Dynamic X-axis labels
    val labels = remember(attempts) {
        attempts.mapIndexed { idx, att ->
            if (attempts.size <= 7) {
                try { dayFormat.format(Date(att.completedAt)) } catch (e: Exception) { "#${idx + 1}" }
            } else {
                "#${idx + 1}"
            }
        }
    }

    // Class average benchmark values
    val classBenchmarkValues = remember(classAttempts, attempts.size, metric) {
        if (classAttempts.isEmpty()) emptyList()
        else {
            val classAvg = when (metric) {
                StudentPerformanceMetric.ACCURACY -> classAttempts.map { it.percentage.toDouble() }.average()
                StudentPerformanceMetric.SCORE -> classAttempts.map { it.score.toDouble() }.average()
                StudentPerformanceMetric.TIME -> classAttempts.map { it.timeSpentSeconds.toDouble() }.average()
            }
            List(attempts.size) { classAvg }
        }
    }

    // Compose-Charts Line data series
    val linesList = remember(studentValues, classBenchmarkValues, showBenchmark, metric, isDark) {
        val list = mutableListOf<Line>()

        // 1. Student Score Trajectory Line
        list.add(
            Line(
                label = "Student Score",
                values = studentValues,
                color = SolidColor(BentoPrimary),
                firstGradientFillColor = BentoPrimary.copy(alpha = 0.35f),
                secondGradientFillColor = BentoPrimary.copy(alpha = 0.02f),
                strokeAnimationSpec = tween(durationMillis = 700),
                gradientAnimationDelay = 150,
                curvedEdges = true,
                dotProperties = DotProperties(
                    enabled = true,
                    radius = 5.dp,
                    color = SolidColor(BentoPrimary),
                    strokeWidth = 2.dp,
                    strokeColor = SolidColor(if (isDark) Color(0xFF1E293B) else Color.White)
                ),
                popupProperties = PopupProperties(
                    enabled = true,
                    containerColor = BentoPrimary,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    contentBuilder = { _, _, value ->
                        when (metric) {
                            StudentPerformanceMetric.ACCURACY -> "${String.format(Locale.US, "%.0f", value)}%"
                            StudentPerformanceMetric.SCORE -> "${String.format(Locale.US, "%.0f", value)} pts"
                            StudentPerformanceMetric.TIME -> "${String.format(Locale.US, "%.0f", value)}s"
                        }
                    }
                )
            )
        )

        // 2. Class Average Benchmark Line (dashed appearance or contrasting secondary color)
        if (showBenchmark && classBenchmarkValues.isNotEmpty()) {
            list.add(
                Line(
                    label = "Class Average Baseline",
                    values = classBenchmarkValues,
                    color = SolidColor(BentoSecondary),
                    firstGradientFillColor = Color.Transparent,
                    secondGradientFillColor = Color.Transparent,
                    strokeAnimationSpec = tween(durationMillis = 600),
                    curvedEdges = false,
                    dotProperties = DotProperties(
                        enabled = false
                    ),
                    popupProperties = PopupProperties(
                        enabled = true,
                        containerColor = BentoSecondary,
                        cornerRadius = 8.dp,
                        textStyle = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        contentBuilder = { _, _, value ->
                            "Class Avg: ${String.format(Locale.US, "%.0f", value)}${metric.unit}"
                        }
                    )
                )
            )
        }

        list
    }

    val maxChartVal = remember(studentValues, classBenchmarkValues, metric) {
        val peak = (studentValues + classBenchmarkValues).maxOrNull() ?: 100.0
        when (metric) {
            StudentPerformanceMetric.ACCURACY -> 100.0
            StudentPerformanceMetric.SCORE -> (peak * 1.15).coerceAtLeast(10.0)
            StudentPerformanceMetric.TIME -> (peak * 1.15).coerceAtLeast(60.0)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Chart legend
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
                Text(
                    text = "My Score Trajectory",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (showBenchmark && classBenchmarkValues.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(BentoSecondary)
                    )
                    Text(
                        text = "Class Average Baseline",
                        style = MaterialTheme.typography.labelSmall,
                        color = BentoSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Compose-Charts LineChart component
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(top = 16.dp, bottom = 10.dp, start = 6.dp, end = 12.dp)
                .testTag("student_compose_charts_line_chart")
        ) {
            LineChart(
                modifier = Modifier.fillMaxSize(),
                data = linesList,
                minValue = 0.0,
                maxValue = maxChartVal,
                labelProperties = LabelProperties(
                    enabled = true,
                    labels = labels,
                    textStyle = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ),
                indicatorProperties = HorizontalIndicatorProperties(
                    enabled = true,
                    count = IndicatorCount.CountBased(5),
                    textStyle = TextStyle(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    ),
                    contentBuilder = { value ->
                        "${value.toInt()}${metric.unit}"
                    }
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

        // Quick-Select Attempt Chips along the trajectory
        Text(
            text = "Tap a test point to inspect detailed questions & timing:",
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
                        .testTag("student_trajectory_chip_$idx")
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
fun StudentCategoryMasteryColumnChart(
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
                1 -> BentoEmerald
                2 -> BentoViolet
                else -> BentoSecondary
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
                .testTag("student_compose_charts_category_column_chart")
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
fun StudentScoreDistributionColumnChart(
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
                .testTag("student_compose_charts_distribution_column_chart")
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
fun StudentAttemptDetailInspectionCard(
    attempt: QuizAttemptEntity,
    dateFormat: SimpleDateFormat,
    onClose: () -> Unit,
    onRetake: ((QuizAttemptEntity) -> Unit)? = null
) {
    BentoCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("student_attempt_inspection_card"),
        borderColor = BentoPrimary.copy(alpha = 0.5f),
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
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
                            text = "Category: ${attempt.categoryName.ifBlank { "General" }}",
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
                    Text("Submitted", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = try { dateFormat.format(Date(attempt.completedAt)) } catch (e: Exception) { "Recent" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (onRetake != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { onRetake(attempt) },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("retake_inspected_quiz_btn")
                ) {
                    Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retake This Quiz to Improve Score")
                }
            }
        }
    }
}

/**
 * Insights card synthesizing strengths, weaknesses, and trajectory growth
 */
@Composable
fun StudentLearningTrajectoryInsightsCard(
    attempts: List<QuizAttemptEntity>,
    averageAccuracy: Float,
    trendDelta: Float
) {
    val bestSubject = remember(attempts) {
        attempts.groupBy { it.categoryName.ifBlank { "General" } }
            .mapValues { entry -> entry.value.map { it.percentage }.average() }
            .maxByOrNull { it.value }
    }

    val weakestSubject = remember(attempts) {
        attempts.groupBy { it.categoryName.ifBlank { "General" } }
            .mapValues { entry -> entry.value.map { it.percentage }.average() }
            .minByOrNull { it.value }
    }

    BentoCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("student_learning_insights_card"),
        cornerRadius = 18.dp,
        padding = 16.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BentoViolet.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BentoViolet, modifier = Modifier.size(18.dp))
                }
                Text(
                    text = "Personalized Learning Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Trend insight
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (trendDelta >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = if (trendDelta >= 0) BentoEmerald else BentoAmber,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (trendDelta >= 0) {
                        "Positive Score Trajectory: You've improved by +${String.format(Locale.US, "%.1f", trendDelta)}% across this period!"
                    } else {
                        "Pacing Review: Score variation of ${String.format(Locale.US, "%.1f", trendDelta)}%. Target unmastered questions to rebound."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            bestSubject?.let { (subject, score) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = BentoAmber, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Strongest Subject: $subject (avg ${String.format(Locale.US, "%.0f", score)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (weakestSubject != null && weakestSubject.key != bestSubject?.key) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.School, contentDescription = null, tint = BentoRose, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Focus Recommendation: Review ${weakestSubject.key} quizzes (avg ${String.format(Locale.US, "%.0f", weakestSubject.value)}%) to lift overall rank.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Reusable Bento KPI Stat Tile
 */
@Composable
fun StudentKpiTile(
    label: String,
    value: String,
    subtext: String,
    valueColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    BentoCard(
        modifier = modifier,
        cornerRadius = 16.dp,
        padding = 12.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = valueColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )

            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Empty state when no attempts match current filters
 */
@Composable
fun StudentEmptyDashboardState(
    onTakeQuiz: (() -> Unit)?,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 16.dp)
            .testTag("student_dashboard_empty_state"),
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
            text = "Take quizzes to visualize your learning trajectory, subject mastery, and score improvements over time with Compose-Charts.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("student_empty_refresh_btn")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sync Cloud")
            }

            if (onTakeQuiz != null) {
                Button(
                    onClick = onTakeQuiz,
                    colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                    modifier = Modifier.testTag("student_empty_take_quiz_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Take a Quiz")
                }
            }
        }
    }
}
