package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Consistent, modern Material 3 'No Data' empty state component.
 *
 * Designed to provide clear, actionable, and visually polished feedback
 * when there are no quiz results, past attempts, or leaderboard entries available.
 */
@Composable
fun EmptyDataState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    iconTint: Color = BentoViolet,
    badgeText: String? = null,
    badgeColor: Color = BentoViolet,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null,
    tipText: String? = null,
    isCompact: Boolean = false,
    testTag: String = "empty_data_state"
) {
    val outerIconSize = if (isCompact) 56.dp else 72.dp
    val innerIconSize = if (isCompact) 26.dp else 34.dp
    val verticalSpacing = if (isCompact) 8.dp else 12.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
            .padding(horizontal = if (isCompact) 12.dp else 20.dp, vertical = if (isCompact) 12.dp else 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. Decorative Circular Icon Aura
        Box(
            modifier = Modifier
                .size(outerIconSize)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            iconTint.copy(alpha = 0.22f),
                            iconTint.copy(alpha = 0.06f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = iconTint.copy(alpha = 0.35f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(outerIconSize - 16.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(innerIconSize)
                )
            }
        }

        Spacer(modifier = Modifier.height(verticalSpacing))

        // 2. Optional Badge Tag
        if (badgeText != null) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = badgeColor.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f)),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 3. Title
        Text(
            text = title,
            style = if (isCompact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 4. Description
        Text(
            text = description,
            style = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = if (isCompact) 18.sp else 21.sp,
            modifier = Modifier
                .widthIn(max = 340.dp)
                .padding(horizontal = 4.dp)
        )

        // 5. Actions (Primary & Optional Secondary)
        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 16.dp))
            Button(
                onClick = onActionClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BentoPrimary,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("${testTag}_action_btn")
            ) {
                if (actionIcon != null) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = actionLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        if (secondaryActionLabel != null && onSecondaryActionClick != null) {
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = onSecondaryActionClick,
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .testTag("${testTag}_secondary_btn")
            ) {
                Text(
                    text = secondaryActionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = BentoViolet
                )
            }
        }

        // 6. Optional Helpful Tip Callout
        if (tipText != null) {
            Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 14.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier.widthIn(max = 360.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = BentoAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tipText,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Card container wrapping [EmptyDataState] in a styled BentoCard.
 */
@Composable
fun EmptyDataStateCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    iconTint: Color = BentoViolet,
    badgeText: String? = null,
    badgeColor: Color = BentoViolet,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null,
    tipText: String? = null,
    isCompact: Boolean = false,
    cornerRadius: Dp = 20.dp,
    testTag: String = "empty_data_card"
) {
    BentoCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        cornerRadius = cornerRadius,
        padding = 0.dp
    ) {
        EmptyDataState(
            icon = icon,
            title = title,
            description = description,
            iconTint = iconTint,
            badgeText = badgeText,
            badgeColor = badgeColor,
            actionLabel = actionLabel,
            actionIcon = actionIcon,
            onActionClick = onActionClick,
            secondaryActionLabel = secondaryActionLabel,
            onSecondaryActionClick = onSecondaryActionClick,
            tipText = tipText,
            isCompact = isCompact,
            testTag = testTag
        )
    }
}

// -------------------------------------------------------------------------
// PRESET CONVENIENCE EMPTY STATES FOR QUIZ RESULTS AND LEADERBOARDS
// -------------------------------------------------------------------------

/**
 * Empty state displayed when there are no quiz results or past attempts yet.
 */
@Composable
fun NoQuizResultsEmptyState(
    modifier: Modifier = Modifier,
    onTakeQuiz: (() -> Unit)? = null,
    customTitle: String? = null,
    customDescription: String? = null,
    isCard: Boolean = true,
    isCompact: Boolean = false,
    testTag: String = "no_quiz_results_empty_state"
) {
    val title = customTitle ?: "No Quiz Results Yet"
    val description = customDescription
        ?: "You haven't completed any assessments yet. Test your knowledge to view score breakdowns, accuracy rates, and question review explanations."

    if (isCard) {
        EmptyDataStateCard(
            icon = Icons.Default.Quiz,
            iconTint = BentoViolet,
            badgeText = "Awaiting Submissions",
            badgeColor = BentoViolet,
            title = title,
            description = description,
            actionLabel = if (onTakeQuiz != null) "Take Your First Quiz" else null,
            actionIcon = if (onTakeQuiz != null) Icons.Default.PlayArrow else null,
            onActionClick = onTakeQuiz,
            tipText = "Your scores, pass status, and correct answer breakdowns will appear here instantly.",
            isCompact = isCompact,
            testTag = testTag,
            modifier = modifier
        )
    } else {
        EmptyDataState(
            icon = Icons.Default.Quiz,
            iconTint = BentoViolet,
            badgeText = "Awaiting Submissions",
            badgeColor = BentoViolet,
            title = title,
            description = description,
            actionLabel = if (onTakeQuiz != null) "Take Your First Quiz" else null,
            actionIcon = if (onTakeQuiz != null) Icons.Default.PlayArrow else null,
            onActionClick = onTakeQuiz,
            tipText = "Your scores, pass status, and correct answer breakdowns will appear here instantly.",
            isCompact = isCompact,
            testTag = testTag,
            modifier = modifier
        )
    }
}

/**
 * Empty state displayed when there are no leaderboard entries or class rankings yet.
 */
@Composable
fun NoLeaderboardEmptyState(
    modifier: Modifier = Modifier,
    onTakeQuizToClimb: (() -> Unit)? = null,
    customTitle: String? = null,
    customDescription: String? = null,
    isCard: Boolean = true,
    isCompact: Boolean = false,
    testTag: String = "no_leaderboard_empty_state"
) {
    val title = customTitle ?: "No Leaderboard Entries Yet"
    val description = customDescription
        ?: "No students have submitted quiz attempts yet. Be the first scholar to complete an assessment and claim the #1 spot on the podium!"

    if (isCard) {
        EmptyDataStateCard(
            icon = Icons.Default.EmojiEvents,
            iconTint = BentoAmber,
            badgeText = "Season Open",
            badgeColor = BentoAmber,
            title = title,
            description = description,
            actionLabel = if (onTakeQuizToClimb != null) "Take Quiz to Climb" else null,
            actionIcon = if (onTakeQuizToClimb != null) Icons.AutoMirrored.Filled.TrendingUp else null,
            onActionClick = onTakeQuizToClimb,
            tipText = "Rankings recalculate in real-time as classmates finish quizzes.",
            isCompact = isCompact,
            testTag = testTag,
            modifier = modifier
        )
    } else {
        EmptyDataState(
            icon = Icons.Default.EmojiEvents,
            iconTint = BentoAmber,
            badgeText = "Season Open",
            badgeColor = BentoAmber,
            title = title,
            description = description,
            actionLabel = if (onTakeQuizToClimb != null) "Take Quiz to Climb" else null,
            actionIcon = if (onTakeQuizToClimb != null) Icons.AutoMirrored.Filled.TrendingUp else null,
            onActionClick = onTakeQuizToClimb,
            tipText = "Rankings recalculate in real-time as classmates finish quizzes.",
            isCompact = isCompact,
            testTag = testTag,
            modifier = modifier
        )
    }
}

/**
 * Empty state displayed when teacher analytics has no quiz results evaluated yet.
 */
@Composable
fun NoTeacherAnalyticsEmptyState(
    modifier: Modifier = Modifier,
    isCard: Boolean = true,
    isCompact: Boolean = false,
    testTag: String = "no_teacher_analytics_empty_state"
) {
    val title = "No Quiz Results Submitted Yet"
    val description = "Students haven't submitted any quiz attempts yet. Once students take assessments, average scores, passing rates, and performance analytics will appear here."

    if (isCard) {
        EmptyDataStateCard(
            icon = Icons.Default.Assessment,
            iconTint = BentoSecondary,
            badgeText = "Class Inactive",
            badgeColor = BentoSecondary,
            title = title,
            description = description,
            tipText = "Share available quizzes with students to start gathering performance analytics.",
            isCompact = isCompact,
            testTag = testTag,
            modifier = modifier
        )
    } else {
        EmptyDataState(
            icon = Icons.Default.Assessment,
            iconTint = BentoSecondary,
            badgeText = "Class Inactive",
            badgeColor = BentoSecondary,
            title = title,
            description = description,
            tipText = "Share available quizzes with students to start gathering performance analytics.",
            isCompact = isCompact,
            testTag = testTag,
            modifier = modifier
        )
    }
}
