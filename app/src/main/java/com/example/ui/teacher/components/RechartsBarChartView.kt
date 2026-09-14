package com.example.ui.teacher.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.ChartDisplayMode
import com.example.data.model.TeacherAnalyticsOverview
import com.example.ui.components.BentoCard
import com.example.ui.components.BentoPillTag
import com.example.ui.theme.BentoEmerald
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoViolet
import org.json.JSONArray
import org.json.JSONObject

/**
 * Interactive Bar Chart component rendering student performance data using the Recharts library.
 * Integrates Recharts (React 18 + Recharts UMD bundle) inside an optimized Android WebView with
 * an instant SVG fallback engine to ensure guaranteed responsiveness online and offline.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RechartsBarChartView(
    overview: TeacherAnalyticsOverview,
    chartMode: ChartDisplayMode,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f

    // Re-generate HTML whenever data, chart mode, or theme changes
    val htmlContent = remember(overview, chartMode, isDarkTheme) {
        generateRechartsHtml(overview, chartMode, isDarkTheme)
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

    BentoCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_bar_chart_card"),
        cornerRadius = 20.dp,
        padding = 14.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Chart Top Subheader & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (chartMode) {
                            ChartDisplayMode.TRENDS -> Icons.AutoMirrored.Filled.TrendingUp
                            ChartDisplayMode.QUIZZES -> Icons.Default.Quiz
                            ChartDisplayMode.STUDENTS -> Icons.Default.Person
                            ChartDisplayMode.DISTRIBUTION -> Icons.Default.PieChart
                        },
                        contentDescription = null,
                        tint = BentoPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = when (chartMode) {
                            ChartDisplayMode.TRENDS -> "Student Progress & Score Trends"
                            ChartDisplayMode.QUIZZES -> "Quiz Scores vs Pass Rates"
                            ChartDisplayMode.STUDENTS -> "Student Average Scores"
                            ChartDisplayMode.DISTRIBUTION -> "Class Grade Distribution"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                BentoPillTag(
                    text = "Recharts 2.x",
                    containerColor = BentoViolet.copy(alpha = 0.12f),
                    contentColor = BentoViolet,
                    icon = Icons.Default.Code
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = when (chartMode) {
                    ChartDisplayMode.TRENDS -> "Chronological performance trajectory across ${overview.progressTrends.size} milestones (Target: 75%)"
                    ChartDisplayMode.QUIZZES -> "Grouped bar chart comparing Class Avg vs Pass Rate across ${overview.quizSummaries.size} quiz sets"
                    ChartDisplayMode.STUDENTS -> "Evaluated across ${overview.studentSummaries.size} active students (Target Benchmark: 75%)"
                    ChartDisplayMode.DISTRIBUTION -> "Distribution of all ${overview.totalAttemptsEvaluated} submitted quiz attempts"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Recharts WebView Canvas Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                    )
            ) {
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
                        .testTag("recharts_webview")
                )

                // Subtle loading overlay
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
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

            Spacer(modifier = Modifier.height(10.dp))

            // Chart Legends & Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (chartMode) {
                    ChartDisplayMode.TRENDS -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ChartLegendItem(color = Color(0xFF6366F1), label = "Avg Score (%)")
                            ChartLegendItem(color = Color(0xFF10B981), label = "Pass Rate (%)", isDashed = true)
                            ChartLegendItem(color = Color(0xFFF59E0B), label = "75% Target", isDashed = true)
                        }
                    }
                    ChartDisplayMode.STUDENTS -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ChartLegendItem(color = Color(0xFF6366F1), label = "Student Avg (%)")
                            ChartLegendItem(color = Color(0xFF10B981), label = ">= 75% Benchmark", isDashed = true)
                        }
                    }
                    ChartDisplayMode.QUIZZES -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ChartLegendItem(color = Color(0xFF6366F1), label = "Avg Score (%)")
                            ChartLegendItem(color = Color(0xFF10B981), label = "Pass Rate (%)")
                        }
                    }
                    ChartDisplayMode.DISTRIBUTION -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ChartLegendItem(color = Color(0xFF10B981), label = "A (90%+)")
                            ChartLegendItem(color = Color(0xFF3B82F6), label = "B (80-89%)")
                            ChartLegendItem(color = Color(0xFFF59E0B), label = "C (70-79%)")
                            ChartLegendItem(color = Color(0xFFEF4444), label = "D/F (<70%)")
                        }
                    }
                }

                Text(
                    text = "Tap bars for details",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ChartLegendItem(
    color: Color,
    label: String,
    isDashed: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                    .size(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Builds the complete self-contained HTML page loading React 18, Recharts 2.x UMD,
 * dynamic dataset JSON, and an instant high-fidelity SVG fallback.
 */
private fun generateRechartsHtml(
    overview: TeacherAnalyticsOverview,
    chartMode: ChartDisplayMode,
    isDarkTheme: Boolean
): String {
    val bgColor = if (isDarkTheme) "#1E293B" else "#F8FAFC"
    val textColor = if (isDarkTheme) "#F1F5F9" else "#1E293B"
    val subtextColor = if (isDarkTheme) "#94A3B8" else "#64748B"
    val gridColor = if (isDarkTheme) "rgba(255,255,255,0.08)" else "rgba(0,0,0,0.06)"
    val cardBg = if (isDarkTheme) "#0F172A" else "#FFFFFF"

    val dataJsonArray = JSONArray()

    when (chartMode) {
        ChartDisplayMode.TRENDS -> {
            overview.progressTrends.take(8).forEach { point ->
                val obj = JSONObject()
                obj.put("name", point.milestone)
                obj.put("fullName", point.milestone)
                obj.put("avgScore", String.format("%.1f", point.averageScore).toDoubleOrNull() ?: point.averageScore.toDouble())
                obj.put("passRate", String.format("%.1f", point.passRate).toDoubleOrNull() ?: point.passRate.toDouble())
                obj.put("attempts", point.attemptsCount)
                dataJsonArray.put(obj)
            }
        }

        ChartDisplayMode.STUDENTS -> {
            overview.studentSummaries.take(7).forEach { student ->
                val obj = JSONObject()
                val shortName = student.studentName.split(" ").firstOrNull() ?: student.studentName
                obj.put("name", shortName)
                obj.put("fullName", student.studentName)
                obj.put("avgScore", String.format("%.1f", student.averageScore).toDoubleOrNull() ?: student.averageScore.toDouble())
                obj.put("highestScore", student.highestScore.toInt())
                obj.put("quizzesTaken", student.quizzesTaken)
                obj.put("grade", student.gradeTier)
                dataJsonArray.put(obj)
            }
        }

        ChartDisplayMode.QUIZZES -> {
            overview.quizSummaries.take(6).forEach { quiz ->
                val obj = JSONObject()
                val shortTitle = if (quiz.quizTitle.length > 12) quiz.quizTitle.take(11) + ".." else quiz.quizTitle
                obj.put("name", shortTitle)
                obj.put("fullTitle", quiz.quizTitle)
                obj.put("avgScore", String.format("%.1f", quiz.averageScore).toDoubleOrNull() ?: quiz.averageScore.toDouble())
                obj.put("passRate", String.format("%.1f", quiz.passRate).toDoubleOrNull() ?: quiz.passRate.toDouble())
                obj.put("attempts", quiz.totalAttempts)
                dataJsonArray.put(obj)
            }
        }

        ChartDisplayMode.DISTRIBUTION -> {
            overview.gradeDistributions.forEach { item ->
                val obj = JSONObject()
                obj.put("name", item.grade)
                obj.put("label", item.label)
                obj.put("count", item.count)
                obj.put("percentage", item.percentageOfTotal.toInt())
                obj.put("color", item.hexColor)
                dataJsonArray.put(obj)
            }
        }
    }

    val modeString = chartMode.name

    return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
  <title>Teacher Performance Recharts Dashboard</title>
  
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
      padding: 10px 8px;
      overflow: hidden;
    }
    #root, #fallback-root {
      width: 100%;
      height: 290px;
    }
    .custom-tooltip {
      background: ${cardBg};
      border-radius: 12px;
      padding: 8px 12px;
      box-shadow: 0 10px 25px rgba(0,0,0,0.15);
      border: 1px solid ${gridColor};
      font-size: 11px;
      line-height: 1.4;
      color: ${textColor};
    }
    .tooltip-title {
      font-weight: 700;
      margin-bottom: 3px;
      color: ${textColor};
    }
    .tooltip-metric {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 11px;
    }
    .tooltip-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
    }
  </style>
</head>
<body>
  <div id="root"></div>
  <div id="fallback-root" style="display:none;"></div>

  <script>
    const chartData = ${dataJsonArray.toString()};
    const chartMode = "${modeString}";
    const isDark = ${isDarkTheme};
    const gridStroke = "${gridColor}";
    const axisStroke = "${subtextColor}";

    // Custom Tooltip Component for Recharts
    function CustomTooltip({ active, payload, label }) {
      if (active && payload && payload.length) {
        const item = payload[0].payload;
        return React.createElement('div', { className: 'custom-tooltip' },
          React.createElement('div', { className: 'tooltip-title' }, item.fullName || item.fullTitle || item.label || label),
          payload.map((entry, idx) => (
            React.createElement('div', { key: idx, className: 'tooltip-metric', style: { color: entry.color } },
              React.createElement('span', { className: 'tooltip-dot', style: { backgroundColor: entry.color } }),
              entry.name + ': ' + entry.value + (chartMode === 'DISTRIBUTION' ? ' students' : '%')
            )
          ))
        );
      }
      return null;
    }

    function renderRecharts() {
      if (!window.Recharts || !window.React || !window.ReactDOM) {
        renderFallbackSvg();
        return;
      }

      const {
        AreaChart, Area, BarChart, Bar, LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend,
        ResponsiveContainer, Cell, ReferenceLine
      } = window.Recharts;

      // React Icons (Lucide / Feather / FontAwesome SVG icon components for React)
      const ReactIcons = {
        TrendingUp: function(props) {
          const s = props.size || 22;
          const c = props.color || 'currentColor';
          return React.createElement('svg', {
            width: s, height: s, viewBox: '0 0 24 24', fill: 'none', stroke: c,
            strokeWidth: '2', strokeLinecap: 'round', strokeLinejoin: 'round'
          },
            React.createElement('polyline', { points: '23 6 13.5 15.5 8.5 10.5 1 18' }),
            React.createElement('polyline', { points: '17 6 23 6 23 12' })
          );
        },
        BarChart: function(props) {
          const s = props.size || 22;
          const c = props.color || 'currentColor';
          return React.createElement('svg', {
            width: s, height: s, viewBox: '0 0 24 24', fill: 'none', stroke: c,
            strokeWidth: '2', strokeLinecap: 'round', strokeLinejoin: 'round'
          },
            React.createElement('line', { x1: '18', y1: '20', x2: '18', y2: '10' }),
            React.createElement('line', { x1: '12', y1: '20', x2: '12', y2: '4' }),
            React.createElement('line', { x1: '6', y1: '20', x2: '6', y2: '14' })
          );
        },
        PieChart: function(props) {
          const s = props.size || 22;
          const c = props.color || 'currentColor';
          return React.createElement('svg', {
            width: s, height: s, viewBox: '0 0 24 24', fill: 'none', stroke: c,
            strokeWidth: '2', strokeLinecap: 'round', strokeLinejoin: 'round'
          },
            React.createElement('path', { d: 'M21.21 15.89A10 10 0 1 1 8 2.83' }),
            React.createElement('path', { d: 'M22 12A10 10 0 0 0 12 2v10z' })
          );
        },
        Users: function(props) {
          const s = props.size || 22;
          const c = props.color || 'currentColor';
          return React.createElement('svg', {
            width: s, height: s, viewBox: '0 0 24 24', fill: 'none', stroke: c,
            strokeWidth: '2', strokeLinecap: 'round', strokeLinejoin: 'round'
          },
            React.createElement('path', { d: 'M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2' }),
            React.createElement('circle', { cx: '9', cy: '7', r: '4' }),
            React.createElement('path', { d: 'M23 21v-2a4 4 0 0 0-3-3.87' }),
            React.createElement('path', { d: 'M16 3.13a4 4 0 0 1 0 7.75' })
          );
        }
      };

      function App() {
        if (!chartData || chartData.length === 0) {
          const ActiveIcon = chartMode === 'TRENDS' ? ReactIcons.TrendingUp :
                             chartMode === 'QUIZZES' ? ReactIcons.BarChart :
                             chartMode === 'STUDENTS' ? ReactIcons.Users : ReactIcons.PieChart;

          return React.createElement('div', {
            style: {
              height: '280px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              color: axisStroke,
              textAlign: 'center',
              padding: '20px'
            }
          },
            React.createElement('div', {
              style: {
                width: '52px',
                height: '52px',
                borderRadius: '50%',
                backgroundColor: isDark ? 'rgba(99, 102, 241, 0.16)' : 'rgba(99, 102, 241, 0.1)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                marginBottom: '12px',
                color: '#6366F1'
              }
            },
              React.createElement(ActiveIcon, { size: 26, color: '#6366F1' })
            ),
            React.createElement('div', { style: { fontWeight: '700', fontSize: '14px', color: isDark ? '#E2E8F0' : '#1E293B' } }, 'No Performance Data Yet'),
            React.createElement('div', { style: { fontSize: '11px', marginTop: '4px', maxWidth: '240px', lineHeight: '1.5' } }, 'Quiz results and student progress trends will automatically graph here once attempts are recorded.')
          );
        }

        if (chartMode === 'TRENDS') {
          return React.createElement(ResponsiveContainer, { width: '100%', height: 280 },
            React.createElement(AreaChart, {
              data: chartData,
              margin: { top: 18, right: 12, left: -20, bottom: 24 }
            },
              React.createElement('defs', null,
                React.createElement('linearGradient', { id: 'scoreGradient', x1: '0', y1: '0', x2: '0', y2: '1' },
                  React.createElement('stop', { offset: '5%', stopColor: '#6366F1', stopOpacity: 0.35 }),
                  React.createElement('stop', { offset: '95%', stopColor: '#6366F1', stopOpacity: 0.0 })
                )
              ),
              React.createElement(CartesianGrid, { strokeDasharray: '3 3', stroke: gridStroke, vertical: false }),
              React.createElement(XAxis, {
                dataKey: 'name',
                stroke: axisStroke,
                tick: { fontSize: 11, fill: axisStroke },
                tickLine: false
              }),
              React.createElement(YAxis, {
                domain: [0, 100],
                stroke: axisStroke,
                tick: { fontSize: 10, fill: axisStroke },
                tickLine: false,
                unit: '%'
              }),
              React.createElement(Tooltip, { content: React.createElement(CustomTooltip) }),
              React.createElement(ReferenceLine, {
                y: 75,
                stroke: '#F59E0B',
                strokeDasharray: '3 3',
                label: { value: '75% Target', position: 'insideTopRight', fill: '#F59E0B', fontSize: 10 }
              }),
              React.createElement(Area, {
                type: 'monotone',
                dataKey: 'avgScore',
                name: 'Avg Score (%)',
                stroke: '#6366F1',
                strokeWidth: 3,
                fill: 'url(#scoreGradient)',
                dot: { r: 4, fill: '#6366F1' },
                activeDot: { r: 6 }
              }),
              React.createElement(Line, {
                type: 'monotone',
                dataKey: 'passRate',
                name: 'Pass Rate (%)',
                stroke: '#10B981',
                strokeWidth: 2,
                strokeDasharray: '4 2',
                dot: { r: 3, fill: '#10B981' }
              })
            )
          );
        }

        let bars = null;

        if (chartMode === 'STUDENTS') {
          bars = [
            React.createElement(Bar, {
              key: 'avgScore',
              dataKey: 'avgScore',
              name: 'Avg Score',
              fill: '#6366F1',
              radius: [6, 6, 0, 0],
              animationDuration: 750
            },
            chartData.map((entry, index) => (
              React.createElement(Cell, {
                key: 'cell-' + index,
                fill: entry.avgScore >= 75 ? '#6366F1' : (entry.avgScore >= 60 ? '#F59E0B' : '#EF4444')
              })
            ))
          ),
          React.createElement(ReferenceLine, {
            key: 'refLine',
            y: 75,
            stroke: '#10B981',
            strokeDasharray: '3 3',
            label: { value: '75% Benchmark', position: 'insideTopRight', fill: '#10B981', fontSize: 10 }
          })
        ];
        } else if (chartMode === 'QUIZZES') {
          bars = [
            React.createElement(Bar, {
              key: 'avgScore',
              dataKey: 'avgScore',
              name: 'Avg Score (%)',
              fill: '#6366F1',
              radius: [5, 5, 0, 0],
              animationDuration: 750
            }),
            React.createElement(Bar, {
              key: 'passRate',
              dataKey: 'passRate',
              name: 'Pass Rate (%)',
              fill: '#10B981',
              radius: [5, 5, 0, 0],
              animationDuration: 750
            })
          ];
        } else {
          // DISTRIBUTION
          bars = [
            React.createElement(Bar, {
              key: 'count',
              dataKey: 'count',
              name: 'Student Count',
              fill: '#8B5CF6',
              radius: [6, 6, 0, 0],
              animationDuration: 750
            },
            chartData.map((entry, index) => (
              React.createElement(Cell, {
                key: 'cell-' + index,
                fill: entry.color || '#6366F1'
              })
            ))
          )
        ];
        }

        const yDomain = chartMode === 'DISTRIBUTION' ? [0, 'auto'] : [0, 100];
        const yUnit = chartMode === 'DISTRIBUTION' ? '' : '%';

        return React.createElement(ResponsiveContainer, { width: '100%', height: 280 },
          React.createElement(BarChart, {
            data: chartData,
            margin: { top: 18, right: 12, left: -20, bottom: 24 }
          },
            React.createElement(CartesianGrid, { strokeDasharray: '3 3', stroke: gridStroke, vertical: false }),
            React.createElement(XAxis, {
              dataKey: 'name',
              stroke: axisStroke,
              tick: { fontSize: 11, fill: axisStroke },
              tickLine: false
            }),
            React.createElement(YAxis, {
              domain: yDomain,
              stroke: axisStroke,
              tick: { fontSize: 10, fill: axisStroke },
              tickLine: false,
              unit: yUnit
            }),
            React.createElement(Tooltip, { content: React.createElement(CustomTooltip) }),
            bars
          )
        );
      }

      try {
        const root = ReactDOM.createRoot(document.getElementById('root'));
        root.render(React.createElement(App));
      } catch (err) {
        console.error("Recharts render error:", err);
        renderFallbackSvg();
      }
    }

    // High-fidelity instant SVG engine fallback if external CDN script is slow/offline
    function renderFallbackSvg() {
      const root = document.getElementById('root');
      root.style.display = 'none';
      const fallback = document.getElementById('fallback-root');
      fallback.style.display = 'block';

      const width = fallback.clientWidth || 340;
      const height = 280;
      const padLeft = 32;
      const padRight = 16;
      const padTop = 24;
      const padBottom = 34;

      const plotW = width - padLeft - padRight;
      const plotH = height - padTop - padBottom;

      if (!chartData || chartData.length === 0) {
        const cx = width / 2;
        let emptySvg = '<svg width="100%" height="280" viewBox="0 0 ' + width + ' ' + height + '" xmlns="http://www.w3.org/2000/svg">';
        // Vector Icon Badge
        emptySvg += '<circle cx="' + cx + '" cy="100" r="24" fill="' + (isDark ? 'rgba(99,102,241,0.18)' : 'rgba(99,102,241,0.1)') + '" />';
        emptySvg += '<path d="M ' + (cx - 10) + ' 106 L ' + (cx - 4) + ' 100 L ' + (cx + 1) + ' 103 L ' + (cx + 9) + ' 94" fill="none" stroke="#6366F1" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />';
        emptySvg += '<path d="M ' + (cx + 4) + ' 94 L ' + (cx + 9) + ' 94 L ' + (cx + 9) + ' 99" fill="none" stroke="#6366F1" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />';
        emptySvg += '<text x="' + cx + '" y="146" fill="' + (isDark ? '#E2E8F0' : '#1E293B') + '" font-size="14" font-weight="bold" text-anchor="middle">No Performance Data Yet</text>';
        emptySvg += '<text x="' + cx + '" y="166" fill="' + axisStroke + '" font-size="11" text-anchor="middle">Student quiz results and trends will appear here.</text>';
        emptySvg += '</svg>';
        fallback.innerHTML = emptySvg;
        return;
      }

      let maxVal = 100;
      if (chartMode === 'DISTRIBUTION') {
        const maxC = Math.max(...chartData.map(d => d.count || 0));
        maxVal = maxC > 0 ? Math.ceil(maxC * 1.25) : 5;
      }

      let svg = '<svg width="100%" height="280" viewBox="0 0 ' + width + ' ' + height + '" xmlns="http://www.w3.org/2000/svg">';
      
      // Grid lines
      const ticks = 4;
      for (let i = 0; i <= ticks; i++) {
        const y = padTop + (plotH / ticks) * i;
        const val = Math.round(maxVal - (maxVal / ticks) * i);
        svg += '<line x1="' + padLeft + '" y1="' + y + '" x2="' + (width - padRight) + '" y2="' + y + '" stroke="' + gridStroke + '" stroke-dasharray="3,3" />';
        svg += '<text x="' + (padLeft - 6) + '" y="' + (y + 3) + '" fill="' + axisStroke + '" font-size="9" text-anchor="end">' + val + (chartMode === 'DISTRIBUTION' ? '' : '%') + '</text>';
      }

      // Benchmark line for students or trends
      if (chartMode === 'STUDENTS' || chartMode === 'TRENDS') {
        const benchY = padTop + plotH * (1 - 75 / 100);
        svg += '<line x1="' + padLeft + '" y1="' + benchY + '" x2="' + (width - padRight) + '" y2="' + benchY + '" stroke="' + (chartMode === 'TRENDS' ? '#F59E0B' : '#10B981') + '" stroke-width="1.5" stroke-dasharray="4,3" />';
        svg += '<text x="' + (width - padRight - 4) + '" y="' + (benchY - 4) + '" fill="' + (chartMode === 'TRENDS' ? '#F59E0B' : '#10B981') + '" font-size="9" font-weight="bold" text-anchor="end">75% Target</text>';
      }

      if (chartMode === 'TRENDS') {
        const pts = chartData.map((d, idx) => {
          const x = padLeft + idx * (plotW / Math.max(1, chartData.length - 1));
          const y = padTop + plotH * (1 - (d.avgScore || 0) / 100);
          return { x, y, d };
        });

        if (pts.length > 0) {
          // Fill area
          let areaPath = 'M ' + pts[0].x + ' ' + (padTop + plotH);
          pts.forEach(p => { areaPath += ' L ' + p.x + ' ' + p.y; });
          areaPath += ' L ' + pts[pts.length - 1].x + ' ' + (padTop + plotH) + ' Z';
          svg += '<path d="' + areaPath + '" fill="rgba(99,102,241,0.18)" />';

          // Stroke line
          let linePath = 'M ' + pts[0].x + ' ' + pts[0].y;
          for (let i = 1; i < pts.length; i++) {
            linePath += ' L ' + pts[i].x + ' ' + pts[i].y;
          }
          svg += '<path d="' + linePath + '" fill="none" stroke="#6366F1" stroke-width="3" stroke-linecap="round" />';

          // Dots and text
          pts.forEach(p => {
            svg += '<circle cx="' + p.x + '" cy="' + p.y + '" r="4" fill="#6366F1" stroke="#FFFFFF" stroke-width="1.5" />';
            svg += '<text x="' + p.x + '" y="' + (p.y - 8) + '" fill="#6366F1" font-size="9" font-weight="bold" text-anchor="middle">' + p.d.avgScore + '%</text>';
            svg += '<text x="' + p.x + '" y="' + (height - 10) + '" fill="' + axisStroke + '" font-size="9" text-anchor="middle">' + p.d.name + '</text>';
          });
        }
      } else {
        // Bars
        const n = chartData.length || 1;
        const colW = plotW / n;

        chartData.forEach((d, idx) => {
          const xCenter = padLeft + idx * colW + colW / 2;

          if (chartMode === 'QUIZZES') {
            const barW = Math.min(14, colW * 0.35);
            // Bar 1: avgScore
            const h1 = (d.avgScore / maxVal) * plotH;
            const y1 = padTop + plotH - h1;
            svg += '<rect x="' + (xCenter - barW - 2) + '" y="' + y1 + '" width="' + barW + '" height="' + h1 + '" fill="#6366F1" rx="4" />';
            
            // Bar 2: passRate
            const h2 = (d.passRate / maxVal) * plotH;
            const y2 = padTop + plotH - h2;
            svg += '<rect x="' + (xCenter + 2) + '" y="' + y2 + '" width="' + barW + '" height="' + h2 + '" fill="#10B981" rx="4" />';
          } else {
            const val = chartMode === 'DISTRIBUTION' ? d.count : d.avgScore;
            const barW = Math.min(26, colW * 0.58);
            const barH = Math.max(4, (val / maxVal) * plotH);
            const barY = padTop + plotH - barH;
            const color = d.color || (val >= 75 ? '#6366F1' : (val >= 60 ? '#F59E0B' : '#EF4444'));
            svg += '<rect x="' + (xCenter - barW / 2) + '" y="' + barY + '" width="' + barW + '" height="' + barH + '" fill="' + color + '" rx="5" />';
            
            // Value label on top
            svg += '<text x="' + xCenter + '" y="' + (barY - 4) + '" fill="' + color + '" font-size="9" font-weight="bold" text-anchor="middle">' + val + (chartMode === 'DISTRIBUTION' ? '' : '%') + '</text>';
          }

          // X-axis label
          svg += '<text x="' + xCenter + '" y="' + (height - 10) + '" fill="' + axisStroke + '" font-size="10" text-anchor="middle">' + d.name + '</text>';
        });
      }

      svg += '</svg>';
      fallback.innerHTML = svg;
    }

    // Try rendering Recharts; if CDN scripts are still loading or offline, fallback will activate
    window.addEventListener('load', () => {
      setTimeout(renderRecharts, 50);
    });

    // Immediate check if scripts are already cached/ready
    if (window.Recharts && window.React && window.ReactDOM) {
      renderRecharts();
    } else {
      // Show instant SVG while network settles
      renderFallbackSvg();
      // Retry once after 600ms to upgrade to Recharts if it finishes loading
      setTimeout(renderRecharts, 600);
    }
  </script>
</body>
</html>
""";
}
