package com.example.ui.student.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.QuizAttemptEntity
import com.example.ui.components.BentoCard
import com.example.ui.components.BentoPillTag
import com.example.ui.theme.*
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.*
import java.text.SimpleDateFormat
import java.util.*

enum class TrendTimeRange(val label: String) {
    ALL("All Time"),
    LAST_5("Recent 5"),
    LAST_10("Recent 10")
}

enum class TrendMetric(val label: String) {
    PERCENTAGE("Accuracy (%)"),
    POINTS("Points (pts)")
}

/**
 * Performance Trend Chart for the Student Profile.
 * Uses the Compose-Charts data visualization library to plot score trajectory over time.
 * Supports interactive inspection, time range filtering, metric switching, and rich summary metrics.
 */
@Composable
fun StudentPerformanceTrendCard(
    attempts: List<QuizAttemptEntity>,
    modifier: Modifier = Modifier,
    title: String = "Performance Trend Over Time",
    subtitle: String = "Score history & learning trajectory powered by Compose-Charts",
    onTakeQuiz: (() -> Unit)? = null,
    onAttemptClick: ((QuizAttemptEntity) -> Unit)? = null
) {
    // Sort attempts chronologically (oldest to newest) to represent progress over time
    val chronologicalAttempts = remember(attempts) {
        attempts.sortedBy { it.completedAt }
    }

    var selectedTimeRange by remember { mutableStateOf(TrendTimeRange.ALL) }
    var selectedMetric by remember { mutableStateOf(TrendMetric.PERCENTAGE) }

    // Filter attempts by time range
    val filteredAttempts = remember(chronologicalAttempts, selectedTimeRange) {
        when (selectedTimeRange) {
            TrendTimeRange.ALL -> chronologicalAttempts
            TrendTimeRange.LAST_5 -> chronologicalAttempts.takeLast(5)
            TrendTimeRange.LAST_10 -> chronologicalAttempts.takeLast(10)
        }
    }

    var selectedAttemptIndex by remember(filteredAttempts) {
        mutableStateOf(if (filteredAttempts.isNotEmpty()) filteredAttempts.lastIndex else null)
    }

    val selectedAttempt = remember(filteredAttempts, selectedAttemptIndex) {
        selectedAttemptIndex?.let { index ->
            if (index in filteredAttempts.indices) filteredAttempts[index] else null
        }
    }

    BentoCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("student_performance_trend_card"),
        cornerRadius = 22.dp,
        padding = 16.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Title & Metric Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .clip(CircleShape)
                            .background(BentoPrimary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = "Performance Trend",
                            tint = BentoPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Data Visualization Library Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BentoViolet.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, BentoViolet.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = BentoViolet,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Compose-Charts",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoViolet
                        )
                    }
                }
            }

            // Filter Pills Row (Time Range + Metric Selector)
            if (chronologicalAttempts.size >= 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time Range Selector
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        TrendTimeRange.values().forEach { range ->
                            val isSelected = selectedTimeRange == range
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedTimeRange = range }
                                    .testTag("trend_filter_${range.name.lowercase()}")
                            ) {
                                Text(
                                    text = range.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Metric Toggle (Percentage vs Points)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        TrendMetric.values().forEach { metric ->
                            val isSelected = selectedMetric == metric
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) BentoPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedMetric = metric }
                                    .testTag("trend_metric_${metric.name.lowercase()}")
                            ) {
                                Text(
                                    text = if (metric == TrendMetric.PERCENTAGE) "%" else "pts",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BentoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Body: Empty State / Single Milestone / Full Interactive Line Chart
            when {
                chronologicalAttempts.isEmpty() -> {
                    // Empty state when no quizzes have been attempted yet
                    EmptyTrendState(onTakeQuiz = onTakeQuiz)
                }
                chronologicalAttempts.size == 1 -> {
                    // Single attempt milestone state
                    SingleAttemptMilestoneCard(
                        attempt = chronologicalAttempts.first(),
                        onTakeQuiz = onTakeQuiz
                    )
                }
                else -> {
                    // Full Performance Trend Visualizer with Compose-Charts
                    PerformanceTrendChartContent(
                        attempts = filteredAttempts,
                        selectedMetric = selectedMetric,
                        selectedAttemptIndex = selectedAttemptIndex,
                        onSelectAttempt = { index -> selectedAttemptIndex = index },
                        onAttemptClick = onAttemptClick
                    )
                }
            }
        }
    }
}

/**
 * Renders the multi-point LineChart using the Compose-Charts library alongside trajectory stats.
 */
@Composable
private fun PerformanceTrendChartContent(
    attempts: List<QuizAttemptEntity>,
    selectedMetric: TrendMetric,
    selectedAttemptIndex: Int?,
    onSelectAttempt: (Int) -> Unit,
    onAttemptClick: ((QuizAttemptEntity) -> Unit)?
) {
    val isDark = MaterialTheme.colorScheme.surface.let { it.red < 0.5f }

    // Summary Calculations
    val firstScore = if (selectedMetric == TrendMetric.PERCENTAGE) attempts.first().percentage else attempts.first().score.toFloat()
    val latestScore = if (selectedMetric == TrendMetric.PERCENTAGE) attempts.last().percentage else attempts.last().score.toFloat()
    val trajectoryDelta = latestScore - firstScore
    val bestScore = if (selectedMetric == TrendMetric.PERCENTAGE) attempts.maxOf { it.percentage } else attempts.maxOf { it.score.toFloat() }
    val avgScore = if (selectedMetric == TrendMetric.PERCENTAGE) attempts.map { it.percentage }.average().toFloat() else attempts.map { it.score }.average().toFloat()

    // Key Metric Summary Tiles
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Trajectory / Growth Pill
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (trajectoryDelta >= 0) BentoEmerald.copy(alpha = 0.12f) else BentoRose.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, if (trajectoryDelta >= 0) BentoEmerald.copy(alpha = 0.3f) else BentoRose.copy(alpha = 0.3f)),
            modifier = Modifier.weight(1f).testTag("trend_stat_trajectory")
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = if (trajectoryDelta >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (trajectoryDelta >= 0) BentoEmerald else BentoRose,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Trajectory",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${if (trajectoryDelta > 0) "+" else ""}${String.format(Locale.US, "%.1f", trajectoryDelta)}${if (selectedMetric == TrendMetric.PERCENTAGE) "%" else " pts"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (trajectoryDelta >= 0) BentoEmerald else BentoRose
                )
            }
        }

        // High Score
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = BentoAmber.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, BentoAmber.copy(alpha = 0.3f)),
            modifier = Modifier.weight(1f).testTag("trend_stat_best")
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Personal Best",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${String.format(Locale.US, "%.0f", bestScore)}${if (selectedMetric == TrendMetric.PERCENTAGE) "%" else " pts"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BentoAmber
                )
            }
        }

        // Average
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = BentoViolet.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, BentoViolet.copy(alpha = 0.3f)),
            modifier = Modifier.weight(1f).testTag("trend_stat_average")
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Mastery Avg",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${String.format(Locale.US, "%.1f", avgScore)}${if (selectedMetric == TrendMetric.PERCENTAGE) "%" else " pts"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BentoViolet
                )
            }
        }
    }

    // Prepare chart values and labels
    val chartValues = remember(attempts, selectedMetric) {
        attempts.map { attempt ->
            if (selectedMetric == TrendMetric.PERCENTAGE) {
                attempt.percentage.toDouble().coerceIn(0.0, 100.0)
            } else {
                attempt.score.toDouble().coerceAtLeast(0.0)
            }
        }
    }

    val dateFormatter = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val chartLabels = remember(attempts) {
        attempts.mapIndexed { index, attempt ->
            if (attempts.size <= 6) {
                dateFormatter.format(Date(attempt.completedAt))
            } else {
                "T${index + 1}"
            }
        }
    }

    val minChartValue = if (selectedMetric == TrendMetric.PERCENTAGE) 0.0 else (chartValues.minOrNull()?.times(0.8) ?: 0.0)
    val maxChartValue = if (selectedMetric == TrendMetric.PERCENTAGE) 100.0 else ((chartValues.maxOrNull()?.times(1.2)) ?: 50.0).coerceAtLeast(10.0)

    // Construct the Line series using Compose-Charts
    val lineSeries = remember(chartValues, selectedMetric, isDark) {
        listOf(
            Line(
                label = if (selectedMetric == TrendMetric.PERCENTAGE) "Accuracy (%)" else "Score Points",
                values = chartValues,
                color = SolidColor(BentoPrimary),
                firstGradientFillColor = BentoPrimary.copy(alpha = 0.35f),
                secondGradientFillColor = BentoPrimary.copy(alpha = 0.03f),
                drawStyle = DrawStyle.Stroke(width = 3.dp),
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
                        if (selectedMetric == TrendMetric.PERCENTAGE) {
                            "${String.format(Locale.US, "%.0f", value)}%"
                        } else {
                            "${String.format(Locale.US, "%.0f", value)} pts"
                        }
                    }
                )
            )
        )
    }

    // Interactive Compose-Charts LineChart Container
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(top = 16.dp, bottom = 12.dp, start = 8.dp, end = 12.dp)
            .testTag("compose_charts_line_chart_container")
    ) {
        LineChart(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .testTag("compose_charts_line_chart"),
            data = lineSeries,
            minValue = minChartValue,
            maxValue = maxChartValue,
            labelProperties = LabelProperties(
                enabled = true,
                labels = chartLabels,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            ),
            indicatorProperties = HorizontalIndicatorProperties(
                enabled = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Normal
                ),
                count = IndicatorCount.CountBased(5),
                contentBuilder = { value ->
                    if (selectedMetric == TrendMetric.PERCENTAGE) {
                        "${value.toInt()}%"
                    } else {
                        "${value.toInt()}"
                    }
                }
            ),
            gridProperties = GridProperties(
                enabled = true,
                xAxisProperties = GridProperties.AxisProperties(
                    enabled = true,
                    color = SolidColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                    thickness = 1.dp
                ),
                yAxisProperties = GridProperties.AxisProperties(
                    enabled = true,
                    color = SolidColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                    thickness = 1.dp
                )
            ),
            dividerProperties = DividerProperties(
                enabled = true,
                xAxisProperties = LineProperties(
                    enabled = true,
                    color = SolidColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                    thickness = 1.dp
                ),
                yAxisProperties = LineProperties(
                    enabled = true,
                    color = SolidColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                    thickness = 1.dp
                )
            ),
            labelHelperProperties = LabelHelperProperties(enabled = false)
        )
    }

    // Interactive Test Points Chip Browser
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Tap a test point to inspect details:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            attempts.forEachIndexed { index, attempt ->
                val isSelected = selectedAttemptIndex == index
                val scorePercent = attempt.percentage.toInt()

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        isSelected -> BentoPrimary
                        scorePercent >= 80 -> BentoEmerald.copy(alpha = 0.12f)
                        scorePercent >= 60 -> BentoAmber.copy(alpha = 0.12f)
                        else -> BentoRose.copy(alpha = 0.12f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) BentoPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectAttempt(index) }
                        .testTag("trend_attempt_chip_$index")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "#${index + 1}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${attempt.score}/${attempt.totalQuestions} ($scorePercent%)",
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                isSelected -> Color.White
                                scorePercent >= 80 -> BentoEmerald
                                scorePercent >= 60 -> BentoAmber
                                else -> BentoRose
                            }
                        )
                    }
                }
            }
        }
    }

    // Selected Point Details Card
    selectedAttemptIndex?.let { idx ->
        if (idx in attempts.indices) {
            val inspectedAttempt = attempts[idx]
            InspectedAttemptCard(
                attempt = inspectedAttempt,
                testIndex = idx + 1,
                onClick = { onAttemptClick?.invoke(inspectedAttempt) }
            )
        }
    }
}

/**
 * Inspection banner card highlighting the clicked data point from the performance chart.
 */
@Composable
private fun InspectedAttemptCard(
    attempt: QuizAttemptEntity,
    testIndex: Int,
    onClick: () -> Unit
) {
    val fullDate = remember(attempt.completedAt) {
        SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(attempt.completedAt))
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, BentoPrimary.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("trend_inspected_attempt_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                attempt.percentage >= 80f -> BentoEmerald.copy(alpha = 0.15f)
                                attempt.percentage >= 60f -> BentoAmber.copy(alpha = 0.15f)
                                else -> BentoRose.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$testIndex",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = when {
                            attempt.percentage >= 80f -> BentoEmerald
                            attempt.percentage >= 60f -> BentoAmber
                            else -> BentoRose
                        }
                    )
                }

                Column {
                    Text(
                        text = attempt.quizTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = attempt.categoryName,
                            style = MaterialTheme.typography.labelSmall,
                            color = BentoPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = fullDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Score & Status Pill
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format(Locale.US, "%.0f", attempt.percentage)}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        attempt.percentage >= 80f -> BentoEmerald
                        attempt.percentage >= 60f -> BentoAmber
                        else -> BentoRose
                    }
                )
                Text(
                    text = "${attempt.score}/${attempt.totalQuestions} pts",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Single Attempt Milestone Card shown when student has completed exactly 1 quiz.
 */
@Composable
private fun SingleAttemptMilestoneCard(
    attempt: QuizAttemptEntity,
    onTakeQuiz: (() -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, BentoPrimary.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth().testTag("single_attempt_milestone_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(BentoPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = BentoPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "First Milestone Recorded!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "You scored ${String.format(Locale.US, "%.0f", attempt.percentage)}% on \"${attempt.quizTitle}\". Take one more quiz to unlock your continuous multi-point score trajectory trendline!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (onTakeQuiz != null) {
                Button(
                    onClick = onTakeQuiz,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                    modifier = Modifier.testTag("trend_take_second_quiz_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Take Another Quiz to Track Trend", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Empty state shown when 0 quizzes have been attempted yet.
 */
@Composable
private fun EmptyTrendState(
    onTakeQuiz: (() -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth().testTag("empty_trend_state")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BentoViolet.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = BentoViolet,
                    modifier = Modifier.size(26.dp)
                )
            }

            Text(
                text = "No Performance History Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Complete practice quizzes and assessments to generate an interactive score history trendline on your profile.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (onTakeQuiz != null) {
                Spacer(modifier = Modifier.height(4.dp))
                FilledTonalButton(
                    onClick = onTakeQuiz,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("trend_empty_start_quiz_btn")
                ) {
                    Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Browse Quizzes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
