package com.example.ui.teacher.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChartDisplayMode
import com.example.data.model.ProgressTrendPoint
import com.example.data.model.TeacherAnalyticsOverview
import com.example.ui.components.BentoCard
import com.example.ui.components.BentoPillTag
import com.example.ui.theme.BentoEmerald
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoViolet

/**
 * Native Jetpack Compose Canvas Bar Chart companion to the Recharts visualizer.
 * Displays high-performance animated bar graphs with touch tooltips and benchmark indicators.
 */
@Composable
fun ComposeNativeBarChart(
    overview: TeacherAnalyticsOverview,
    chartMode: ChartDisplayMode,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember(chartMode) { mutableStateOf<Int?>(null) }
    var animationTrigger by remember(chartMode) { mutableStateOf(false) }

    LaunchedEffect(chartMode) {
        animationTrigger = true
    }

    val progressAnim by animateFloatAsState(
        targetValue = if (animationTrigger) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "barAnimation"
    )

    BentoCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("compose_native_bar_chart_card"),
        cornerRadius = 20.dp,
        padding = 14.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Chart Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (chartMode) {
                        ChartDisplayMode.TRENDS -> "Progress Trends & Trajectory"
                        ChartDisplayMode.STUDENTS -> "Student Average Scores"
                        ChartDisplayMode.QUIZZES -> "Quiz Scores vs Pass Rates"
                        ChartDisplayMode.DISTRIBUTION -> "Class Grade Distribution"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                BentoPillTag(
                    text = "Native Compose",
                    containerColor = BentoEmerald.copy(alpha = 0.12f),
                    contentColor = BentoEmerald,
                    icon = Icons.Default.Info
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = when (chartMode) {
                    ChartDisplayMode.TRENDS -> "Interactive Canvas • Touch any node to inspect milestone metrics"
                    else -> "Interactive Canvas • Touch any bar to inspect student breakdown"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas & Bars
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                when (chartMode) {
                    ChartDisplayMode.TRENDS -> {
                        val trends = overview.progressTrends.take(8)
                        NativeTrendLineCanvas(
                            trends = trends,
                            progress = progressAnim,
                            selectedIndex = selectedIndex,
                            onSelect = { selectedIndex = it }
                        )
                    }
                    ChartDisplayMode.STUDENTS -> {
                        val students = overview.studentSummaries.take(7)
                        NativeStudentBarCanvas(
                            students = students,
                            progress = progressAnim,
                            selectedIndex = selectedIndex,
                            onSelect = { selectedIndex = it }
                        )
                    }
                    ChartDisplayMode.QUIZZES -> {
                        val quizzes = overview.quizSummaries.take(6)
                        NativeQuizBarCanvas(
                            quizzes = quizzes,
                            progress = progressAnim,
                            selectedIndex = selectedIndex,
                            onSelect = { selectedIndex = it }
                        )
                    }
                    ChartDisplayMode.DISTRIBUTION -> {
                        val distributions = overview.gradeDistributions
                        NativeDistributionBarCanvas(
                            distributions = distributions,
                            progress = progressAnim,
                            selectedIndex = selectedIndex,
                            onSelect = { selectedIndex = it }
                        )
                    }
                }
            }

            // Selected details pill
            if (selectedIndex != null) {
                Spacer(modifier = Modifier.height(10.dp))
                SelectedBarDetailsPill(
                    overview = overview,
                    chartMode = chartMode,
                    selectedIndex = selectedIndex!!,
                    onDismiss = { selectedIndex = null }
                )
            }
        }
    }
}

@Composable
private fun NativeStudentBarCanvas(
    students: List<com.example.data.model.StudentPerformanceSummary>,
    progress: Float,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit
) {
    val items = if (students.isNotEmpty()) students else listOf(
        com.example.data.model.StudentPerformanceSummary("s1", "Aashish", "aashish@edu", 3, 95f, 100f, 100f, 15),
        com.example.data.model.StudentPerformanceSummary("s2", "Sophia", "sophia@edu", 2, 88f, 92f, 100f, 12),
        com.example.data.model.StudentPerformanceSummary("s3", "Marcus", "marcus@edu", 2, 78f, 85f, 100f, 10),
        com.example.data.model.StudentPerformanceSummary("s4", "Elena", "elena@edu", 3, 84f, 90f, 100f, 13),
        com.example.data.model.StudentPerformanceSummary("s5", "David", "david@edu", 1, 65f, 65f, 0f, 5)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(items) {
                        detectTapGestures { offset ->
                            val slotW = size.width / items.size
                            val clicked = (offset.x / slotW).toInt().coerceIn(0, items.size - 1)
                            onSelect(clicked)
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val slotW = w / items.size
                val barW = (slotW * 0.52f).coerceAtMost(36f)

                // Grid lines at 25%, 50%, 75%, 100%
                val gridLines = listOf(0.25f, 0.5f, 0.75f, 1.0f)
                gridLines.forEach { frac ->
                    val y = h * (1f - frac)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.2f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                // Benchmark line at 75%
                val benchY = h * (1f - 0.75f)
                drawLine(
                    color = Color(0xFF10B981).copy(alpha = 0.85f),
                    start = Offset(0f, benchY),
                    end = Offset(w, benchY),
                    strokeWidth = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
                )

                // Render Bars
                items.forEachIndexed { i, student ->
                    val x = i * slotW + (slotW - barW) / 2f
                    val scoreFraction = (student.averageScore / 100f).coerceIn(0f, 1f) * progress
                    val barH = (scoreFraction * h).coerceAtLeast(6f)
                    val y = h - barH

                    val isSelected = selectedIndex == i
                    val barColor = when {
                        student.averageScore >= 75f -> BentoPrimary
                        student.averageScore >= 60f -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                if (isSelected) barColor else barColor.copy(alpha = 0.9f),
                                barColor.copy(alpha = 0.75f)
                            ),
                            startY = y,
                            endY = h
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // X-Axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            items.forEachIndexed { idx, s ->
                val shortName = s.studentName.split(" ").firstOrNull() ?: s.studentName
                Text(
                    text = shortName,
                    fontSize = 11.sp,
                    fontWeight = if (selectedIndex == idx) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedIndex == idx) BentoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(idx) }
                )
            }
        }
    }
}

@Composable
private fun NativeQuizBarCanvas(
    quizzes: List<com.example.data.model.QuizPerformanceSummary>,
    progress: Float,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit
) {
    val items = if (quizzes.isNotEmpty()) quizzes else listOf(
        com.example.data.model.QuizPerformanceSummary("q1", "Algorithms", "CS", 4, 85f, 100f),
        com.example.data.model.QuizPerformanceSummary("q2", "Calculus", "Math", 3, 72f, 67f),
        com.example.data.model.QuizPerformanceSummary("q3", "Physics", "Science", 2, 90f, 100f),
        com.example.data.model.QuizPerformanceSummary("q4", "History", "History", 2, 68f, 50f)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(items) {
                        detectTapGestures { offset ->
                            val slotW = size.width / items.size
                            val clicked = (offset.x / slotW).toInt().coerceIn(0, items.size - 1)
                            onSelect(clicked)
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val slotW = w / items.size
                val groupBarW = (slotW * 0.32f).coerceAtMost(16f)

                // Grid lines
                listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { frac ->
                    val y = h * (1f - frac)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.2f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                items.forEachIndexed { i, quiz ->
                    val center = i * slotW + slotW / 2f
                    // Bar 1: Avg score
                    val avgFrac = (quiz.averageScore / 100f).coerceIn(0f, 1f) * progress
                    val h1 = (avgFrac * h).coerceAtLeast(6f)
                    val x1 = center - groupBarW - 2f
                    val y1 = h - h1

                    drawRoundRect(
                        color = BentoPrimary,
                        topLeft = Offset(x1, y1),
                        size = Size(groupBarW, h1),
                        cornerRadius = CornerRadius(8f, 8f)
                    )

                    // Bar 2: Pass rate
                    val passFrac = (quiz.passRate / 100f).coerceIn(0f, 1f) * progress
                    val h2 = (passFrac * h).coerceAtLeast(6f)
                    val x2 = center + 2f
                    val y2 = h - h2

                    drawRoundRect(
                        color = BentoEmerald,
                        topLeft = Offset(x2, y2),
                        size = Size(groupBarW, h2),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            items.forEachIndexed { idx, q ->
                val shortTitle = if (q.quizTitle.length > 9) q.quizTitle.take(8) + ".." else q.quizTitle
                Text(
                    text = shortTitle,
                    fontSize = 11.sp,
                    fontWeight = if (selectedIndex == idx) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedIndex == idx) BentoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(idx) }
                )
            }
        }
    }
}

@Composable
private fun NativeDistributionBarCanvas(
    distributions: List<com.example.data.model.GradeDistributionItem>,
    progress: Float,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit
) {
    val items = if (distributions.isNotEmpty()) distributions else listOf(
        com.example.data.model.GradeDistributionItem("A", "A (90-100%)", 4, 40f, "#10B981"),
        com.example.data.model.GradeDistributionItem("B", "B (80-89%)", 3, 30f, "#3B82F6"),
        com.example.data.model.GradeDistributionItem("C", "C (70-79%)", 2, 20f, "#F59E0B"),
        com.example.data.model.GradeDistributionItem("D", "D (60-69%)", 1, 10f, "#F97316"),
        com.example.data.model.GradeDistributionItem("F", "F (<60%)", 0, 0f, "#EF4444")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(items) {
                        detectTapGestures { offset ->
                            val slotW = size.width / items.size
                            val clicked = (offset.x / slotW).toInt().coerceIn(0, items.size - 1)
                            onSelect(clicked)
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val slotW = w / items.size
                val barW = (slotW * 0.55f).coerceAtMost(36f)

                val maxCount = items.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 5

                items.forEachIndexed { i, item ->
                    val x = i * slotW + (slotW - barW) / 2f
                    val fraction = (item.count.toFloat() / maxCount.toFloat()) * progress
                    val barH = (fraction * h).coerceAtLeast(8f)
                    val y = h - barH

                    val color = try {
                        Color(android.graphics.Color.parseColor(item.hexColor))
                    } catch (_: Exception) {
                        BentoViolet
                    }

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            items.forEachIndexed { idx, itm ->
                Text(
                    text = itm.grade,
                    fontSize = 11.sp,
                    fontWeight = if (selectedIndex == idx) FontWeight.Bold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SelectedBarDetailsPill(
    overview: TeacherAnalyticsOverview,
    chartMode: ChartDisplayMode,
    selectedIndex: Int,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (chartMode) {
                ChartDisplayMode.TRENDS -> {
                    val t = overview.progressTrends.getOrNull(selectedIndex)
                    if (t != null) {
                        Text(
                            text = "${t.milestone}: ${String.format("%.1f", t.averageScore)}% Avg, ${String.format("%.1f", t.passRate)}% Pass (${t.attemptsCount} attempts)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                ChartDisplayMode.STUDENTS -> {
                    val s = overview.studentSummaries.getOrNull(selectedIndex)
                    if (s != null) {
                        Text(
                            text = "${s.studentName}: ${String.format("%.1f", s.averageScore)}% Avg (${s.quizzesTaken} tests)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                ChartDisplayMode.QUIZZES -> {
                    val q = overview.quizSummaries.getOrNull(selectedIndex)
                    if (q != null) {
                        Text(
                            text = "${q.quizTitle}: ${String.format("%.1f", q.averageScore)}% Avg, ${String.format("%.1f", q.passRate)}% Pass",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                ChartDisplayMode.DISTRIBUTION -> {
                    val d = overview.gradeDistributions.getOrNull(selectedIndex)
                    if (d != null) {
                        Text(
                            text = "${d.label}: ${d.count} submissions (${d.percentageOfTotal.toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            TextButton(
                onClick = onDismiss,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Close", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun NativeTrendLineCanvas(
    trends: List<ProgressTrendPoint>,
    progress: Float,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit
) {
    if (trends.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "📈",
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "No Progress Trends Yet",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Quiz attempts will generate class trajectory over time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val primaryColor = BentoPrimary
    val passColor = BentoEmerald
    val targetColor = Color(0xFFF59E0B)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val textStyleColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(trends) {
                        detectTapGestures { offset ->
                            val n = trends.size
                            if (n == 1) {
                                onSelect(0)
                            } else {
                                val padLeft = 36.dp.toPx()
                                val padRight = 16.dp.toPx()
                                val plotW = size.width - padLeft - padRight
                                val step = plotW / (n - 1)
                                val relX = offset.x - padLeft
                                val idx = (relX / step + 0.5f).toInt().coerceIn(0, n - 1)
                                onSelect(idx)
                            }
                        }
                    }
            ) {
                val padLeft = 36.dp.toPx()
                val padRight = 16.dp.toPx()
                val padTop = 20.dp.toPx()
                val padBottom = 20.dp.toPx()

                val plotW = size.width - padLeft - padRight
                val plotH = size.height - padTop - padBottom

                // Grid lines (0%, 25%, 50%, 75%, 100%)
                for (i in 0..4) {
                    val y = padTop + (plotH / 4f) * i
                    drawLine(
                        color = gridColor,
                        start = Offset(padLeft, y),
                        end = Offset(size.width - padRight, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                }

                // 75% Target Line
                val targetY = padTop + plotH * (1f - 0.75f)
                drawLine(
                    color = targetColor.copy(alpha = 0.8f),
                    start = Offset(padLeft, targetY),
                    end = Offset(size.width - padRight, targetY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )

                val n = trends.size
                val points = trends.mapIndexed { idx, item ->
                    val x = if (n == 1) {
                        padLeft + plotW / 2f
                    } else {
                        padLeft + idx * (plotW / (n - 1))
                    }
                    val effectiveScore = (item.averageScore * progress).coerceIn(0f, 100f)
                    val y = padTop + plotH * (1f - effectiveScore / 100f)
                    Offset(x, y)
                }

                // Fill Area gradient under curve
                if (points.isNotEmpty()) {
                    val areaPath = Path().apply {
                        moveTo(points.first().x, padTop + plotH)
                        for (p in points) {
                            lineTo(p.x, p.y)
                        }
                        lineTo(points.last().x, padTop + plotH)
                        close()
                    }
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.28f),
                                primaryColor.copy(alpha = 0.02f)
                            ),
                            startY = padTop,
                            endY = padTop + plotH
                        ),
                        style = Fill
                    )

                    // Line Stroke
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }
                    drawPath(
                        path = linePath,
                        color = primaryColor,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Secondary pass rate line
                    val passPoints = trends.mapIndexed { idx, item ->
                        val x = if (n == 1) padLeft + plotW / 2f else padLeft + idx * (plotW / (n - 1))
                        val effPass = (item.passRate * progress).coerceIn(0f, 100f)
                        val y = padTop + plotH * (1f - effPass / 100f)
                        Offset(x, y)
                    }
                    val passPath = Path().apply {
                        moveTo(passPoints.first().x, passPoints.first().y)
                        for (i in 1 until passPoints.size) {
                            lineTo(passPoints[i].x, passPoints[i].y)
                        }
                    }
                    drawPath(
                        path = passPath,
                        color = passColor,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                        )
                    )

                    // Draw circles at data points
                    points.forEachIndexed { idx, p ->
                        val isSelected = selectedIndex == idx
                        val radius = if (isSelected) 7.dp.toPx() else 4.5f.dp.toPx()
                        
                        if (isSelected) {
                            drawCircle(
                                color = primaryColor.copy(alpha = 0.25f),
                                radius = 12.dp.toPx(),
                                center = p
                            )
                        }
                        
                        drawCircle(
                            color = primaryColor,
                            radius = radius,
                            center = p
                        )
                        drawCircle(
                            color = Color.White,
                            radius = radius * 0.45f,
                            center = p
                        )
                    }
                }
            }
        }

        // Milestone labels along bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 36.dp, end = 16.dp, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            trends.forEachIndexed { idx, t ->
                val isSelected = selectedIndex == idx
                Text(
                    text = if (t.milestone.length > 9) t.milestone.take(8) + ".." else t.milestone,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) BentoPrimary else textStyleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
