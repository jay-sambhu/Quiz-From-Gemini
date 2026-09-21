package com.example.ui.student

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.QuizAttemptEntity
import com.example.ui.QuizViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizResultScreen(
    viewModel: QuizViewModel,
    onDone: () -> Unit,
    onReviewHistory: () -> Unit
) {
    val activeState by viewModel.activeQuizState.collectAsState()
    val allAttempts by viewModel.allAttempts.collectAsState()
    val attempt = activeState.completedAttempt ?: allAttempts.maxByOrNull { it.completedAt }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showShareDialog by remember { mutableStateOf(false) }

    if (attempt == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            NoQuizResultsEmptyState(
                isCard = true,
                customTitle = "No Quiz Result Available",
                customDescription = "There is no active or completed quiz result session to display at this time. Return to your dashboard to select an assessment.",
                onTakeQuiz = onDone,
                testTag = "quiz_result_empty_state"
            )
        }
        return
    }

    val isPassed = attempt.percentage >= 60f
    val isPerfectScore = (attempt.score == attempt.totalQuestions && attempt.totalQuestions > 0) || attempt.percentage >= 99.9f
    var celebrationTriggerKey by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isPerfectScore) "Perfect Quiz Performance" else "Quiz Analytics & Performance",
                                fontWeight = FontWeight.Bold
                            )
                            if (isPerfectScore) {
                                Spacer(modifier = Modifier.width(8.dp))
                                BentoPillTag(
                                    text = "100%",
                                    containerColor = Color(0xFFFEF3C7),
                                    contentColor = Color(0xFFB45309),
                                    icon = Icons.Default.AutoAwesome
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDone) { Icon(Icons.Default.Close, contentDescription = "Close") }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Celebratory Badge: Flawless/Perfect or Passed Success
                if (isPerfectScore) {
                    PerfectScoreCelebrationBadge(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("perfect_score_celebration_badge"),
                        onReplayCelebration = {
                            celebrationTriggerKey++
                        }
                    )
                } else if (isPassed) {
                    SuccessCelebrationBadge(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("success_celebration_badge"),
                        score = attempt.score,
                        totalQuestions = attempt.totalQuestions,
                        percentage = attempt.percentage,
                        onReplayCelebration = {
                            celebrationTriggerKey++
                        }
                    )
                }

                // Bento Hero Score Badge Card
                BentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = when {
                        isPerfectScore -> Color(0xFFFFFBEB)
                        isPassed -> BentoEmerald.copy(alpha = 0.12f)
                        else -> BentoRose.copy(alpha = 0.12f)
                    },
                    borderColor = when {
                        isPerfectScore -> Color(0xFFF59E0B).copy(alpha = 0.5f)
                        isPassed -> BentoEmerald.copy(alpha = 0.35f)
                        else -> BentoRose.copy(alpha = 0.35f)
                    },
                    cornerRadius = 24.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isPerfectScore -> Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFF59E0B)))
                                        isPassed -> Brush.linearGradient(listOf(BentoEmerald, BentoEmerald))
                                        else -> Brush.linearGradient(listOf(BentoRose, BentoRose))
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    isPerfectScore -> Icons.Default.EmojiEvents
                                    isPassed -> Icons.Default.EmojiEvents
                                    else -> Icons.Default.SentimentDissatisfied
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = when {
                                isPerfectScore -> "Flawless! Perfect Score!"
                                isPassed -> "Congratulations! Test Passed"
                                else -> "Keep Practicing! Test Failed"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isPerfectScore -> Color(0xFFB45309)
                                isPassed -> BentoEmerald
                                else -> BentoRose
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = attempt.quizTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "${attempt.score} / ${attempt.totalQuestions}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Accuracy Score: ${String.format("%.1f", attempt.percentage)}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isPerfectScore -> Color(0xFFB45309)
                                isPassed -> BentoEmerald
                                else -> BentoRose
                            }
                        )
                    }
                }

                // Bento Detailed Metrics Grid
                BentoSectionTitle(title = "Detailed Analytics Matrix")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BentoStatTile(
                        label = "Accuracy",
                        value = "${String.format("%.0f", attempt.percentage)}%",
                        icon = Icons.Default.PieChart,
                        accentColor = if (isPerfectScore) Color(0xFFF59E0B) else BentoViolet,
                        modifier = Modifier.weight(1f)
                    )
                    BentoStatTile(
                        label = "Time Taken",
                        value = "${attempt.timeSpentSeconds}s",
                        icon = Icons.Default.Timer,
                        accentColor = BentoCyan,
                        modifier = Modifier.weight(1f)
                    )
                    BentoStatTile(
                        label = "Correct",
                        value = "${attempt.score}/${attempt.totalQuestions}",
                        icon = Icons.Default.CheckCircle,
                        accentColor = BentoEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    if (isPerfectScore) {
                        BentoStatTile(
                            label = "Bonus",
                            value = "+100 PTS",
                            icon = Icons.Default.AutoAwesome,
                            accentColor = Color(0xFFD97706),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bento Action Buttons
                BentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { showShareDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("share_result_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPerfectScore) Color(0xFFF59E0B) else BentoPrimary
                            )
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Result Card", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onReviewHistory,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Review Attempt History & Explanations", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Celebratory Confetti Animation Layer on top of Screen when student successfully completes a quiz
        if (isPassed) {
            CelebrationConfettiOverlay(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("celebration_confetti_overlay"),
                triggerKey = celebrationTriggerKey,
                particleCount = if (isPerfectScore) 150 else 105,
                burstDurationMs = if (isPerfectScore) 5500L else 4200L
            )
        }
    }

    if (showShareDialog) {
        val shareText = if (isPerfectScore) {
            """
            FLAWLESS 100% PERFECT SCORE!
            I scored ${attempt.score}/${attempt.totalQuestions} (100%) on '${attempt.quizTitle}' in Quiz Platform!
            Can you match my perfect score?
            """.trimIndent()
        } else {
            """
            I scored ${attempt.score}/${attempt.totalQuestions} (${String.format("%.0f", attempt.percentage)}%) on '${attempt.quizTitle}' in Quiz Platform! 
            Can you beat my score? Download Quiz Platform to compete!
            """.trimIndent()
        }

        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Share Achievement Card", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Share your result card with classmates:")

                    BentoCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = BentoViolet.copy(alpha = 0.12f),
                        borderColor = BentoViolet.copy(alpha = 0.3f),
                        cornerRadius = 14.dp
                    ) {
                        Text(
                            text = shareText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Quiz Achievement"))
                        showShareDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share via App", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(shareText))
                        Toast.makeText(context, "Copied share text to clipboard!", Toast.LENGTH_SHORT).show()
                        showShareDialog = false
                    }
                ) {
                    Text("Copy Text")
                }
            }
        )
    }
}

