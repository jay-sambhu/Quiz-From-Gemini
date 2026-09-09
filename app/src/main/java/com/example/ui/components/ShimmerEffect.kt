package com.example.ui.components

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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Creates an animated linear gradient brush for high-performance, subtle shimmer effects.
 */
@Composable
fun rememberShimmerBrush(
    targetValue: Float = 1300f,
    durationMillis: Int = 1300,
    baseColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    highlightColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    return Brush.linearGradient(
        colors = listOf(
            baseColor,
            highlightColor,
            baseColor
        ),
        start = Offset(x = translateAnimation - 350f, y = translateAnimation - 350f),
        end = Offset(x = translateAnimation + 150f, y = translateAnimation + 150f)
    )
}

/**
 * Modifier extension to apply a shimmer background to any composable shape.
 */
fun Modifier.shimmerEffect(
    enabled: Boolean = true,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
    durationMillis: Int = 1300
): Modifier = composed {
    if (!enabled) return@composed this
    val brush = rememberShimmerBrush(durationMillis = durationMillis)
    this
        .clip(shape)
        .background(brush)
}

/**
 * Basic rectangular shimmer placeholder box.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    brush: Brush = rememberShimmerBrush()
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

/**
 * Full skeleton representation of a multiple-choice question card,
 * providing immediate visual structure while Gemini AI synthesizes questions.
 */
@Composable
fun ShimmerQuestionCardSkeleton(
    modifier: Modifier = Modifier,
    questionIndex: Int = 1,
    brush: Brush = rememberShimmerBrush()
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(
                    BentoPrimary.copy(alpha = 0.25f),
                    BentoViolet.copy(alpha = 0.15f)
                )
            ),
            width = 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row (Question Number badge + Difficulty Tag skeleton)
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
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(brush)
                    )
                    ShimmerBox(
                        modifier = Modifier
                            .width(85.dp)
                            .height(14.dp),
                        brush = brush
                    )
                }

                ShimmerBox(
                    modifier = Modifier
                        .width(55.dp)
                        .height(20.dp),
                    cornerRadius = 10.dp,
                    brush = brush
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Question Text skeleton lines
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(16.dp),
                    cornerRadius = 6.dp,
                    brush = brush
                )
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(16.dp),
                    cornerRadius = 6.dp,
                    brush = brush
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 4 Option Choices skeleton
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val widths = listOf(0.75f, 0.88f, 0.60f, 0.70f)
                val labels = listOf("A", "B", "C", "D")
                for (i in 0..3) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = brush,
                            width = 1.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(BentoPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = labels[i],
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoPrimary.copy(alpha = 0.6f)
                                )
                            }
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth(widths[i])
                                    .height(14.dp),
                                cornerRadius = 6.dp,
                                brush = brush
                            )
                        }
                    }
                }
            }

            // Explanation Skeleton Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.80f)
                        .height(12.dp),
                    cornerRadius = 4.dp,
                    brush = brush
                )
            }
        }
    }
}

/**
 * Step progress tracker and shimmer loading presentation for the AI Question Generator.
 * Displays real-time progress steps, dynamic progress bar, and simulated question preview skeletons.
 */
@Composable
fun AiQuestionGenerationProgress(
    topic: String,
    difficulty: String,
    targetCount: Int = 5,
    currentStepIndex: Int = 1,
    progressPercentage: Float = 0.45f,
    modifier: Modifier = Modifier
) {
    val shimmerBrush = rememberShimmerBrush()

    val steps = listOf(
        "Calibrating $difficulty pedagogy for '$topic'",
        "Querying Gemini 3.5 Flash neural models",
        "Crafting 4 options & plausible distractors",
        "Synthesizing in-depth explanations & answer keys",
        "Publishing to Question Bank & Cloud Firestore"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Banner Header with Gemini Sparkle Badge
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = BentoViolet.copy(alpha = 0.08f),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(
                    listOf(BentoViolet.copy(alpha = 0.5f), BentoCyan.copy(alpha = 0.5f))
                ),
                width = 1.dp
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
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
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(BentoViolet, BentoCyan)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Gemini AI Question Generator",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Topic: $topic • $difficulty ($targetCount Qs)",
                                style = MaterialTheme.typography.labelSmall,
                                color = BentoViolet,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Progress percentage pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BentoViolet.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${(progressPercentage * 100).toInt()}%",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = BentoViolet
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Animated Gradient Progress Indicator
                LinearProgressIndicator(
                    progress = { progressPercentage.coerceIn(0.05f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = BentoViolet,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Active Step Indicator with dynamic pulse
                val safeIndex = currentStepIndex.coerceIn(0, steps.size - 1)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = BentoCyan
                    )
                    Text(
                        text = steps[safeIndex],
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Shimmer Question Card Preview Skeleton
        Text(
            text = "PREVIEWING SKELETON QUESTION STRUCTURE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            letterSpacing = 0.5.sp
        )

        ShimmerQuestionCardSkeleton(
            brush = shimmerBrush
        )
    }
}

/**
 * Skeleton screen displayed on Quiz Taking screen while questions load.
 */
@Composable
fun ShimmerQuizTakingSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ShimmerBox(modifier = Modifier.width(180.dp).height(20.dp), brush = brush)
                ShimmerBox(modifier = Modifier.width(100.dp).height(14.dp), brush = brush)
            }
            ShimmerBox(modifier = Modifier.width(70.dp).height(32.dp), cornerRadius = 12.dp, brush = brush)
        }

        // Timer and progress bar skeleton
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(8.dp), cornerRadius = 4.dp, brush = brush)

        Spacer(modifier = Modifier.height(8.dp))

        // Question Card Skeleton
        ShimmerQuestionCardSkeleton(brush = brush)

        Spacer(modifier = Modifier.weight(1f))

        // Navigation buttons skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBox(modifier = Modifier.weight(1f).height(48.dp), cornerRadius = 12.dp, brush = brush)
            ShimmerBox(modifier = Modifier.weight(1f).height(48.dp), cornerRadius = 12.dp, brush = brush)
        }
    }
}
