package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BentoEmerald
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoSecondary
import com.example.ui.theme.BentoViolet
import kotlin.math.roundToInt

/**
 * Polished, animated progress bar header designed for the top of the Active Quiz screen.
 * Dynamically and smoothly updates with physics-based spring animation as the student
 * moves forward or backward between questions.
 */
@Composable
fun QuizTopProgressBar(
    currentIndex: Int,
    totalQuestions: Int,
    answeredCount: Int,
    modifier: Modifier = Modifier,
    showDetailsHeader: Boolean = true
) {
    val safeTotal = totalQuestions.coerceAtLeast(1)
    val currentStep = (currentIndex + 1).coerceIn(1, safeTotal)
    val targetProgress = (currentStep.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)

    // Smooth fluid animation when navigating between questions
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = 220f
        ),
        label = "smooth_quiz_progress"
    )

    // Shimmer sheen animation moving along the bar for subtle visual delight
    val infiniteTransition = rememberInfiniteTransition(label = "progress_shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sheen_offset"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quiz_top_progress_container"),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showDetailsHeader) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Question Stepper with animated counter transitions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BentoPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = BentoPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Question ",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            AnimatedContent(
                                targetState = currentStep,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        (slideInVertically { height -> height } + fadeIn()).togetherWith(
                                            slideOutVertically { height -> -height } + fadeOut()
                                        )
                                    } else {
                                        (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                                            slideOutVertically { height -> height } + fadeOut()
                                        )
                                    }
                                },
                                label = "step_number_anim"
                            ) { step ->
                                Text(
                                    text = "$step",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoPrimary
                                )
                            }
                            Text(
                                text = " of $safeTotal",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Progress indicators: Answered Pill & Completion Percentage
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (answeredCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BentoEmerald.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = BentoEmerald,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "$answeredCount/$safeTotal",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoEmerald,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Percentage chip
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BentoPrimary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "${(animatedProgress * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = BentoPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Smooth Animated Progress Track
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(percent = 50)
                    )
            ) {
                val fullWidth = maxWidth

                if (animatedProgress > 0f) {
                    val fillWidth = fullWidth * animatedProgress

                    // Gradient Progress Fill
                    Box(
                        modifier = Modifier
                            .width(fillWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(percent = 50))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        BentoPrimary,
                                        BentoViolet,
                                        BentoSecondary
                                    )
                                )
                            )
                    ) {
                        // Shimmer highlight effect across progress
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(60.dp)
                                .offset(x = (fillWidth.value * shimmerOffset).dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.35f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }

                    // Active pulse indicator at the head of the progress bar
                    if (animatedProgress > 0.05f) {
                        Box(
                            modifier = Modifier
                                .offset(x = fillWidth - 5.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.5.dp, BentoSecondary, CircleShape)
                                .shadow(2.dp, CircleShape)
                        )
                    }
                }
            }
        }
    }
}
