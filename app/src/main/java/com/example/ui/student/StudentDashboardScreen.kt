package com.example.ui.student

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.example.R
import com.example.data.local.entities.CategoryEntity
import com.example.data.local.entities.QuestionEntity
import com.example.data.local.entities.QuizAttemptEntity
import com.example.data.local.entities.QuizSetEntity
import com.example.ui.QuizViewModel
import com.example.ui.StudentProgressSummary
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    viewModel: QuizViewModel,
    onStartQuiz: (QuizSetEntity) -> Unit,
    onViewHistory: () -> Unit,
    onViewLeaderboard: (() -> Unit)? = null
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val allQuizSets by viewModel.allQuizSets.collectAsState()
    val allAttempts by viewModel.allAttempts.collectAsState()
    val isCloudConnected by viewModel.isCloudConnected.collectAsState()
    val cloudSyncMessage by viewModel.cloudSyncMessage.collectAsState()
    val progressSummary by viewModel.studentProgressSummary.collectAsState()

    // Dashboard navigation tabs: 0 = Overview, 1 = Top Students Leaderboard, 2 = Upcoming Quizzes, 3 = Recent Scores
    var selectedDashboardTab by remember { mutableStateOf(0) }
    var selectedCategoryId by remember { mutableStateOf("ALL") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf("ALL") }

    // Student's personal completed attempts sorted newest first
    val studentAttempts = remember(allAttempts, currentUser) {
        val studentId = currentUser?.id ?: ""
        allAttempts.filter { it.studentId == studentId }
            .sortedByDescending { it.completedAt }
    }

    // Set of quiz IDs the student has attempted
    val attemptedQuizIds = remember(studentAttempts) {
        studentAttempts.map { it.quizSetId }.toSet()
    }

    // Dynamically discover all unique tags across all available quiz sets
    val allAvailableTags = remember(allQuizSets) {
        val tagList = linkedSetOf<String>()
        allQuizSets.forEach { quiz ->
            tagList.addAll(quiz.getTagList())
        }
        tagList.toList()
    }

    // Filtered quizzes for study and assessment based on Category, Tag, Search Query, and Difficulty
    val filteredUpcomingQuizzes = remember(
        allQuizSets,
        attemptedQuizIds,
        selectedCategoryId,
        selectedTag,
        searchQuery,
        selectedDifficulty
    ) {
        var list = allQuizSets

        // Category filter (e.g. Mathematics, World History, Science, Computer Science)
        if (selectedCategoryId != "ALL") {
            list = list.filter { it.categoryId == selectedCategoryId }
        }

        // Tag filter (e.g. #Math, #History, #Science, #Calculus, #Physics, #Algorithms)
        if (!selectedTag.isNullOrBlank()) {
            list = list.filter { quiz ->
                quiz.getTagList().any { it.equals(selectedTag, ignoreCase = true) }
            }
        }

        // Difficulty filter (Easy, Medium, Hard)
        if (selectedDifficulty != "ALL") {
            list = list.filter { quiz ->
                quiz.difficulty.equals(selectedDifficulty, ignoreCase = true)
            }
        }

        // Realtime text search across title, description, category, and tags
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.trim().lowercase()
            list = list.filter { quiz ->
                quiz.title.lowercase().contains(query) ||
                quiz.description.lowercase().contains(query) ||
                quiz.categoryName.lowercase().contains(query) ||
                quiz.getTagList().any { it.lowercase().contains(query) }
            }
        }

        // Prioritize unattempted quizzes first, followed by others available for retake
        list.sortedBy { it.id in attemptedQuizIds }
    }

    // Unfiltered upcoming quizzes for Overview preview
    val upcomingQuizzes = remember(allQuizSets, attemptedQuizIds) {
        allQuizSets.sortedBy { it.id in attemptedQuizIds }
    }

    val unattemptedUpcomingQuizzes = remember(filteredUpcomingQuizzes, attemptedQuizIds) {
        filteredUpcomingQuizzes.filter { it.id !in attemptedQuizIds }
    }

    Scaffold(
        modifier = Modifier.testTag("student_dashboard_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserProfileAvatar(
                            name = currentUser?.name ?: "Student",
                            photoUrl = currentUser?.photoUrl?.ifBlank { null },
                            size = 40.dp,
                            backgroundColor = BentoViolet
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Hello, ${currentUser?.name ?: "Student"}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isCloudConnected) BentoEmerald else BentoAmber)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isCloudConnected) "Cloud Firestore Synced" else "Local Cache",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCloudConnected) BentoEmerald else BentoAmber,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onViewHistory,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "Full Quiz History",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
        ) {
            // Dashboard Navigation Tabs (Overview | Top Students | Upcoming Quizzes | Recent Scores)
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TabRow(
                        selectedTabIndex = selectedDashboardTab,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = {}
                    ) {
                        Tab(
                            selected = selectedDashboardTab == 0,
                            onClick = { selectedDashboardTab = 0 },
                            modifier = Modifier
                                .padding(2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedDashboardTab == 0) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .testTag("overview_tab"),
                            text = {
                                Text(
                                    "Overview",
                                    fontWeight = if (selectedDashboardTab == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedDashboardTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        )

                        Tab(
                            selected = selectedDashboardTab == 1,
                            onClick = { selectedDashboardTab = 1 },
                            modifier = Modifier
                                .padding(2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedDashboardTab == 1) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .testTag("top_students_tab"),
                            text = {
                                Text(
                                    "Top Students",
                                    fontWeight = if (selectedDashboardTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedDashboardTab == 1) BentoAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        )

                        Tab(
                            selected = selectedDashboardTab == 2,
                            onClick = { selectedDashboardTab = 2 },
                            modifier = Modifier
                                .padding(2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedDashboardTab == 2) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .testTag("upcoming_quizzes_tab"),
                            text = {
                                Text(
                                    "Upcoming (${unattemptedUpcomingQuizzes.size})",
                                    fontWeight = if (selectedDashboardTab == 2) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedDashboardTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        )

                        Tab(
                            selected = selectedDashboardTab == 3,
                            onClick = { selectedDashboardTab = 3 },
                            modifier = Modifier
                                .padding(2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedDashboardTab == 3) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .testTag("recent_scores_tab"),
                            text = {
                                Text(
                                    "Scores (${studentAttempts.size})",
                                    fontWeight = if (selectedDashboardTab == 3) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedDashboardTab == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // TAB 0: OVERVIEW (Progress Summary + Top Students + Previews)
            // -------------------------------------------------------------
            if (selectedDashboardTab == 0) {
                // 1. Personal Progress Summary Card (Hero Bento Card)
                item {
                    PersonalProgressSummaryCard(
                        summary = progressSummary,
                        isCloudConnected = isCloudConnected,
                        cloudSyncMessage = cloudSyncMessage
                    )
                }

                // 2. Curriculum Subjects & Categories
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoSectionTitle(
                            title = "Curriculum Subjects",
                            subtitle = "Explore study quizzes and practice by discipline",
                            actionText = "All Material",
                            onActionClick = {
                                selectedCategoryId = "ALL"
                                selectedTag = null
                                selectedDashboardTab = 2
                            }
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(end = 8.dp)
                        ) {
                            items(allCategories, key = { it.id }) { cat ->
                                val quizzesInCat = allQuizSets.filter { it.categoryId == cat.id }
                                val icon = when {
                                    cat.name.contains("Math", ignoreCase = true) -> Icons.Default.Calculate
                                    cat.name.contains("Science", ignoreCase = true) || cat.name.contains("Physics", ignoreCase = true) -> Icons.Default.Science
                                    cat.name.contains("History", ignoreCase = true) -> Icons.Default.AutoStories
                                    else -> Icons.Default.Computer
                                }
                                val tintColor = when {
                                    cat.name.contains("Math", ignoreCase = true) -> BentoEmerald
                                    cat.name.contains("Science", ignoreCase = true) -> BentoCyan
                                    cat.name.contains("History", ignoreCase = true) -> BentoViolet
                                    else -> BentoPrimary
                                }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, tintColor.copy(alpha = 0.25f)),
                                    modifier = Modifier
                                        .width(170.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            selectedCategoryId = cat.id
                                            selectedTag = null
                                            selectedDashboardTab = 2
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(tintColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = cat.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${quizzesInCat.size} quizzes",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Top Students Leaderboard Component (Compact Bento Preview)
                item {
                    TopStudentsLeaderboardComponent(
                        viewModel = viewModel,
                        isCompactPreview = true,
                        onViewFullLeaderboard = { selectedDashboardTab = 1 },
                        onTakeQuizToClimb = { selectedDashboardTab = 2 }
                    )
                }

                // 4. Upcoming Quizzes Section (Preview on Overview)
                item {
                    BentoSectionTitle(
                        title = "Upcoming Quizzes (${upcomingQuizzes.size})",
                        subtitle = "Assigned tests and available assessments",
                        actionText = "See All",
                        onActionClick = { selectedDashboardTab = 2 }
                    )
                }

                if (upcomingQuizzes.isEmpty()) {
                    item {
                        EmptyUpcomingQuizzesCard()
                    }
                } else {
                    // Show up to 2 upcoming quizzes on overview
                    val previewQuizzes = upcomingQuizzes.take(2)
                    items(previewQuizzes, key = { it.id }) { quizSet ->
                        val isAttempted = quizSet.id in attemptedQuizIds
                        StudentUpcomingQuizCard(
                            quizSet = quizSet,
                            isAttempted = isAttempted,
                            onStart = { onStartQuiz(quizSet) },
                            onTagClick = { tag ->
                                selectedTag = tag
                                selectedDashboardTab = 2
                            }
                        )
                    }
                }

                // 4. Recent Scores Section (Preview on Overview)
                item {
                    BentoSectionTitle(
                        title = "Recent Scores & Feedback",
                        subtitle = "Your latest quiz attempts from Cloud Firestore",
                        actionText = if (studentAttempts.isNotEmpty()) "See All History" else null,
                        onActionClick = onViewHistory
                    )
                }

                if (studentAttempts.isEmpty()) {
                    item {
                        EmptyRecentScoresCard(onTakeFirstQuiz = { selectedDashboardTab = 2 })
                    }
                } else {
                    // Show up to 2 recent score cards on overview
                    val recentPreview = studentAttempts.take(2)
                    items(recentPreview, key = { it.id }) { attempt ->
                        RecentScoreCard(
                            attempt = attempt,
                            viewModel = viewModel,
                            onRetakeQuiz = {
                                val targetQuiz = allQuizSets.find { it.id == attempt.quizSetId }
                                if (targetQuiz != null) onStartQuiz(targetQuiz)
                            }
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // TAB 1: TOP STUDENTS LEADERBOARD (Full Firestore Live Standings)
            // -------------------------------------------------------------
            if (selectedDashboardTab == 1) {
                item {
                    TopStudentsLeaderboardComponent(
                        viewModel = viewModel,
                        isCompactPreview = false,
                        onTakeQuizToClimb = { selectedDashboardTab = 2 }
                    )
                }
            }

            // -------------------------------------------------------------
            // TAB 2: UPCOMING QUIZZES (Study Material, Subject & Tag Filter)
            // -------------------------------------------------------------
            if (selectedDashboardTab == 2) {
                // Realtime Search Bar for study material
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoSectionTitle(
                            title = "Study Material & Assessments",
                            subtitle = "Explore quizzes organized by subject and curriculum tags"
                        )
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search topics, tags, or concepts...", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = BentoPrimary)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quiz_search_input")
                        )
                    }
                }

                // Category Filter Pills (Subject Domains)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Subject Domain",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (selectedCategoryId != "ALL") {
                                TextButton(
                                    onClick = { selectedCategoryId = "ALL" },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Reset", fontSize = 12.sp, color = BentoPrimary)
                                }
                            }
                        }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(end = 8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCategoryId == "ALL",
                                    onClick = { selectedCategoryId = "ALL" },
                                    label = { Text("All Subjects (${allQuizSets.size})", fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = {
                                        Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            items(allCategories, key = { it.id }) { cat ->
                                val count = allQuizSets.count { it.categoryId == cat.id }
                                val icon = when {
                                    cat.name.contains("Math", ignoreCase = true) -> Icons.Default.Calculate
                                    cat.name.contains("Science", ignoreCase = true) || cat.name.contains("Physics", ignoreCase = true) -> Icons.Default.Science
                                    cat.name.contains("History", ignoreCase = true) -> Icons.Default.AutoStories
                                    else -> Icons.Default.Computer
                                }
                                FilterChip(
                                    selected = selectedCategoryId == cat.id,
                                    onClick = {
                                        selectedCategoryId = if (selectedCategoryId == cat.id) "ALL" else cat.id
                                    },
                                    label = { Text("${cat.name} ($count)", fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = {
                                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }

                // Tag Filter Carousel (Curriculum Tags)
                if (allAvailableTags.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocalOffer, contentDescription = null, tint = BentoViolet, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Filter by Tag",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (selectedTag != null) {
                                    TextButton(
                                        onClick = { selectedTag = null },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Clear Tag", fontSize = 12.sp, color = BentoViolet)
                                    }
                                }
                            }
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(end = 8.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedTag == null,
                                        onClick = { selectedTag = null },
                                        label = { Text("All Tags", fontSize = 12.sp) },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                                items(allAvailableTags) { tag ->
                                    val isSelected = selectedTag.equals(tag, ignoreCase = true)
                                    val tagCount = allQuizSets.count { q ->
                                        q.getTagList().any { it.equals(tag, ignoreCase = true) }
                                    }
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedTag = if (isSelected) null else tag
                                        },
                                        label = { Text("#$tag ($tagCount)", fontSize = 12.sp) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BentoViolet.copy(alpha = 0.2f),
                                            selectedLabelColor = BentoViolet
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Active Filters Summary bar (if any filter is active)
                val hasActiveFilters = selectedCategoryId != "ALL" || selectedTag != null || searchQuery.isNotBlank() || selectedDifficulty != "ALL"
                if (hasActiveFilters) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.FilterList, contentDescription = null, tint = BentoPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = buildString {
                                            append("Filtered: ")
                                            val parts = mutableListOf<String>()
                                            if (selectedCategoryId != "ALL") {
                                                val catName = allCategories.find { it.id == selectedCategoryId }?.name ?: "Subject"
                                                parts.add(catName)
                                            }
                                            if (selectedTag != null) parts.add("#$selectedTag")
                                            if (searchQuery.isNotBlank()) parts.add("\"$searchQuery\"")
                                            if (selectedDifficulty != "ALL") parts.add(selectedDifficulty)
                                            append(parts.joinToString(" • "))
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        selectedCategoryId = "ALL"
                                        selectedTag = null
                                        searchQuery = ""
                                        selectedDifficulty = "ALL"
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BentoRose)
                                }
                            }
                        }
                    }
                }

                // Results count and header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Available Assessments (${filteredUpcomingQuizzes.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (unattemptedUpcomingQuizzes.isNotEmpty()) {
                            BentoPillTag(
                                text = "${unattemptedUpcomingQuizzes.size} New Pending",
                                containerColor = BentoAmber.copy(alpha = 0.15f),
                                contentColor = BentoAmber,
                                icon = Icons.Default.Bolt
                            )
                        }
                    }
                }

                // Quizzes List or Empty State
                if (filteredUpcomingQuizzes.isEmpty()) {
                    item {
                        BentoCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.FilterAltOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = BentoViolet.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "No Quizzes Match Selected Filters",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Try clearing the tag, selecting another subject, or resetting filters.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        selectedCategoryId = "ALL"
                                        selectedTag = null
                                        searchQuery = ""
                                        selectedDifficulty = "ALL"
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Reset All Filters")
                                }
                            }
                        }
                    }
                } else {
                    items(filteredUpcomingQuizzes, key = { it.id }) { quizSet ->
                        val isAttempted = quizSet.id in attemptedQuizIds
                        StudentUpcomingQuizCard(
                            quizSet = quizSet,
                            isAttempted = isAttempted,
                            onStart = { onStartQuiz(quizSet) },
                            onTagClick = { clickedTag ->
                                selectedTag = clickedTag
                            }
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // TAB 3: RECENT SCORES (Detailed attempts history & explanations)
            // -------------------------------------------------------------
            if (selectedDashboardTab == 3) {
                // Aggregate score header summary
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = BentoPrimary.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BentoPrimary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${studentAttempts.size}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoPrimary
                                )
                                Text("Quizzes Taken", style = MaterialTheme.typography.labelSmall)
                            }
                            HorizontalDivider(modifier = Modifier.height(32.dp).width(1.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${String.format("%.0f", progressSummary.averageAccuracy)}%",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoEmerald
                                )
                                Text("Avg Accuracy", style = MaterialTheme.typography.labelSmall)
                            }
                            HorizontalDivider(modifier = Modifier.height(32.dp).width(1.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${progressSummary.passedQuizzesCount}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoViolet
                                )
                                Text("Passed Tests", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                item {
                    BentoSectionTitle(
                        title = "Completed Quiz History (${studentAttempts.size})",
                        subtitle = "Review scores, accuracy rates, and question breakdown",
                        actionText = if (studentAttempts.isNotEmpty()) "Open Past History" else null,
                        onActionClick = onViewHistory
                    )
                }

                if (studentAttempts.isEmpty()) {
                    item {
                        EmptyRecentScoresCard(onTakeFirstQuiz = { selectedDashboardTab = 2 })
                    }
                } else {
                    items(studentAttempts, key = { it.id }) { attempt ->
                        RecentScoreCard(
                            attempt = attempt,
                            viewModel = viewModel,
                            onRetakeQuiz = {
                                val targetQuiz = allQuizSets.find { it.id == attempt.quizSetId }
                                if (targetQuiz != null) onStartQuiz(targetQuiz)
                            }
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// PERSONAL PROGRESS SUMMARY CARD (Hero Bento Card)
// -------------------------------------------------------------
@Composable
fun PersonalProgressSummaryCard(
    summary: StudentProgressSummary,
    isCloudConnected: Boolean,
    cloudSyncMessage: String
) {
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = BentoPrimary.copy(alpha = 0.25f),
        cornerRadius = 24.dp
    ) {
        // Top Row: Tier badge + Cloud sync indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BentoPillTag(
                text = summary.tierTitle,
                containerColor = BentoAmber.copy(alpha = 0.18f),
                contentColor = BentoAmber,
                icon = Icons.Default.EmojiEvents
            )

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isCloudConnected) BentoEmerald.copy(alpha = 0.12f) else BentoAmber.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = if (isCloudConnected) BentoEmerald else BentoAmber,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCloudConnected) "Firestore Live" else "Offline Cache",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCloudConnected) BentoEmerald else BentoAmber,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Center Hero Row: Circular Accuracy Gauge + Main Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Personal Progress Summary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (summary.totalQuizzesCompleted > 0)
                        "${summary.totalQuizzesCompleted} assessments completed • ${summary.totalStudyTimeMinutes} mins study time"
                    else
                        "No quizzes taken yet. Ready to start your learning journey?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Circular Visual Progress Gauge
            Box(
                modifier = Modifier.size(76.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 7.dp,
                    trackColor = Color.Transparent
                )
                CircularProgressIndicator(
                    progress = { (summary.averageAccuracy / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxSize(),
                    color = when {
                        summary.averageAccuracy >= 80f -> BentoEmerald
                        summary.averageAccuracy >= 60f -> BentoAmber
                        else -> BentoRose
                    },
                    strokeWidth = 7.dp,
                    trackColor = Color.Transparent
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${String.format("%.0f", summary.averageAccuracy)}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Accuracy",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4-Tile Bento Grid Matrix inside summary card
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BentoStatTile(
                label = "Total Points",
                value = "${summary.totalPointsEarned} pts",
                icon = Icons.Default.Star,
                accentColor = BentoAmber,
                modifier = Modifier.weight(1f)
            )

            BentoStatTile(
                label = "Quizzes Done",
                value = "${summary.totalQuizzesCompleted}",
                icon = Icons.Default.CheckCircle,
                accentColor = BentoEmerald,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BentoStatTile(
                label = "Class Rank",
                value = if (summary.totalQuizzesCompleted > 0) "#${summary.rank}" else "--",
                icon = Icons.Default.Leaderboard,
                accentColor = BentoViolet,
                subtitle = "${String.format("%.0f", summary.percentile)}th percentile",
                modifier = Modifier.weight(1f)
            )

            BentoStatTile(
                label = "Pass Rate",
                value = "${String.format("%.0f", summary.passRatePercentage)}%",
                icon = Icons.Default.TrendingUp,
                accentColor = BentoCyan,
                subtitle = "${summary.passedQuizzesCount} passed",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress Milestone Bar
        val targetMilestone = when {
            summary.totalQuizzesCompleted < 3 -> 3
            summary.totalQuizzesCompleted < 5 -> 5
            summary.totalQuizzesCompleted < 10 -> 10
            else -> summary.totalQuizzesCompleted + 5
        }
        val milestoneProgress = (summary.totalQuizzesCompleted.toFloat() / targetMilestone.toFloat()).coerceIn(0f, 1f)

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Next Tier Progress: ${summary.totalQuizzesCompleted}/$targetMilestone tests",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${String.format("%.0f", milestoneProgress * 100)}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = BentoPrimary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { milestoneProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = BentoPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

// -------------------------------------------------------------
// UPCOMING QUIZ CARD
// -------------------------------------------------------------
@Composable
fun StudentUpcomingQuizCard(
    quizSet: QuizSetEntity,
    isAttempted: Boolean,
    onStart: () -> Unit,
    onTagClick: ((String) -> Unit)? = null
) {
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        // Category and Status Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BentoPillTag(
                text = quizSet.categoryName,
                containerColor = BentoViolet.copy(alpha = 0.15f),
                contentColor = BentoViolet,
                icon = Icons.Default.Category
            )

            if (!isAttempted) {
                BentoPillTag(
                    text = "New Assignment",
                    containerColor = BentoAmber.copy(alpha = 0.18f),
                    contentColor = BentoAmber,
                    icon = Icons.Default.Bolt
                )
            } else {
                BentoPillTag(
                    text = "Retake Available",
                    containerColor = BentoEmerald.copy(alpha = 0.15f),
                    contentColor = BentoEmerald,
                    icon = Icons.Default.Check
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = quizSet.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = quizSet.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Tag Pills (Subject & Topic categorization)
        if (quizSet.getTagList().isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                quizSet.getTagList().forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BentoViolet.copy(alpha = 0.09f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = onTagClick != null) { onTagClick?.invoke(tag) }
                    ) {
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.labelSmall,
                            color = BentoViolet,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quiz Metadata & Launch Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = BentoCyan
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${quizSet.durationMinutes} mins",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = when (quizSet.difficulty) {
                            "Easy" -> BentoEmerald
                            "Hard" -> BentoRose
                            else -> BentoAmber
                        }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        quizSet.difficulty,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Button(
                onClick = onStart,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isAttempted) BentoPrimary else BentoEmerald
                ),
                modifier = Modifier.testTag("start_quiz_btn_${quizSet.id}")
            ) {
                Icon(
                    imageVector = if (!isAttempted) Icons.Default.PlayArrow else Icons.Default.Replay,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (!isAttempted) "Start Quiz" else "Retake Quiz",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// -------------------------------------------------------------
// RECENT SCORE CARD (With Question Review Accordion)
// -------------------------------------------------------------
@Composable
fun RecentScoreCard(
    attempt: QuizAttemptEntity,
    viewModel: QuizViewModel,
    onRetakeQuiz: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var questionsList by remember { mutableStateOf<List<QuestionEntity>>(emptyList()) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault()) }
    val formattedDate = remember(attempt.completedAt) {
        dateFormat.format(Date(attempt.completedAt))
    }

    val isPassed = attempt.percentage >= 60f
    val mins = attempt.timeSpentSeconds / 60
    val secs = attempt.timeSpentSeconds % 60
    val durationText = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"

    LaunchedEffect(attempt.quizSetId, isExpanded) {
        if (isExpanded && questionsList.isEmpty()) {
            questionsList = viewModel.repository.getQuestionsForQuizSet(attempt.quizSetId)
        }
    }

    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        borderColor = if (isPassed) BentoEmerald.copy(alpha = 0.3f) else BentoRose.copy(alpha = 0.3f)
    ) {
        Column {
            // Header Row: Quiz title, Date & Pass/Fail status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = attempt.quizTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$formattedDate • $durationText",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Big prominent score pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isPassed) BentoEmerald.copy(alpha = 0.15f) else BentoRose.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPassed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (isPassed) BentoEmerald else BentoRose,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${attempt.score}/${attempt.totalQuestions} (${String.format("%.0f", attempt.percentage)}%)",
                            fontWeight = FontWeight.Bold,
                            color = if (isPassed) BentoEmerald else BentoRose,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Row: Expand Review & Retake Quiz Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { isExpanded = !isExpanded },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isExpanded) "Hide Questions" else "Review Answers",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(
                    onClick = onRetakeQuiz,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Replay,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = BentoPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Retake Quiz",
                        color = BentoPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Expandable Question Breakdown & Explanations
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(4.dp))

                    if (questionsList.isEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Loading quiz questions...", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        questionsList.forEachIndexed { index, q ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "${index + 1}. ${q.questionText}",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Correct Answer: ${q.getOptionText(q.correctOptionIndex)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BentoEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (q.explanation.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "Explanation: ${q.explanation}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// EMPTY STATES
// -------------------------------------------------------------
@Composable
fun EmptyUpcomingQuizzesCard() {
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.TaskAlt,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = BentoEmerald
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "All Caught Up!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "No pending upcoming quizzes in this category. Great job keeping up with your coursework!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyRecentScoresCard(onTakeFirstQuiz: () -> Unit) {
    NoQuizResultsEmptyState(
        onTakeQuiz = onTakeFirstQuiz,
        isCard = true,
        customTitle = "No Recent Quiz Results Yet",
        customDescription = "Complete an upcoming quiz to view your score breakdowns, accuracy rates, and question review explanations right here.",
        testTag = "empty_recent_scores_card"
    )
}
