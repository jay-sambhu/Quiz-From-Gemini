package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.QuizViewModel
import com.example.ui.StudentLeaderboardEntry
import com.example.ui.theme.*

enum class LeaderboardSortCriterion {
    TOTAL_POINTS,
    ACCURACY_RATE
}

/**
 * Modern Bento-style Top Students Leaderboard Component for the Student Dashboard.
 * Integrates live cloud data from Firestore via QuizViewModel.leaderboardEntries.
 */
@Composable
fun TopStudentsLeaderboardComponent(
    viewModel: QuizViewModel,
    modifier: Modifier = Modifier,
    isCompactPreview: Boolean = false,
    onViewFullLeaderboard: (() -> Unit)? = null,
    onTakeQuizToClimb: (() -> Unit)? = null
) {
    val rawEntries by viewModel.leaderboardEntries.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isCloudConnected by viewModel.isCloudConnected.collectAsState()
    val cloudSyncMessage by viewModel.cloudSyncMessage.collectAsState()

    var sortCriterion by remember { mutableStateOf(LeaderboardSortCriterion.TOTAL_POINTS) }
    val isSyncingFirestore by viewModel.isSyncingFirestore.collectAsState()

    // Sort entries according to selected criterion (Total Points vs. Accuracy %)
    val sortedEntries = remember(rawEntries, sortCriterion) {
        val list = when (sortCriterion) {
            LeaderboardSortCriterion.TOTAL_POINTS -> rawEntries.sortedWith(
                compareByDescending<StudentLeaderboardEntry> { it.totalScore }
                    .thenByDescending { it.averagePercentage }
            )
            LeaderboardSortCriterion.ACCURACY_RATE -> rawEntries.sortedWith(
                compareByDescending<StudentLeaderboardEntry> { it.averagePercentage }
                    .thenByDescending { it.totalScore }
            )
        }
        val totalCount = list.size
        list.mapIndexed { index, item ->
            val rank = index + 1
            val percentile = if (totalCount > 1) {
                ((totalCount - rank).toFloat() / (totalCount - 1).toFloat()) * 100f
            } else 100f
            item.copy(rank = rank, percentile = percentile)
        }
    }

    val myId = currentUser?.id ?: ""
    val myEntry = remember(sortedEntries, myId) {
        sortedEntries.find { it.studentId == myId }
    }

    // Top 3 Podium
    val top1 = sortedEntries.getOrNull(0)
    val top2 = sortedEntries.getOrNull(1)
    val top3 = sortedEntries.getOrNull(2)

    // Remainder list
    val listToDisplay = if (isCompactPreview) {
        // Show up to 3 or 4 students in compact preview
        sortedEntries.take(4)
    } else {
        sortedEntries
    }

    BentoCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("top_students_leaderboard_card"),
        borderColor = BentoViolet.copy(alpha = 0.35f),
        cornerRadius = 24.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header with Title, Firestore live sync indicator & Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(BentoAmber, Color(0xFFF59E0B))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = "Leaderboard",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Top Students",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isCloudConnected) BentoEmerald else BentoAmber)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isCloudConnected) "Cloud Firestore Live" else "Cached Data",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCloudConnected) BentoEmerald else BentoAmber,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Cloud Refresh button & action
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            viewModel.syncWithFirestoreCloud()
                        },
                        enabled = !isSyncingFirestore,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .testTag("refresh_leaderboard_firestore_btn")
                    ) {
                        if (isSyncingFirestore) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = BentoEmerald
                            )
                        } else {
                            Icon(
                                Icons.Default.CloudSync,
                                contentDescription = "Sync from Firestore",
                                tint = if (isCloudConnected) BentoEmerald else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (isCompactPreview && onViewFullLeaderboard != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(
                            onClick = onViewFullLeaderboard,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("see_all_top_students_btn")
                        ) {
                            Text(
                                text = "See All",
                                fontWeight = FontWeight.Bold,
                                color = BentoPrimary,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = BentoPrimary
                            )
                        }
                    }
                }
            }

            // 2. Sort Criterion Pills (Points vs Accuracy) - only in full mode or expandable
            if (!isCompactPreview) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = sortCriterion == LeaderboardSortCriterion.TOTAL_POINTS,
                        onClick = { sortCriterion = LeaderboardSortCriterion.TOTAL_POINTS },
                        label = { Text("Total Points", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (sortCriterion == LeaderboardSortCriterion.TOTAL_POINTS) BentoAmber else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        shape = RoundedCornerShape(10.dp)
                    )

                    FilterChip(
                        selected = sortCriterion == LeaderboardSortCriterion.ACCURACY_RATE,
                        onClick = { sortCriterion = LeaderboardSortCriterion.ACCURACY_RATE },
                        label = { Text("Accuracy Rate", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (sortCriterion == LeaderboardSortCriterion.ACCURACY_RATE) BentoEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // 3. Top 3 Scholars Podium (Visual Standings)
            if (sortedEntries.isNotEmpty()) {
                PodiumSection(
                    top1 = top1,
                    top2 = top2,
                    top3 = top3,
                    currentUserId = myId
                )
            } else {
                NoLeaderboardEmptyState(
                    onTakeQuizToClimb = onTakeQuizToClimb,
                    isCard = false,
                    isCompact = isCompactPreview,
                    testTag = "top_students_leaderboard_empty_state"
                )
            }

            // 4. Ranked List of Students
            if (listToDisplay.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listToDisplay.forEach { entry ->
                        val isMe = entry.studentId == myId
                        TopStudentRowItem(
                            entry = entry,
                            isMe = isMe,
                            sortCriterion = sortCriterion
                        )
                    }
                }
            }

            // 5. "Your Standing" Comparison Card (Sticky at bottom if current user is not #1)
            if (myEntry != null && myEntry.rank > 1) {
                val leader = sortedEntries.firstOrNull()
                val pointsDiff = if (leader != null) (leader.totalScore - myEntry.totalScore).coerceAtLeast(1) else 1

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = BentoViolet.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BentoViolet.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(BentoViolet),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#${myEntry.rank}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = "Your Standing: Rank #${myEntry.rank}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoViolet
                                )
                                Text(
                                    text = "$pointsDiff pts behind #1 (${leader?.studentName?.split(" ")?.firstOrNull() ?: "Leader"}) • ${String.format("%.0f", myEntry.percentile)}th percentile",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (onTakeQuizToClimb != null) {
                            Button(
                                onClick = onTakeQuizToClimb,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BentoViolet),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("climb_ranks_quiz_btn")
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Climb Ranks", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// PODIUM COMPONENT (Visual 2nd - 1st - 3rd Pedestals)
// -------------------------------------------------------------
@Composable
private fun PodiumSection(
    top1: StudentLeaderboardEntry?,
    top2: StudentLeaderboardEntry?,
    top3: StudentLeaderboardEntry?,
    currentUserId: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Rank 2 (Left)
        if (top2 != null) {
            PodiumPillar(
                entry = top2,
                rank = 2,
                height = 120.dp,
                accentColor = Color(0xFF94A3B8),
                badgeIcon = Icons.Default.MilitaryTech,
                isMe = top2.studentId == currentUserId,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // Rank 1 (Center - Elevated Champion)
        if (top1 != null) {
            PodiumPillar(
                entry = top1,
                rank = 1,
                height = 148.dp,
                accentColor = BentoAmber,
                badgeIcon = Icons.Default.EmojiEvents,
                isMe = top1.studentId == currentUserId,
                modifier = Modifier.weight(1.15f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1.15f))
        }

        // Rank 3 (Right)
        if (top3 != null) {
            PodiumPillar(
                entry = top3,
                rank = 3,
                height = 108.dp,
                accentColor = Color(0xFFD97706),
                badgeIcon = Icons.Default.WorkspacePremium,
                isMe = top3.studentId == currentUserId,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PodiumPillar(
    entry: StudentLeaderboardEntry,
    rank: Int,
    height: androidx.compose.ui.unit.Dp,
    accentColor: Color,
    badgeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isMe: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = accentColor.copy(alpha = if (rank == 1) 0.16f else 0.10f),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isMe || rank == 1) 2.dp else 1.dp,
            color = if (isMe) BentoViolet else accentColor.copy(alpha = 0.4f)
        ),
        modifier = modifier.height(height)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Rank badge / Medal
            Box(
                modifier = Modifier
                    .size(if (rank == 1) 34.dp else 28.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    badgeIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(if (rank == 1) 18.dp else 14.dp)
                )
            }

            // Student Name & YOU badge
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = entry.studentName.split(" ").firstOrNull() ?: entry.studentName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                if (isMe) {
                    Text(
                        text = "YOU",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = BentoViolet,
                        fontSize = 9.sp
                    )
                }
            }

            // Score & Accuracy Pills
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${entry.totalScore} pts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (rank == 1) BentoAmber else BentoPrimary
                )
                Text(
                    text = "${String.format("%.0f", entry.averagePercentage)}% acc",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// -------------------------------------------------------------
// INDIVIDUAL STUDENT LEADERBOARD ROW ITEM
// -------------------------------------------------------------
@Composable
private fun TopStudentRowItem(
    entry: StudentLeaderboardEntry,
    isMe: Boolean,
    sortCriterion: LeaderboardSortCriterion
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isMe) BentoViolet.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isMe) 1.5.dp else 1.dp,
            color = if (isMe) BentoViolet.copy(alpha = 0.5f) else Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Rank Badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when (entry.rank) {
                            1 -> BentoAmber
                            2 -> Color(0xFF94A3B8)
                            3 -> Color(0xFFD97706)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${entry.rank}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (entry.rank <= 3) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }

            // Student Profile Avatar
            UserProfileAvatar(
                name = entry.studentName,
                photoUrl = entry.photoUrl.ifBlank { null },
                size = 36.dp,
                backgroundColor = if (isMe) BentoViolet else BentoPrimary.copy(alpha = 0.75f)
            )

            // Student Information
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.studentName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(6.dp))
                        BentoPillTag(
                            text = "YOU",
                            containerColor = BentoViolet,
                            contentColor = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${entry.totalQuizzesTaken} quizzes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format("%.0f", entry.averagePercentage)}% accuracy",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (entry.averagePercentage >= 80f) BentoEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (entry.averagePercentage >= 80f) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }

            // Metric Value
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${entry.totalScore} pts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (entry.rank == 1) BentoAmber else BentoPrimary
                )
                Text(
                    text = "${String.format("%.0f", entry.percentile)}th %ile",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
