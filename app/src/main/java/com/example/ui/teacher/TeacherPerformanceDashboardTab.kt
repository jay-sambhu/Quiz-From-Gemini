package com.example.ui.teacher

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChartDisplayMode
import com.example.data.model.ChartEngineType
import com.example.data.model.StudentPerformanceSummary
import com.example.data.model.TeacherAnalyticsOverview
import com.example.ui.QuizViewModel
import com.example.ui.components.BentoCard
import com.example.ui.components.BentoPillTag
import com.example.ui.components.BentoStatTile
import com.example.ui.components.NoTeacherAnalyticsEmptyState
import com.example.ui.teacher.components.ComposeNativeBarChart
import com.example.ui.teacher.components.RechartsBarChartView
import com.example.ui.theme.BentoAmber
import com.example.ui.theme.BentoEmerald
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoViolet

/**
 * Teacher Dashboard tab dedicated to student performance analytics and bar chart visualizations.
 * Includes interactive Recharts library bar charts via WebView with a Native Compose engine toggle,
 * key performance indicators, grade distributions, and student evaluation rosters.
 */
@Composable
fun TeacherPerformanceDashboardTab(
    viewModel: QuizViewModel,
    modifier: Modifier = Modifier,
    onSendAlertToStudent: (StudentPerformanceSummary) -> Unit = {}
) {
    val analyticsOverview by viewModel.teacherAnalyticsOverview.collectAsState()

    var selectedMode by remember { mutableStateOf(ChartDisplayMode.STUDENTS) }
    var selectedEngine by remember { mutableStateOf(ChartEngineType.RECHARTS) }
    var studentSearchQuery by remember { mutableStateOf("") }
    var selectedFilterTier by remember { mutableStateOf("ALL") } // ALL, HONORS, PASSING, ATTENTION

    val filteredStudents = remember(analyticsOverview.studentSummaries, studentSearchQuery, selectedFilterTier) {
        analyticsOverview.studentSummaries.filter { student ->
            val matchesQuery = student.studentName.contains(studentSearchQuery, ignoreCase = true) ||
                    student.studentEmail.contains(studentSearchQuery, ignoreCase = true)
            val matchesTier = when (selectedFilterTier) {
                "HONORS" -> student.averageScore >= 90f
                "PASSING" -> student.averageScore in 60f..89.9f
                "ATTENTION" -> student.averageScore < 60f
                else -> true
            }
            matchesQuery && matchesTier
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("teacher_performance_tab_content"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // High-Level KPIs Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BentoStatTile(
                    label = "Class Average",
                    value = "${String.format("%.1f", analyticsOverview.classAverageScore)}%",
                    icon = Icons.Default.TrendingUp,
                    accentColor = BentoPrimary,
                    modifier = Modifier.weight(1f)
                )
                BentoStatTile(
                    label = "Evaluated Tests",
                    value = "${analyticsOverview.totalAttemptsEvaluated}",
                    icon = Icons.Default.FactCheck,
                    accentColor = BentoViolet,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BentoStatTile(
                    label = "Overall Pass Rate",
                    value = "${String.format("%.1f", analyticsOverview.overallPassRate)}%",
                    icon = Icons.Default.CheckCircle,
                    accentColor = BentoEmerald,
                    modifier = Modifier.weight(1f)
                )
                BentoStatTile(
                    label = "Active Students",
                    value = "${analyticsOverview.totalStudentsEvaluated}",
                    icon = Icons.Default.People,
                    accentColor = BentoAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // View Controls Toolbar (Chart Mode + Engine Selection)
        item {
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp,
                padding = 12.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Row 1: Chart Mode Selector
                    Text(
                        text = "CHART METRIC",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                            onClick = { selectedMode = ChartDisplayMode.STUDENTS },
                            selected = selectedMode == ChartDisplayMode.STUDENTS,
                            label = { Text("Students", fontSize = 12.sp) },
                            icon = {
                                if (selectedMode == ChartDisplayMode.STUDENTS) {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            },
                            modifier = Modifier.testTag("mode_students_btn")
                        )
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                            onClick = { selectedMode = ChartDisplayMode.QUIZZES },
                            selected = selectedMode == ChartDisplayMode.QUIZZES,
                            label = { Text("Quizzes", fontSize = 12.sp) },
                            icon = {
                                if (selectedMode == ChartDisplayMode.QUIZZES) {
                                    Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            },
                            modifier = Modifier.testTag("mode_quizzes_btn")
                        )
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                            onClick = { selectedMode = ChartDisplayMode.DISTRIBUTION },
                            selected = selectedMode == ChartDisplayMode.DISTRIBUTION,
                            label = { Text("Grades", fontSize = 12.sp) },
                            icon = {
                                if (selectedMode == ChartDisplayMode.DISTRIBUTION) {
                                    Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            },
                            modifier = Modifier.testTag("mode_grades_btn")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 2: Engine Selector (Recharts vs Compose Native)
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
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = BentoPrimary
                            )
                            Text(
                                text = "Engine:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedEngine == ChartEngineType.RECHARTS,
                                onClick = { selectedEngine = ChartEngineType.RECHARTS },
                                label = { Text("Recharts (Web)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                modifier = Modifier.testTag("engine_recharts_chip")
                            )

                            FilterChip(
                                selected = selectedEngine == ChartEngineType.COMPOSE,
                                onClick = { selectedEngine = ChartEngineType.COMPOSE },
                                label = { Text("Native Compose", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                modifier = Modifier.testTag("engine_compose_chip")
                            )
                        }
                    }
                }
            }
        }

        // Primary Bar Chart View
        item {
            AnimatedContent(
                targetState = selectedEngine,
                label = "chartEngineTransition"
            ) { engine ->
                when (engine) {
                    ChartEngineType.RECHARTS -> {
                        RechartsBarChartView(
                            overview = analyticsOverview,
                            chartMode = selectedMode
                        )
                    }
                    ChartEngineType.COMPOSE -> {
                        ComposeNativeBarChart(
                            overview = analyticsOverview,
                            chartMode = selectedMode
                        )
                    }
                }
            }
        }

        // Section Title: Student Roster & Grade Breakdown
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Student Performance Roster",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    BentoPillTag(
                        text = "${filteredStudents.size} Students",
                        containerColor = BentoPrimary.copy(alpha = 0.12f),
                        contentColor = BentoPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar
                OutlinedTextField(
                    value = studentSearchQuery,
                    onValueChange = { studentSearchQuery = it },
                    placeholder = { Text("Search by student name or email...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (studentSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { studentSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_search_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tier Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TierFilterChip("ALL", "All", selectedFilterTier) { selectedFilterTier = it }
                    TierFilterChip("HONORS", "Honors (90%+)", selectedFilterTier) { selectedFilterTier = it }
                    TierFilterChip("PASSING", "Passing (60-89%)", selectedFilterTier) { selectedFilterTier = it }
                    TierFilterChip("ATTENTION", "< 60%", selectedFilterTier) { selectedFilterTier = it }
                }
            }
        }

        // Student Roster Items
        if (filteredStudents.isEmpty()) {
            item {
                if (analyticsOverview.studentSummaries.isEmpty()) {
                    NoTeacherAnalyticsEmptyState(
                        isCard = true,
                        testTag = "teacher_no_analytics_empty_state"
                    )
                } else {
                    BentoCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        padding = 24.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No matching students found",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Try clearing the search query or tier filter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(filteredStudents, key = { it.studentId }) { student ->
                StudentPerformanceCard(
                    student = student,
                    onSendAlert = { onSendAlertToStudent(student) }
                )
            }
        }

        // Quiz Sets Breakdown Overview
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Quiz Set Mastery Index",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(analyticsOverview.quizSummaries, key = { it.quizSetId }) { quiz ->
            QuizSetPerformanceCard(quiz = quiz)
        }
    }
}

@Composable
private fun TierFilterChip(
    key: String,
    label: String,
    selectedKey: String,
    onSelect: (String) -> Unit
) {
    FilterChip(
        selected = selectedKey == key,
        onClick = { onSelect(key) },
        label = { Text(label, fontSize = 11.sp) },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.height(32.dp)
    )
}

@Composable
private fun StudentPerformanceCard(
    student: StudentPerformanceSummary,
    onSendAlert: () -> Unit
) {
    val tierColor = when {
        student.averageScore >= 90f -> BentoEmerald
        student.averageScore >= 75f -> BentoPrimary
        student.averageScore >= 60f -> BentoAmber
        else -> Color(0xFFEF4444)
    }

    BentoCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("student_perf_card_${student.studentId}"),
        cornerRadius = 16.dp,
        padding = 14.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Student Rank & Initial Avatar
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(tierColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = student.studentName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = tierColor,
                            fontSize = 18.sp
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = student.studentName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (student.rank <= 3) {
                                val badgeText = when (student.rank) {
                                    1 -> "🥇 #1"
                                    2 -> "🥈 #2"
                                    else -> "🥉 #3"
                                }
                                Text(badgeText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            text = student.studentEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Grade Pill
                BentoPillTag(
                    text = student.gradeTier,
                    containerColor = tierColor.copy(alpha = 0.12f),
                    contentColor = tierColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Score Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Average Score: ${String.format("%.1f", student.averageScore)}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = tierColor
                )
                Text(
                    text = "${student.quizzesTaken} tests completed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { (student.averageScore / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = tierColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Highest: ${student.highestScore.toInt()}% • Pass Rate: ${student.passRate.toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconButton(
                    onClick = onSendAlert,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send alert",
                        tint = BentoPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizSetPerformanceCard(quiz: com.example.data.model.QuizPerformanceSummary) {
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        padding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quiz.quizTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${quiz.categoryName} • ${quiz.totalAttempts} submissions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format("%.1f", quiz.averageScore)}% Avg",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = BentoPrimary
                )
                Text(
                    text = "${String.format("%.1f", quiz.passRate)}% Pass",
                    style = MaterialTheme.typography.labelSmall,
                    color = BentoEmerald,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
