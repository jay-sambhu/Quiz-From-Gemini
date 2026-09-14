package com.example.ui.student.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.entities.QuizAttemptEntity
import com.example.ui.components.BentoCard
import com.example.ui.components.BentoPillTag
import com.example.ui.theme.BentoAmber
import com.example.ui.theme.BentoEmerald
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoRose
import com.example.ui.theme.BentoViolet
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ChartEngine {
    RECHARTS,
    NATIVE
}

/**
 * Visualizes a student's score improvement over time on the 'Past History' screen.
 * Supports both Recharts (React 18 + Recharts 2.x inside WebView) and
 * high-performance Native Jetpack Compose Canvas with interactive tooltips.
 */
@Composable
fun StudentScoreImprovementChart(
    attempts: List<QuizAttemptEntity>,
    modifier: Modifier = Modifier
) {
    // Sort attempts chronologically (oldest to newest) to represent progress over time
    val chronologicalAttempts = remember(attempts) {
        attempts.sortedBy { it.completedAt }
    }

    if (chronologicalAttempts.isEmpty()) {
        return
    }

    var selectedEngine by remember { mutableStateOf(ChartEngine.RECHARTS) }
    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f

    // Calculate score improvement metrics
    val firstAttempt = chronologicalAttempts.first()
    val latestAttempt = chronologicalAttempts.last()
    val improvementDelta = latestAttempt.percentage - firstAttempt.percentage
    val highestScore = chronologicalAttempts.maxOf { it.percentage }
    val averageScore = chronologicalAttempts.map { it.percentage }.average().toFloat()

    BentoCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("student_score_improvement_chart_card"),
        cornerRadius = 20.dp,
        padding = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Title & Engine Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (improvementDelta >= 0) BentoEmerald.copy(alpha = 0.15f)
                                else BentoRose.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (improvementDelta >= 0) Icons.AutoMirrored.Filled.TrendingUp
                            else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = "Score Improvement Trend",
                            tint = if (improvementDelta >= 0) BentoEmerald else BentoRose,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Score Improvement Trajectory",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tracking ${chronologicalAttempts.size} completed ${if (chronologicalAttempts.size == 1) "quiz" else "quizzes"} over time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Chart engine toggle pills
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ChartEnginePill(
                        label = "Recharts",
                        isSelected = selectedEngine == ChartEngine.RECHARTS,
                        testTag = "chart_engine_toggle_recharts",
                        onClick = { selectedEngine = ChartEngine.RECHARTS }
                    )
                    ChartEnginePill(
                        label = "Native",
                        isSelected = selectedEngine == ChartEngine.NATIVE,
                        testTag = "chart_engine_toggle_native",
                        onClick = { selectedEngine = ChartEngine.NATIVE }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Summary Metric Pills Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Improvement Delta Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (improvementDelta >= 0) BentoEmerald.copy(alpha = 0.12f) else BentoRose.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, if (improvementDelta >= 0) BentoEmerald.copy(alpha = 0.3f) else BentoRose.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("score_improvement_delta_badge")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Trajectory",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${if (improvementDelta > 0) "+" else ""}${String.format(Locale.US, "%.1f", improvementDelta)}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (improvementDelta >= 0) BentoEmerald else BentoRose
                        )
                    }
                }

                // Initial Score
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Initial",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.0f", firstAttempt.percentage)}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Latest Score
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BentoPrimary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, BentoPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Latest",
                            style = MaterialTheme.typography.labelSmall,
                            color = BentoPrimary,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.0f", latestAttempt.percentage)}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoPrimary
                        )
                    }
                }

                // Peak Score
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BentoViolet.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, BentoViolet.copy(alpha = 0.25f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Peak",
                            style = MaterialTheme.typography.labelSmall,
                            color = BentoViolet,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.0f", highestScore)}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoViolet
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Chart Canvas / WebView Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF8FAFC))
            ) {
                if (selectedEngine == ChartEngine.RECHARTS) {
                    RechartsLineChartWebView(
                        chronologicalAttempts = chronologicalAttempts,
                        isDarkTheme = isDarkTheme
                    )
                } else {
                    NativeComposeLineChart(
                        chronologicalAttempts = chronologicalAttempts,
                        isDarkTheme = isDarkTheme
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chart Legends & Indicators Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ChartLegendIndicator(color = BentoPrimary, label = "Score (%)")
                    ChartLegendIndicator(color = BentoEmerald, label = "Target (75%)", isDashed = true)
                    ChartLegendIndicator(color = BentoRose, label = "Pass (60%)", isDashed = true)
                }

                Text(
                    text = "Tap points for details",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ChartEnginePill(
    label: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun ChartLegendIndicator(
    color: Color,
    label: String,
    isDashed: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (isDashed) {
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(3.dp)
                    .background(color, shape = RoundedCornerShape(1.5.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Recharts Line Chart component embedded within an Android WebView.
 * Loads React 18, ReactDOM, PropTypes, and Recharts 2.x UMD bundle with
 * responsive SVG rendering and custom HTML tooltips.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun RechartsLineChartWebView(
    chronologicalAttempts: List<QuizAttemptEntity>,
    isDarkTheme: Boolean
) {
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val htmlContent = remember(chronologicalAttempts, isDarkTheme) {
        generateRechartsLineChartHtml(chronologicalAttempts, isDarkTheme)
    }

    LaunchedEffect(htmlContent) {
        webViewRef?.loadDataWithBaseURL(
            "https://cdn.recharts.org",
            htmlContent,
            "text/html",
            "UTF-8",
            null
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT

                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                        }
                    }

                    loadDataWithBaseURL(
                        "https://cdn.recharts.org",
                        htmlContent,
                        "text/html",
                        "UTF-8",
                        null
                    )
                    webViewRef = this
                }
            },
            update = { view ->
                webViewRef = view
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("recharts_line_chart_webview")
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = BentoPrimary
                )
            }
        }
    }
}

/**
 * Builds the complete self-contained HTML page using Recharts LineChart.
 */
private fun generateRechartsLineChartHtml(
    attempts: List<QuizAttemptEntity>,
    isDarkTheme: Boolean
): String {
    val bgColor = if (isDarkTheme) "#1E293B" else "#F8FAFC"
    val textColor = if (isDarkTheme) "#F1F5F9" else "#1E293B"
    val subtextColor = if (isDarkTheme) "#94A3B8" else "#64748B"
    val gridColor = if (isDarkTheme) "rgba(255,255,255,0.08)" else "rgba(0,0,0,0.06)"
    val cardBg = if (isDarkTheme) "#0F172A" else "#FFFFFF"

    val dataJsonArray = JSONArray()
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val fullDateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())

    attempts.forEachIndexed { index, attempt ->
        val obj = JSONObject()
        val shortDate = dateFormat.format(Date(attempt.completedAt))
        val fullDate = fullDateFormat.format(Date(attempt.completedAt))
        val milestone = "#${index + 1} ($shortDate)"

        obj.put("milestone", milestone)
        obj.put("quizTitle", attempt.quizTitle)
        obj.put("score", attempt.score)
        obj.put("totalQuestions", attempt.totalQuestions)
        obj.put("percentage", String.format(Locale.US, "%.1f", attempt.percentage).toDoubleOrNull() ?: attempt.percentage.toDouble())
        obj.put("fullDate", fullDate)
        obj.put("category", attempt.categoryName)
        obj.put("isPassed", attempt.percentage >= 60f)
        dataJsonArray.put(obj)
    }

    return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
  <title>Score Improvement Recharts</title>
  
  <!-- React 18 & Recharts 2.x UMD -->
  <script src="https://unpkg.com/react@18.2.0/umd/react.production.min.js"></script>
  <script src="https://unpkg.com/react-dom@18.2.0/umd/react-dom.production.min.js"></script>
  <script src="https://unpkg.com/prop-types@15.8.1/prop-types.min.js"></script>
  <script src="https://unpkg.com/recharts@2.12.7/umd/Recharts.min.js"></script>

  <style>
    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
      -webkit-tap-highlight-color: transparent;
      user-select: none;
    }
    body {
      background-color: ${bgColor};
      color: ${textColor};
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      padding: 8px 6px;
      overflow: hidden;
    }
    #root, #fallback-root {
      width: 100%;
      height: 260px;
    }
    .custom-tooltip {
      background: ${cardBg};
      border-radius: 12px;
      padding: 8px 12px;
      box-shadow: 0 8px 24px rgba(0,0,0,0.2);
      border: 1px solid ${gridColor};
      font-size: 11px;
      line-height: 1.4;
      color: ${textColor};
      max-width: 220px;
    }
    .tooltip-title {
      font-weight: 700;
      font-size: 12px;
      margin-bottom: 2px;
      color: ${textColor};
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .tooltip-date {
      font-size: 10px;
      color: ${subtextColor};
      margin-bottom: 5px;
    }
    .tooltip-score {
      font-weight: 700;
      font-size: 13px;
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .badge {
      display: inline-block;
      padding: 1px 6px;
      border-radius: 6px;
      font-size: 9px;
      font-weight: 700;
    }
    .badge-pass {
      background: rgba(16, 185, 129, 0.15);
      color: #10B981;
    }
    .badge-fail {
      background: rgba(244, 63, 94, 0.15);
      color: #F43F5E;
    }
  </style>
</head>
<body>
  <div id="root"></div>
  <div id="fallback-root" style="display:none;"></div>

  <script>
    const chartData = ${dataJsonArray.toString()};
    const isDark = ${isDarkTheme};
    const gridStroke = "${gridColor}";
    const axisStroke = "${subtextColor}";

    function CustomTooltip({ active, payload }) {
      if (active && payload && payload.length) {
        const item = payload[0].payload;
        return React.createElement('div', { className: 'custom-tooltip' },
          React.createElement('div', { className: 'tooltip-title' }, item.quizTitle),
          React.createElement('div', { className: 'tooltip-date' }, item.fullDate),
          React.createElement('div', { className: 'tooltip-score' },
            React.createElement('span', { style: { color: '#6366F1' } }, item.percentage + '%'),
            React.createElement('span', { style: { color: '${subtextColor}', fontSize: '11px', fontWeight: 'normal' } }, '(' + item.score + '/' + item.totalQuestions + ')'),
            React.createElement('span', { className: 'badge ' + (item.isPassed ? 'badge-pass' : 'badge-fail') }, item.isPassed ? 'Passed' : 'Failed')
          )
        );
      }
      return null;
    }

    function renderRecharts() {
      try {
        const { ResponsiveContainer, AreaChart, Area, Line, XAxis, YAxis, CartesianGrid, Tooltip, ReferenceLine } = window.Recharts;
        
        const App = () => {
          return React.createElement(ResponsiveContainer, { width: '100%', height: 250 },
            React.createElement(AreaChart, { data: chartData, margin: { top: 15, right: 15, left: -20, bottom: 5 } },
              React.createElement('defs', null,
                React.createElement('linearGradient', { id: 'scoreGradient', x1: '0', y1: '0', x2: '0', y2: '1' },
                  React.createElement('stop', { offset: '5%', stopColor: '#6366F1', stopOpacity: 0.35 }),
                  React.createElement('stop', { offset: '95%', stopColor: '#6366F1', stopOpacity: 0.0 })
                )
              ),
              React.createElement(CartesianGrid, { strokeDasharray: '3 3', stroke: gridStroke }),
              React.createElement(XAxis, {
                dataKey: 'milestone',
                stroke: axisStroke,
                fontSize: 10,
                tickLine: false,
                dy: 6
              }),
              React.createElement(YAxis, {
                stroke: axisStroke,
                fontSize: 10,
                domain: [0, 100],
                tickLine: false,
                tickFormatter: (val) => val + '%'
              }),
              React.createElement(Tooltip, { content: React.createElement(CustomTooltip) }),
              React.createElement(ReferenceLine, {
                y: 60,
                stroke: '#F43F5E',
                strokeDasharray: '4 4',
                label: { value: '60% Pass', fill: '#F43F5E', fontSize: 9, position: 'insideTopLeft' }
              }),
              React.createElement(ReferenceLine, {
                y: 75,
                stroke: '#10B981',
                strokeDasharray: '4 4',
                label: { value: '75% Target', fill: '#10B981', fontSize: 9, position: 'insideTopLeft' }
              }),
              React.createElement(Area, {
                type: 'monotone',
                dataKey: 'percentage',
                stroke: '#6366F1',
                strokeWidth: 3,
                fillOpacity: 1,
                fill: 'url(#scoreGradient)',
                dot: { r: 4, fill: '#6366F1', stroke: '#FFFFFF', strokeWidth: 2 },
                activeDot: { r: 7, fill: '#10B981', stroke: '#FFFFFF', strokeWidth: 2 }
              })
            )
          );
        };

        const root = ReactDOM.createRoot(document.getElementById('root'));
        root.render(React.createElement(App));
      } catch (err) {
        renderFallback();
      }
    }

    function renderFallback() {
      document.getElementById('root').style.display = 'none';
      const fallback = document.getElementById('fallback-root');
      fallback.style.display = 'block';

      if (chartData.length === 0) return;

      const w = fallback.clientWidth || 300;
      const h = 240;
      const padding = { top: 20, right: 20, bottom: 30, left: 35 };
      const innerW = w - padding.left - padding.right;
      const innerH = h - padding.top - padding.bottom;

      const points = chartData.map((d, i) => {
        const x = chartData.length > 1 ? padding.left + (i / (chartData.length - 1)) * innerW : padding.left + innerW / 2;
        const y = padding.top + innerH - (d.percentage / 100) * innerH;
        return { x, y, ...d };
      });

      let pathD = 'M ' + points[0].x + ' ' + points[0].y;
      for (let i = 1; i < points.length; i++) {
        pathD += ' L ' + points[i].x + ' ' + points[i].y;
      }

      const passY = padding.top + innerH - 0.6 * innerH;
      const targetY = padding.top + innerH - 0.75 * innerH;

      let svg = '<svg width="' + w + '" height="' + h + '" style="overflow:visible">';
      svg += '<line x1="' + padding.left + '" y1="' + passY + '" x2="' + (w - padding.right) + '" y2="' + passY + '" stroke="#F43F5E" stroke-dasharray="3 3" stroke-width="1"/>';
      svg += '<line x1="' + padding.left + '" y1="' + targetY + '" x2="' + (w - padding.right) + '" y2="' + targetY + '" stroke="#10B981" stroke-dasharray="3 3" stroke-width="1"/>';
      svg += '<path d="' + pathD + '" fill="none" stroke="#6366F1" stroke-width="3" stroke-linecap="round"/>';
      
      points.forEach(p => {
        svg += '<circle cx="' + p.x + '" cy="' + p.y + '" r="5" fill="#6366F1" stroke="#FFFFFF" stroke-width="2"/>';
      });

      svg += '</svg>';
      fallback.innerHTML = svg;
    }

    window.addEventListener('load', () => {
      if (window.Recharts && window.React && window.ReactDOM) {
        renderRecharts();
      } else {
        renderFallback();
      }
    });

    setTimeout(() => {
      if (!document.getElementById('root').innerHTML) {
        renderFallback();
      }
    }, 2500);
  </script>
</body>
</html>
""".trimIndent()
}

/**
 * Native Jetpack Compose Canvas Line Chart companion.
 * Renders smooth cubic bezier curve, score gradient area, passing/target benchmarks,
 * and touch-interactive inspectable points.
 */
@Composable
private fun NativeComposeLineChart(
    chronologicalAttempts: List<QuizAttemptEntity>,
    isDarkTheme: Boolean
) {
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    var animationTrigger by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animationTrigger = true
    }

    val progressAnim by animateFloatAsState(
        targetValue = if (animationTrigger) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "lineProgress"
    )

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .testTag("compose_native_line_chart_canvas")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(chronologicalAttempts) {
                    detectTapGestures { offset ->
                        val leftPadding = 80f
                        val rightPadding = 40f
                        val topPadding = 40f
                        val bottomPadding = 60f
                        val chartWidth = size.width - leftPadding - rightPadding

                        if (chronologicalAttempts.size == 1) {
                            selectedPointIndex = 0
                            return@detectTapGestures
                        }

                        val stepX = chartWidth / (chronologicalAttempts.size - 1)
                        var closestIndex = 0
                        var minDistance = Float.MAX_VALUE

                        chronologicalAttempts.forEachIndexed { i, _ ->
                            val ptX = leftPadding + (i * stepX)
                            val dist = kotlin.math.abs(offset.x - ptX)
                            if (dist < minDistance) {
                                minDistance = dist
                                closestIndex = i
                            }
                        }

                        selectedPointIndex = if (minDistance < stepX * 0.7f) closestIndex else null
                    }
                }
        ) {
            val leftPadding = 80f
            val rightPadding = 40f
            val topPadding = 40f
            val bottomPadding = 60f
            val chartWidth = size.width - leftPadding - rightPadding
            val chartHeight = size.height - topPadding - bottomPadding

            if (chartWidth <= 0 || chartHeight <= 0) return@Canvas

            // Horizontal grid lines and benchmark lines
            val passY = topPadding + chartHeight * (1f - 0.60f)
            val targetY = topPadding + chartHeight * (1f - 0.75f)

            // Grid lines at 25%, 50%, 75%, 100%
            val gridColor = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
            listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { frac ->
                val gy = topPadding + chartHeight * (1f - frac)
                drawLine(
                    color = gridColor,
                    start = Offset(leftPadding, gy),
                    end = Offset(size.width - rightPadding, gy),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Benchmark dashed line: 60% Passing
            drawLine(
                color = BentoRose.copy(alpha = 0.7f),
                start = Offset(leftPadding, passY),
                end = Offset(size.width - rightPadding, passY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            // Benchmark dashed line: 75% Target
            drawLine(
                color = BentoEmerald.copy(alpha = 0.7f),
                start = Offset(leftPadding, targetY),
                end = Offset(size.width - rightPadding, targetY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            val stepX = if (chronologicalAttempts.size > 1) {
                chartWidth / (chronologicalAttempts.size - 1)
            } else {
                0f
            }

            val points = chronologicalAttempts.mapIndexed { index, att ->
                val x = if (chronologicalAttempts.size > 1) leftPadding + (index * stepX) else leftPadding + chartWidth / 2f
                val y = topPadding + chartHeight * (1f - (att.percentage / 100f) * progressAnim)
                Offset(x, y)
            }

            // Draw Area fill under curve
            if (points.isNotEmpty()) {
                val fillPath = Path().apply {
                    moveTo(points.first().x, topPadding + chartHeight)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(points.last().x, topPadding + chartHeight)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BentoPrimary.copy(alpha = 0.35f),
                            BentoPrimary.copy(alpha = 0.0f)
                        ),
                        startY = topPadding,
                        endY = topPadding + chartHeight
                    )
                )

                // Draw connecting line
                val strokePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }

                drawPath(
                    path = strokePath,
                    color = BentoPrimary,
                    style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )

                // Draw circles for points
                points.forEachIndexed { i, pt ->
                    val isSelected = selectedPointIndex == i
                    val ptColor = if (chronologicalAttempts[i].percentage >= 60f) BentoPrimary else BentoRose

                    if (isSelected) {
                        // Outer pulse ring
                        drawCircle(
                            color = ptColor.copy(alpha = 0.25f),
                            radius = 12.dp.toPx(),
                            center = pt
                        )
                    }

                    // Solid inner circle
                    drawCircle(
                        color = Color.White,
                        radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = ptColor,
                        radius = if (isSelected) 4.5.dp.toPx() else 3.dp.toPx(),
                        center = pt
                    )
                }
            }
        }

        // Overlay Inspection Card if point selected
        selectedPointIndex?.let { idx ->
            if (idx in chronologicalAttempts.indices) {
                val selectedAttempt = chronologicalAttempts[idx]
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, BentoPrimary.copy(alpha = 0.3f)),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column {
                            Text(
                                text = selectedAttempt.quizTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = dateFormat.format(Date(selectedAttempt.completedAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }

                        Text(
                            text = "${String.format(Locale.US, "%.0f", selectedAttempt.percentage)}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedAttempt.percentage >= 60f) BentoEmerald else BentoRose
                        )

                        Text(
                            text = "${selectedAttempt.score}/${selectedAttempt.totalQuestions}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
