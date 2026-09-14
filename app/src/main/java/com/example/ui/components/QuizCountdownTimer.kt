package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Visual Urgency Level for the Quiz Timer.
 */
enum class TimerUrgencyLevel {
    NORMAL,    // > 25% and > 90 seconds
    WARNING,   // <= 25% or <= 90 seconds
    CRITICAL,  // <= 30 seconds (pulsing alert)
    EXPIRED    // 0 seconds (auto-submitting)
}

/**
 * Dedicated, prominent countdown timer component for the active quiz screen.
 * Automatically tracks countdown progress, changes color/animations based on urgency,
 * triggers haptic reminders at key milestones, and triggers [onTimeExpired] when time reaches 0.
 */
@Composable
fun QuizCountdownTimerCard(
    timeRemainingSeconds: Int,
    totalDurationSeconds: Int,
    isTimerRunning: Boolean,
    onTimeExpired: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val totalTime = totalDurationSeconds.coerceAtLeast(1)
    val remaining = timeRemainingSeconds.coerceAtLeast(0)

    // Calculate urgency tier
    val urgencyLevel = when {
        remaining <= 0 -> TimerUrgencyLevel.EXPIRED
        remaining <= 30 -> TimerUrgencyLevel.CRITICAL
        remaining <= 90 || (remaining.toFloat() / totalTime.toFloat()) <= 0.25f -> TimerUrgencyLevel.WARNING
        else -> TimerUrgencyLevel.NORMAL
    }

    // Auto-submission trigger when countdown hits zero
    LaunchedEffect(remaining, isTimerRunning) {
        if (remaining <= 0 && isTimerRunning) {
            triggerTimerHaptic(context, isExpired = true)
            onTimeExpired()
        }
    }

    // Milestone haptic alert cues (at 60s, 30s, and 10s)
    LaunchedEffect(remaining) {
        if (remaining == 60 || remaining == 30 || remaining == 10) {
            triggerTimerHaptic(context, isExpired = false)
        }
    }

    // Animated pulse effect for critical urgency
    val infiniteTransition = rememberInfiniteTransition(label = "timer_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (urgencyLevel == TimerUrgencyLevel.CRITICAL) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "critical_pulse_scale"
    )

    val progress = (remaining.toFloat() / totalTime.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "timer_progress"
    )

    val minutes = remaining / 60
    val seconds = remaining % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    val containerColor = when (urgencyLevel) {
        TimerUrgencyLevel.EXPIRED -> Color(0xFFFFEBEE)
        TimerUrgencyLevel.CRITICAL -> Color(0xFFFFF1F2)
        TimerUrgencyLevel.WARNING -> Color(0xFFFFFBEB)
        TimerUrgencyLevel.NORMAL -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }

    val accentColor = when (urgencyLevel) {
        TimerUrgencyLevel.EXPIRED -> BentoRose
        TimerUrgencyLevel.CRITICAL -> BentoRose
        TimerUrgencyLevel.WARNING -> BentoAmber
        TimerUrgencyLevel.NORMAL -> BentoPrimary
    }

    val borderColor = when (urgencyLevel) {
        TimerUrgencyLevel.EXPIRED -> BentoRose.copy(alpha = 0.6f)
        TimerUrgencyLevel.CRITICAL -> BentoRose.copy(alpha = 0.5f)
        TimerUrgencyLevel.WARNING -> BentoAmber.copy(alpha = 0.4f)
        TimerUrgencyLevel.NORMAL -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(pulseScale)
            .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
            .testTag("quiz_countdown_timer_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (urgencyLevel == TimerUrgencyLevel.CRITICAL) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side: Timer Icon & Monospace Time Readout
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (urgencyLevel) {
                                TimerUrgencyLevel.EXPIRED -> Icons.Default.TimerOff
                                TimerUrgencyLevel.CRITICAL -> Icons.Default.HourglassTop
                                TimerUrgencyLevel.WARNING -> Icons.Default.HourglassBottom
                                TimerUrgencyLevel.NORMAL -> Icons.Default.Timer
                            },
                            contentDescription = "Timer status",
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (urgencyLevel == TimerUrgencyLevel.EXPIRED) "00:00" else formattedTime,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            ),
                            fontWeight = FontWeight.Black,
                            color = accentColor
                        )
                        Text(
                            text = "TIME REMAINING",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                // Right side: Urgency Status Badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = accentColor.copy(alpha = 0.14f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (urgencyLevel == TimerUrgencyLevel.CRITICAL || urgencyLevel == TimerUrgencyLevel.EXPIRED) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                        }
                        Text(
                            text = when (urgencyLevel) {
                                TimerUrgencyLevel.EXPIRED -> "TIME UP: SUBMITTING"
                                TimerUrgencyLevel.CRITICAL -> "FINAL 30 SECONDS!"
                                TimerUrgencyLevel.WARNING -> "PACE UP"
                                TimerUrgencyLevel.NORMAL -> "ON PACE"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Smooth Countdown Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accentColor.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    accentColor,
                                    if (urgencyLevel == TimerUrgencyLevel.NORMAL) BentoCyan else accentColor
                                )
                            )
                        )
                )
            }

            // Subtitle helper hint
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (urgencyLevel) {
                        TimerUrgencyLevel.EXPIRED -> "Auto-submitting test now. Please wait..."
                        TimerUrgencyLevel.CRITICAL -> "Answers will auto-submit when timer expires"
                        TimerUrgencyLevel.WARNING -> "Tip: Review any skipped questions before time runs out"
                        TimerUrgencyLevel.NORMAL -> "Auto-submits automatically at 00:00"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = if (urgencyLevel == TimerUrgencyLevel.CRITICAL || urgencyLevel == TimerUrgencyLevel.EXPIRED) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(progress * 100).toInt()}% left",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = accentColor
                )
            }
        }
    }
}

/**
 * Compact countdown chip for top bars.
 */
@Composable
fun CompactCountdownTimerChip(
    timeRemainingSeconds: Int,
    modifier: Modifier = Modifier
) {
    val remaining = timeRemainingSeconds.coerceAtLeast(0)
    val mins = remaining / 60
    val secs = remaining % 60
    val formattedTime = String.format("%02d:%02d", mins, secs)

    val isUrgent = remaining <= 60
    val isCritical = remaining <= 30

    val chipBg = when {
        isCritical -> Color(0xFFFFEBEE)
        isUrgent -> Color(0xFFFFFBEB)
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val chipContent = when {
        isCritical -> BentoRose
        isUrgent -> BentoAmber
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = chipBg,
        modifier = modifier.testTag("compact_quiz_timer_chip")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = if (isCritical) Icons.Default.HourglassTop else Icons.Default.Timer,
                contentDescription = "Remaining Time",
                tint = chipContent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = formattedTime,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = chipContent,
                style = MaterialTheme.typography.titleSmall,
                fontSize = 13.sp
            )
        }
    }
}

/**
 * Modal dialog displayed when quiz timer expires to notify user that auto-submission is in progress.
 */
@Composable
fun TimeExpiredAutoSubmitDialog(
    isSubmitting: Boolean
) {
    AlertDialog(
        onDismissRequest = { /* Non-dismissible: user must await auto-submit completion */ },
        icon = {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(BentoRose.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TimerOff,
                    contentDescription = null,
                    tint = BentoRose,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = "Time's Up!",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "The countdown timer for this quiz has expired. Your current answers are being collected and automatically submitted now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = BentoRose
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isSubmitting) "Grading & recording score..." else "Finalizing submission...",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = BentoRose
                        )
                    }
                }
            }
        },
        confirmButton = {
            // No manual buttons needed since submission finishes asynchronously and closes screen
        }
    )
}

private fun triggerTimerHaptic(context: Context, isExpired: Boolean) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            val vibrator = vibratorManager?.defaultVibrator
            if (isExpired) {
                // Double long vibration for expiration
                val pattern = longArrayOf(0, 150, 80, 250)
                val amplitudes = intArrayOf(0, 220, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                // Short warning pulse
                vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isExpired) {
                    val pattern = longArrayOf(0, 150, 80, 250)
                    val amplitudes = intArrayOf(0, 220, 0, 255)
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
                } else {
                    vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(if (isExpired) 400 else 100)
            }
        }
    } catch (_: Exception) {
        // Safe ignore if vibrator is unavailable
    }
}
