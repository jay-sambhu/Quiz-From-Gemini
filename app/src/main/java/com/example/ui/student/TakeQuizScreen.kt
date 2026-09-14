package com.example.ui.student

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.QuizViewModel
import com.example.ui.components.CompactCountdownTimerChip
import com.example.ui.components.QuizCountdownTimerCard
import com.example.ui.components.QuizTopProgressBar
import com.example.ui.components.ShimmerQuizTakingSkeleton
import com.example.ui.components.TimeExpiredAutoSubmitDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeQuizScreen(
    viewModel: QuizViewModel,
    onQuizSubmitted: () -> Unit,
    onCancel: () -> Unit
) {
    val activeState by viewModel.activeQuizState.collectAsState()
    val quizSet = activeState.quizSet
    val questions = activeState.questions

    var showSubmitDialog by remember { mutableStateOf(false) }

    // Auto-submit countdown trigger safeguard in Compose runtime
    LaunchedEffect(activeState.timeRemainingSeconds, activeState.isTimerRunning) {
        if (activeState.timeRemainingSeconds <= 0 && activeState.isTimerRunning && !activeState.isSubmitting && !activeState.isSubmitted) {
            showSubmitDialog = false
            viewModel.submitActiveQuiz()
        }
    }

    LaunchedEffect(activeState.isSubmitted, activeState.completedAttempt) {
        if (activeState.isSubmitted && activeState.completedAttempt != null) {
            showSubmitDialog = false
            onQuizSubmitted()
        }
    }

    if (quizSet == null || questions.isEmpty()) {
        ShimmerQuizTakingSkeleton()
        return
    }

    val currentQuestion = questions.getOrNull(activeState.currentQuestionIndex) ?: return
    val selectedChoice = activeState.userAnswers[currentQuestion.id]

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Column {
                            Text(text = quizSet.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(text = "Active Examination", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Default.Close, contentDescription = "Exit")
                        }
                    },
                    actions = {
                        // Quick submit button always accessible in the top app bar
                        FilledTonalButton(
                            onClick = { showSubmitDialog = true },
                            enabled = !activeState.isSubmitting,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("submit_quiz_top_btn"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            if (activeState.isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Submit", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        CompactCountdownTimerChip(
                            timeRemainingSeconds = activeState.timeRemainingSeconds,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                )

                // Smooth dynamic progress bar at the top of the Active Quiz screen
                QuizTopProgressBar(
                    currentIndex = activeState.currentQuestionIndex,
                    totalQuestions = questions.size,
                    answeredCount = activeState.userAnswers.size
                )
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.goToPreviousQuestion() },
                        enabled = activeState.currentQuestionIndex > 0 && !activeState.isSubmitting
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Previous")
                    }

                    if (activeState.currentQuestionIndex == questions.size - 1) {
                        Button(
                            onClick = { showSubmitDialog = true },
                            enabled = !activeState.isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("submit_quiz_final_btn")
                        ) {
                            if (activeState.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Submitting...")
                            } else {
                                Text("Submit Test", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                            }
                        }
                    } else {
                        Button(
                            onClick = { viewModel.goToNextQuestion() },
                            enabled = !activeState.isSubmitting
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Prominent Countdown Timer Component with Urgency Animations & Auto-Submit
            QuizCountdownTimerCard(
                timeRemainingSeconds = activeState.timeRemainingSeconds,
                totalDurationSeconds = (quizSet.durationMinutes * 60).coerceAtLeast(60),
                isTimerRunning = activeState.isTimerRunning && !activeState.isSubmitting && !activeState.isSubmitted,
                onTimeExpired = {
                    showSubmitDialog = false
                    if (!activeState.isSubmitting && !activeState.isSubmitted) {
                        viewModel.submitActiveQuiz()
                    }
                }
            )

            // Question Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "Question ${activeState.currentQuestionIndex + 1}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = currentQuestion.questionText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp
                    )
                }
            }

            Text("Select Answer Choice:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            // Multiple Choice Options
            val choices = listOf(
                0 to currentQuestion.optionA,
                1 to currentQuestion.optionB,
                2 to currentQuestion.optionC,
                3 to currentQuestion.optionD
            )

            choices.forEach { (index, text) ->
                val isSelected = selectedChoice == index
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectAnswer(currentQuestion.id, index) }
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (index) {
                                    0 -> "A"
                                    1 -> "B"
                                    2 -> "C"
                                    else -> "D"
                                },
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )

                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.selectAnswer(currentQuestion.id, index) }
                        )
                    }
                }
            }
        }
    }

    if (showSubmitDialog) {
        val answeredCount = activeState.userAnswers.size
        val unansweredCount = questions.size - answeredCount

        AlertDialog(
            onDismissRequest = {
                if (!activeState.isSubmitting) showSubmitDialog = false
            },
            icon = {
                Icon(
                    Icons.Default.TaskAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Submit Quiz Assessment?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "You have answered $answeredCount of ${questions.size} questions.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (unansweredCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$unansweredCount unanswered question(s). Unanswered questions will be scored as zero.",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Text(
                        "Your answers will be graded immediately and recorded securely to Cloud Firestore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitActiveQuiz()
                    },
                    enabled = !activeState.isSubmitting,
                    modifier = Modifier.testTag("confirm_submit_quiz_btn")
                ) {
                    if (activeState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Submitting...")
                    } else {
                        Text("Confirm & Submit", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSubmitDialog = false },
                    enabled = !activeState.isSubmitting
                ) {
                    Text("Review Questions")
                }
            }
        )
    }

    // Modal dialog notifying user that time expired and auto-submission is executing
    if (activeState.timeRemainingSeconds <= 0 && (activeState.isSubmitting || (!activeState.isSubmitted && activeState.completedAttempt == null))) {
        TimeExpiredAutoSubmitDialog(isSubmitting = activeState.isSubmitting)
    }
}
