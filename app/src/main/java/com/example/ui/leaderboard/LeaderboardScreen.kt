package com.example.ui.leaderboard

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.local.entities.QuizAttemptEntity
import com.example.data.model.StudentLeaderboardEntry
import com.example.ui.QuizViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LeaderboardFilterRange(val label: String) {
    ALL("All Students"),
    TOP_10("Top 10"),
    TOP_25("Top 25"),
    PODIUM_ONLY("Podium (Top 3)")
}

enum class LeaderboardSortOrder(val label: String) {
    TOTAL_POINTS("Total Points"),
    ACCURACY("Accuracy %"),
    QUIZZES_TAKEN("Quizzes Taken")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: QuizViewModel,
    onTakeQuiz: (() -> Unit)? = null
) {
    // Cloud Firestore leaderboard state
    val effectiveLeaderboard by viewModel.effectiveLeaderboard.collectAsState()
    val isFetching by viewModel.isFetchingLeaderboard.collectAsState()
    val lastFetchedTime by viewModel.leaderboardLastFetchedTime.collectAsState()
    val fetchError by viewModel.leaderboardFetchError.collectAsState()
    val isCloudConnected by viewModel.isCloudConnected.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val allAttempts by viewModel.allAttempts.collectAsState()

    // Local UI states for search, filters, sorting and detail modal
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(LeaderboardFilterRange.ALL) }
    var selectedSortOrder by remember { mutableStateOf(LeaderboardSortOrder.TOTAL_POINTS) }
    var selectedStudentForDetail by remember { mutableStateOf<StudentLeaderboardEntry?>(null) }

    // Fetch freshest data from Firestore on entry
    LaunchedEffect(Unit) {
        viewModel.fetchLeaderboardFromFirestore()
    }

    // Filter and sort the student roster
    val filteredEntries = remember(effectiveLeaderboard, searchQuery, selectedFilter, selectedSortOrder) {
        var list = effectiveLeaderboard

        // Search filter by name or email
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.trim().lowercase()
            list = list.filter {
                it.studentName.lowercase().contains(query) || it.studentEmail.lowercase().contains(query)
            }
        }

        // Apply secondary sort order if requested (default is TOTAL_POINTS which matches Firestore ranking)
        list = when (selectedSortOrder) {
            LeaderboardSortOrder.TOTAL_POINTS -> list.sortedWith(
                compareByDescending<StudentLeaderboardEntry> { it.totalScore }
                    .thenByDescending { it.averagePercentage }
                    .thenByDescending { it.totalQuizzesTaken }
            )
            LeaderboardSortOrder.ACCURACY -> list.sortedWith(
                compareByDescending<StudentLeaderboardEntry> { it.averagePercentage }
                    .thenByDescending { it.totalScore }
            )
            LeaderboardSortOrder.QUIZZES_TAKEN -> list.sortedWith(
                compareByDescending<StudentLeaderboardEntry> { it.totalQuizzesTaken }
                    .thenByDescending { it.totalScore }
            )
        }

        // Apply range filter
        when (selectedFilter) {
            LeaderboardFilterRange.ALL -> list
            LeaderboardFilterRange.TOP_10 -> list.take(10)
            LeaderboardFilterRange.TOP_25 -> list.take(25)
            LeaderboardFilterRange.PODIUM_ONLY -> list.take(3)
        }
    }

    // Top 3 Podium (from unfiltered list sorted by total points)
    val sortedByPoints = remember(effectiveLeaderboard) {
        effectiveLeaderboard.sortedWith(
            compareByDescending<StudentLeaderboardEntry> { it.totalScore }
                .thenByDescending { it.averagePercentage }
        )
    }
    val firstPlace = sortedByPoints.getOrNull(0)
    val secondPlace = sortedByPoints.getOrNull(1)
    val thirdPlace = sortedByPoints.getOrNull(2)

    // Current student's entry on the leaderboard
    val myStudentId = currentUser?.id
    val myEntry = remember(effectiveLeaderboard, myStudentId) {
        if (myStudentId != null) effectiveLeaderboard.find { it.studentId == myStudentId } else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BentoViolet.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = BentoViolet,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Classroom Leaderboard",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isCloudConnected) "Cloud Firestore • Live Sync" else "Local Cache Mode",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCloudConnected) BentoSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.fetchLeaderboardFromFirestore(forceRefreshToast = true) },
                        enabled = !isFetching,
                        modifier = Modifier.testTag("refresh_leaderboard_button")
                    ) {
                        if (isFetching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = BentoViolet
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Leaderboard from Firestore",
                                tint = BentoViolet
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isFetching,
            onRefresh = { viewModel.fetchLeaderboardFromFirestore(forceRefreshToast = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("leaderboard_pull_refresh")
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag("leaderboard_list"),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
            ) {
            // Bento Hero Banner with live ranking stats
            item {
                BentoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                    cornerRadius = 22.dp
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_leaderboard_banner_1785302532366),
                            contentDescription = "Leaderboard Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.35f),
                                            Color.Black.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BentoPillTag(
                                        text = "Ranked by Total Points",
                                        containerColor = BentoAmber,
                                        contentColor = Color.Black,
                                        icon = Icons.Default.EmojiEvents
                                    )

                                    val totalPointsSum = effectiveLeaderboard.sumOf { it.totalScore }
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "$totalPointsSum Total Pts",
                                            color = BentoAmber,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Top Performing Students",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Real-time points accumulated across all completed quiz attempts in Cloud Firestore.",
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Cloud Sync Status Bar
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isCloudConnected) BentoSuccess.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isCloudConnected) BentoSuccess.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isCloudConnected) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = if (isCloudConnected) BentoSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isCloudConnected) "Fetched from Cloud Firestore" else "Using Local Database Cache",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isCloudConnected) BentoSuccess else MaterialTheme.colorScheme.onSurface
                                )
                                val timeText = lastFetchedTime?.let {
                                    SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date(it))
                                } ?: "Just now"
                                Text(
                                    text = "Updated: $timeText • ${effectiveLeaderboard.size} students evaluated",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        TextButton(
                            onClick = { viewModel.fetchLeaderboardFromFirestore(forceRefreshToast = true) },
                            enabled = !isFetching,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            if (isFetching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = BentoViolet
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Syncing...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BentoViolet)
                            } else {
                                Text("Sync Now", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BentoViolet)
                            }
                        }
                    }
                }
            }

            // Error banner if Firestore fetch had an issue
            if (fetchError != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = BentoError.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BentoError.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = BentoError, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = fetchError ?: "Error syncing with Firestore",
                                style = MaterialTheme.typography.bodySmall,
                                color = BentoError,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.fetchLeaderboardFromFirestore(true) }) {
                                Text("Retry", color = BentoError, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // "Your Standing" Personal Card (if current user is a student)
            if (myEntry != null) {
                item {
                    val myRank = myEntry.rank
                    val totalClassSize = effectiveLeaderboard.size
                    val aheadOfMe = if (myRank > 1) sortedByPoints.getOrNull(myRank - 2) else null
                    val pointsDiffToNext = if (aheadOfMe != null) (aheadOfMe.totalScore - myEntry.totalScore) else 0

                    BentoCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = BentoViolet.copy(alpha = 0.12f),
                        borderColor = BentoViolet.copy(alpha = 0.45f),
                        cornerRadius = 20.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(BentoViolet),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = myEntry.studentName.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Your Standing",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            BentoPillTag(
                                                text = "YOU",
                                                containerColor = BentoViolet,
                                                contentColor = Color.White
                                            )
                                        }
                                        Text(
                                            text = "Rank #$myRank of $totalClassSize students",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    color = BentoViolet,
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "${myEntry.totalScore}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                        Text(
                                            text = "POINTS",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = BentoViolet.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    val statusIcon = if (myRank == 1) Icons.Default.EmojiEvents else Icons.AutoMirrored.Filled.TrendingUp
                                    val statusColor = if (myRank == 1) Color(0xFFF59E0B) else BentoPrimary
                                    Icon(
                                        imageVector = statusIcon,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (myRank == 1) {
                                            "Leading 1st Place! Keep it up!"
                                        } else if (aheadOfMe != null) {
                                            "${pointsDiffToNext + 1} pts behind #${myRank - 1} (${aheadOfMe.studentName})"
                                        } else {
                                            "Take quizzes to climb ranks!"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = "${String.format(Locale.getDefault(), "%.0f", myEntry.percentile)}th percentile",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoViolet
                                )
                            }
                        }
                    }
                }
            }

            // Top 3 Podium Showcase
            if (sortedByPoints.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BentoSectionTitle(
                                title = "Classroom Podium",
                                subtitle = "Top 3 highest point earners"
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // 2nd Place (Left)
                            secondPlace?.let {
                                BentoPodiumCard(
                                    entry = it,
                                    rank = 2,
                                    height = 135.dp,
                                    accentColor = Color(0xFF94A3B8),
                                    badgeText = "2nd",
                                    icon = Icons.Default.MilitaryTech,
                                    modifier = Modifier.weight(1f),
                                    onClick = { selectedStudentForDetail = it }
                                )
                            }

                            // 1st Place (Center, Tallest, Gold)
                            firstPlace?.let {
                                BentoPodiumCard(
                                    entry = it,
                                    rank = 1,
                                    height = 160.dp,
                                    accentColor = BentoAmber,
                                    badgeText = "1st",
                                    icon = Icons.Default.EmojiEvents,
                                    modifier = Modifier.weight(1.15f),
                                    onClick = { selectedStudentForDetail = it }
                                )
                            }

                            // 3rd Place (Right)
                            thirdPlace?.let {
                                BentoPodiumCard(
                                    entry = it,
                                    rank = 3,
                                    height = 120.dp,
                                    accentColor = Color(0xFFD97706),
                                    badgeText = "3rd",
                                    icon = Icons.Default.MilitaryTech,
                                    modifier = Modifier.weight(1f),
                                    onClick = { selectedStudentForDetail = it }
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar & Filter Controls
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    BentoSectionTitle(
                        title = "Student Standings",
                        subtitle = "Cumulative points across all quiz attempts"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("leaderboard_search_input"),
                        placeholder = { Text("Search by student name or email...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = BentoViolet)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Range Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LeaderboardFilterRange.values().forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter.label, fontSize = 12.sp) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BentoViolet,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Loading Shimmer or Empty State or List
            if (isFetching && effectiveLeaderboard.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = BentoViolet,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Loading Live Leaderboard from Firestore...",
                                style = MaterialTheme.typography.bodySmall,
                                color = BentoViolet,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                items(3) {
                    LeaderboardShimmerRow()
                }
            } else if (filteredEntries.isEmpty()) {
                item {
                    NoLeaderboardEmptyState(
                        isCard = true,
                        onTakeQuizToClimb = onTakeQuiz,
                        testTag = "leaderboard_screen_empty_state"
                    )
                }
            } else {
                itemsIndexed(
                    items = filteredEntries,
                    key = { _, item -> item.studentId }
                ) { _, entry ->
                    val isMe = currentUser?.id == entry.studentId
                    InteractiveLeaderboardRow(
                        entry = entry,
                        isMe = isMe,
                        onClick = { selectedStudentForDetail = entry }
                    )
                }
            }
        }
    }
    }

    // Student Performance Detail Modal Dialog
    selectedStudentForDetail?.let { student ->
        val studentAttempts = remember(allAttempts, student.studentId) {
            allAttempts.filter { it.studentId == student.studentId }.sortedByDescending { it.completedAt }
        }
        StudentDetailModalDialog(
            student = student,
            attempts = studentAttempts,
            onDismiss = { selectedStudentForDetail = null }
        )
    }
}

/**
 * 3-Tier Podium Card for Top 1, 2, 3 performers
 */
@Composable
fun BentoPodiumCard(
    entry: StudentLeaderboardEntry,
    rank: Int,
    height: androidx.compose.ui.unit.Dp,
    accentColor: Color,
    badgeText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    BentoCard(
        modifier = modifier
            .height(height)
            .clickable { onClick() }
            .testTag("podium_card_rank_$rank"),
        backgroundColor = accentColor.copy(alpha = 0.12f),
        borderColor = accentColor.copy(alpha = 0.45f),
        cornerRadius = 18.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(if (rank == 1) 36.dp else 30.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (rank == 1) Color.Black else Color.White,
                    modifier = Modifier.size(if (rank == 1) 20.dp else 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = entry.studentName.split(" ").firstOrNull() ?: entry.studentName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Surface(
                color = accentColor.copy(alpha = 0.25f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = "${entry.totalScore} pts",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (rank == 1) BentoAmber else BentoPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Text(
                text = "${entry.totalQuizzesTaken} quizzes",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Enhanced interactive row item with total points prominently featured
 */
@Composable
fun InteractiveLeaderboardRow(
    entry: StudentLeaderboardEntry,
    isMe: Boolean,
    onClick: () -> Unit
) {
    BentoCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("leaderboard_row_${entry.studentId}"),
        backgroundColor = if (isMe) BentoViolet.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        borderColor = if (isMe) BentoViolet.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Distinctive Rank Position Badge
            Box(
                modifier = Modifier
                    .size(38.dp)
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
                    fontSize = 13.sp,
                    color = if (entry.rank == 1) Color.Black else if (entry.rank <= 3) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }

            // Student Avatar
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        when (entry.rank) {
                            1 -> BentoAmber.copy(alpha = 0.2f)
                            else -> BentoViolet.copy(alpha = 0.15f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.studentName.take(1).uppercase(),
                    color = if (entry.rank == 1) BentoAmber else BentoViolet,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // Details Column
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.studentName,
                        style = MaterialTheme.typography.titleMedium,
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${entry.totalQuizzesTaken} quizzes taken",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.0f", entry.averagePercentage)}% accuracy",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (entry.averagePercentage >= 80f) BentoSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (entry.averagePercentage >= 80f) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }

            // Prominent Total Points Pill
            Surface(
                color = if (isMe) BentoViolet else BentoPrimary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (isMe) Color.White else BentoAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${entry.totalScore} pts",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isMe) Color.White else BentoPrimary
                    )
                }
            }
        }
    }
}

/**
 * Shimmer Loading Row
 */
@Composable
fun LeaderboardShimmerRow() {
    val shimmerBrush = rememberShimmerBrush()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(shimmerBrush)
    )
}

/**
 * Modal Dialog showing detailed breakdown of a student's performance
 */
@Composable
fun StudentDetailModalDialog(
    student: StudentLeaderboardEntry,
    attempts: List<QuizAttemptEntity> = emptyList(),
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                // Header with Rank and Student Name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    when (student.rank) {
                                        1 -> BentoAmber
                                        2 -> Color(0xFF94A3B8)
                                        3 -> Color(0xFFD97706)
                                        else -> BentoViolet
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = student.studentName.take(1).uppercase(),
                                color = if (student.rank == 1) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = student.studentName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = student.studentEmail.ifBlank { "Classroom Student" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = BentoAmber.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "#${student.rank}",
                            color = BentoAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Stats Grid
                Text(
                    text = "Cumulative Performance",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "Total Points",
                        value = "${student.totalScore} pts",
                        color = BentoPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Quizzes Taken",
                        value = "${student.totalQuizzesTaken}",
                        color = BentoViolet,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "Avg Accuracy",
                        value = "${String.format(Locale.getDefault(), "%.1f", student.averagePercentage)}%",
                        color = BentoSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Percentile",
                        value = "${String.format(Locale.getDefault(), "%.0f", student.percentile)}th",
                        color = BentoAmber,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Breakdown of Completed Quizzes contributing to Total Points
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Completed Quizzes (${attempts.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${student.totalScore} pts accumulated",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BentoAmber
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (attempts.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        attempts.forEach { att ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = att.quizTitle.ifBlank { "Quiz Assessment" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (att.categoryName.isNotBlank()) {
                                                Text(
                                                    text = att.categoryName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = BentoViolet,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "•",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(att.completedAt))
                                            Text(
                                                text = dateStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Surface(
                                        color = BentoAmber.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = BentoAmber,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "+${att.score} pts",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = BentoAmber
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {
                        Text(
                            text = "Student has completed ${student.totalQuizzesTaken} quizzes totaling ${student.totalScore} points recorded in Cloud Firestore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BentoViolet),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
