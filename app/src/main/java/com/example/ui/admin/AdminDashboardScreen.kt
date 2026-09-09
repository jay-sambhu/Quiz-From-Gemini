package com.example.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firestore.SystemLogItem
import com.example.data.local.entities.CategoryEntity
import com.example.data.local.entities.UserEntity
import com.example.data.model.UserRole
import com.example.ui.AdminPlatformMetrics
import com.example.ui.QuizViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(viewModel: QuizViewModel) {
    val platformMetrics by viewModel.platformMetrics.collectAsState()
    val recentLogs by viewModel.recentSystemLogs.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Bento Hub, 1: System Logs, 2: User Roster, 3: Categories
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    var catName by remember { mutableStateOf("") }
    var catDesc by remember { mutableStateOf("") }
    var catColor by remember { mutableStateOf("#4F46E5") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BentoViolet.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = BentoViolet
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Admin Console",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                BentoPillTag(
                                    text = "ADMIN",
                                    containerColor = BentoRose.copy(alpha = 0.2f),
                                    contentColor = BentoRose,
                                    icon = Icons.Default.AdminPanelSettings
                                )
                            }
                            Text(
                                text = "Platform Oversight & Firebase Central Management",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.forceSyncFirestore() },
                        modifier = Modifier.testTag("admin_force_sync_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Force Sync Cloud",
                            tint = BentoPrimary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.triggerAdminDiagnosticLog() },
                        modifier = Modifier.testTag("admin_run_diagnostic_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Run Diagnostics",
                            tint = BentoCyan
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 3) {
                ExtendedFloatingActionButton(
                    onClick = { showAddCategoryDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Category", fontWeight = FontWeight.Bold) },
                    containerColor = BentoPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("admin_add_category_fab")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Cloud Status Header Banner
            FirestoreLiveStatusBanner(
                metrics = platformMetrics,
                onForceSync = { viewModel.forceSyncFirestore() }
            )

            // Primary Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Bento Grid Hub", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("System Logs (${recentLogs.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("User Roster (${allUsers.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Categories (${allCategories.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("API Configuration", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            when (selectedTab) {
                0 -> BentoGridOverviewTab(
                    metrics = platformMetrics,
                    recentLogs = recentLogs,
                    allCategories = allCategories,
                    onNavigateToLogs = { selectedTab = 1 },
                    onNavigateToUsers = { selectedTab = 2 },
                    onNavigateToCategories = { selectedTab = 3 },
                    onNavigateToApiConfig = { selectedTab = 4 },
                    onAddCategory = { showAddCategoryDialog = true },
                    onRunDiagnostic = { viewModel.triggerAdminDiagnosticLog() },
                    onForceSync = { viewModel.forceSyncFirestore() }
                )
                1 -> FullSystemLogsTab(
                    logs = recentLogs,
                    onTriggerDiagnostic = { viewModel.triggerAdminDiagnosticLog() }
                )
                2 -> UserRosterTab(
                    users = allUsers,
                    onRoleChange = { userId, newRole ->
                        viewModel.changeUserRole(userId, newRole)
                    }
                )
                3 -> CategoryManagementTab(
                    categories = allCategories,
                    onDelete = { categoryId ->
                        viewModel.deleteCategory(categoryId)
                    }
                )
                4 -> GeminiAiAdminTab(viewModel = viewModel)
            }
        }
    }

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Create Subject Category", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = catName,
                        onValueChange = { catName = it },
                        label = { Text("Category Name (e.g. Science)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = catDesc,
                        onValueChange = { catDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Category Theme Color:", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("#4F46E5", "#0EA5E9", "#10B981", "#F59E0B", "#F43F5E").forEach { colorStr ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(colorStr)))
                                    .border(
                                        width = if (catColor == colorStr) 3.dp else 0.dp,
                                        color = if (catColor == colorStr) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { catColor = colorStr }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (catName.isNotBlank()) {
                            viewModel.addCategory(
                                name = catName.trim(),
                                description = catDesc.trim(),
                                iconName = "Category",
                                colorHex = catColor
                            )
                            catName = ""
                            catDesc = ""
                            showAddCategoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary)
                ) {
                    Text("Save Category", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Top Live Cloud Status Pill Banner
 */
@Composable
fun FirestoreLiveStatusBanner(
    metrics: AdminPlatformMetrics,
    onForceSync: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (metrics.isFirestoreConnected) BentoEmerald.copy(alpha = pulseAlpha) else BentoRose
                        )
                )
                Text(
                    text = metrics.firestoreSyncMessage,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (metrics.isFirestoreConnected) BentoEmerald else BentoRose
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BentoPrimary.copy(alpha = 0.12f),
                modifier = Modifier.clickable { onForceSync() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = BentoPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${metrics.totalCloudDocumentsCount} Docs Synced",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BentoPrimary
                    )
                }
            }
        }
    }
}

/**
 * Tab 0: Comprehensive Bento Grid Overview
 */
@Composable
fun BentoGridOverviewTab(
    metrics: AdminPlatformMetrics,
    recentLogs: List<SystemLogItem>,
    allCategories: List<CategoryEntity>,
    onNavigateToLogs: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToApiConfig: () -> Unit = {},
    onAddCategory: () -> Unit,
    onRunDiagnostic: () -> Unit,
    onForceSync: () -> Unit
) {
    var selectedLogFilter by remember { mutableStateOf("ALL") }

    val filteredLogs = remember(recentLogs, selectedLogFilter) {
        if (selectedLogFilter == "ALL") {
            recentLogs.take(5)
        } else {
            recentLogs.filter { it.category == selectedLogFilter }.take(5)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
    ) {
        // Bento Card 1 (Hero Large): User Ecosystem & Access Distribution
        item {
            BentoUserEcosystemCard(
                metrics = metrics,
                onClick = onNavigateToUsers
            )
        }

        // Bento Row 2 (Asymmetric 2-Column): Active Quizzes & Platform Performance
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Active Quizzes Card
                BentoActiveQuizzesCard(
                    metrics = metrics,
                    categories = allCategories,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToCategories
                )

                // Platform Performance Card
                BentoPlatformPerformanceCard(
                    metrics = metrics,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Bento Card 3 (Wide): Cloud Datastore Architecture & Collections
        item {
            BentoCloudDatastoreCard(
                metrics = metrics,
                logsCount = recentLogs.size,
                onForceSync = onForceSync
            )
        }

        // Bento Card 4: Recent System Logs & Audit Stream (Aggregated from Firestore)
        item {
            BentoRecentLogsSection(
                logs = filteredLogs,
                totalLogsCount = recentLogs.size,
                selectedFilter = selectedLogFilter,
                onFilterSelect = { selectedLogFilter = it },
                onViewAll = onNavigateToLogs,
                onRunDiagnostic = onRunDiagnostic
            )
        }

        // Bento Card 5: Quick Management Actions
        item {
            BentoQuickActionsCard(
                onAddCategory = onAddCategory,
                onRunDiagnostic = onRunDiagnostic,
                onForceSync = onForceSync,
                onNavigateToApiConfig = onNavigateToApiConfig
            )
        }
    }
}

/**
 * Bento Tile: User Ecosystem & Role Distribution
 */
@Composable
fun BentoUserEcosystemCard(
    metrics: AdminPlatformMetrics,
    onClick: () -> Unit
) {
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = BentoViolet.copy(alpha = 0.28f),
        cornerRadius = 20.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BentoViolet.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = BentoViolet,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "User Population & Access Tiers",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Aggregated across Firestore Multi-Role DB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            BentoPillTag(
                text = "RBAC IAM",
                containerColor = BentoViolet.copy(alpha = 0.12f),
                contentColor = BentoViolet,
                icon = Icons.Default.Security
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "${metrics.totalUsers}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Total Registered Accounts",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BentoRoleBubble(count = metrics.studentCount, label = "Students", color = BentoCyan)
                BentoRoleBubble(count = metrics.teacherCount, label = "Teachers", color = BentoViolet)
                BentoRoleBubble(count = metrics.adminCount, label = "Admins", color = BentoAmber)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Proportional Segmented Progress Bar
        val total = if (metrics.totalUsers > 0) metrics.totalUsers.toFloat() else 1f
        val studentWeight = (metrics.studentCount.toFloat() / total).coerceIn(0.05f, 1f)
        val teacherWeight = (metrics.teacherCount.toFloat() / total).coerceIn(0.05f, 1f)
        val adminWeight = (metrics.adminCount.toFloat() / total).coerceIn(0.05f, 1f)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            Box(modifier = Modifier.weight(studentWeight).fillMaxHeight().background(BentoCyan))
            Spacer(modifier = Modifier.width(2.dp))
            Box(modifier = Modifier.weight(teacherWeight).fillMaxHeight().background(BentoViolet))
            Spacer(modifier = Modifier.width(2.dp))
            Box(modifier = Modifier.weight(adminWeight).fillMaxHeight().background(BentoAmber))
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Protected with Firestore Security Rules",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Tap to manage roster →",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = BentoPrimary
            )
        }
    }
}

@Composable
fun BentoRoleBubble(count: Int, label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Bento Tile: Active Quizzes & Curriculum Content
 */
@Composable
fun BentoActiveQuizzesCard(
    metrics: AdminPlatformMetrics,
    categories: List<CategoryEntity>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    BentoCard(
        modifier = modifier,
        backgroundColor = BentoEmerald.copy(alpha = 0.06f),
        borderColor = BentoEmerald.copy(alpha = 0.22f),
        cornerRadius = 20.dp,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(BentoEmerald.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Quiz,
                contentDescription = null,
                tint = BentoEmerald,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "${metrics.totalQuizSets}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Active Quizzes",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = BentoEmerald
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${metrics.totalQuestionsCount} Questions Pool",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "${metrics.totalCategoriesCount} Subject Categories",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Mini category dots
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            categories.take(4).forEach { cat ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            try {
                                Color(android.graphics.Color.parseColor(cat.colorHex))
                            } catch (e: Exception) {
                                BentoViolet
                            }
                        )
                )
            }
        }
    }
}

/**
 * Bento Tile: Platform Performance & Engagement
 */
@Composable
fun BentoPlatformPerformanceCard(
    metrics: AdminPlatformMetrics,
    modifier: Modifier = Modifier
) {
    BentoCard(
        modifier = modifier,
        backgroundColor = BentoAmber.copy(alpha = 0.06f),
        borderColor = BentoAmber.copy(alpha = 0.22f),
        cornerRadius = 20.dp
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(BentoAmber.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                tint = BentoAmber,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "${"%.1f".format(metrics.passRatePercentage)}%",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Platform Pass Rate",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = BentoAmber
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${metrics.totalAttemptsCount} Completed Tests",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "${metrics.totalTimeSpentMinutes} Total Study Mins",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        BentoPillTag(
            text = "Avg: ${"%.1f".format(metrics.averageScorePercentage)}%",
            containerColor = BentoAmber.copy(alpha = 0.16f),
            contentColor = BentoAmber
        )
    }
}

/**
 * Bento Tile: Cloud Datastore Infrastructure & Collections
 */
@Composable
fun BentoCloudDatastoreCard(
    metrics: AdminPlatformMetrics,
    logsCount: Int,
    onForceSync: () -> Unit
) {
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        cornerRadius = 20.dp
    ) {
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
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = BentoCyan,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Cloud Firestore Datastore",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = onForceSync,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sync Datastore",
                    tint = BentoPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Grid of 6 Firestore collection documents
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CollectionBadge("users", metrics.totalUsers, BentoViolet, Modifier.weight(1f))
            CollectionBadge("quiz_sets", metrics.totalQuizSets, BentoEmerald, Modifier.weight(1f))
            CollectionBadge("questions", metrics.totalQuestionsCount, BentoCyan, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CollectionBadge("categories", metrics.totalCategoriesCount, BentoAmber, Modifier.weight(1f))
            CollectionBadge("attempts", metrics.totalAttemptsCount, BentoRose, Modifier.weight(1f))
            CollectionBadge("system_logs", logsCount, BentoPrimary, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bi-directional Real-Time Cloud Listeners",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "< 80ms Sync Latency",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = BentoEmerald
            )
        }
    }
}

@Composable
fun CollectionBadge(name: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Bento Section: Recent System Logs & Audit Stream
 */
@Composable
fun BentoRecentLogsSection(
    logs: List<SystemLogItem>,
    totalLogsCount: Int,
    selectedFilter: String,
    onFilterSelect: (String) -> Unit,
    onViewAll: () -> Unit,
    onRunDiagnostic: () -> Unit
) {
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Recent System Logs & Audit Stream",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Aggregated from Firestore `system_logs`",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = onViewAll) {
                Text("View All ($totalLogsCount)", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter chips row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val filters = listOf(
                "ALL" to "All Logs",
                "SECURITY" to "Security & IAM",
                "AI_ENGINE" to "Gemini AI",
                "QUIZ_ACTIVITY" to "Curriculum",
                "FIRESTORE_SYNC" to "Cloud Sync"
            )
            items(filters) { (key, label) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { onFilterSelect(key) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No system logs match this filter",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                logs.forEach { logItem ->
                    SystemLogItemTile(log = logItem)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onRunDiagnostic,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Record Admin Diagnostic Test in Firestore", fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Single System Log Tile
 */
@Composable
fun SystemLogItemTile(log: SystemLogItem) {
    val severityColor = when (log.severity) {
        "SUCCESS" -> BentoEmerald
        "WARNING" -> BentoAmber
        "ALERT" -> BentoRose
        else -> BentoCyan
    }

    val categoryIcon = when (log.category) {
        "SECURITY" -> Icons.Default.Security
        "AI_ENGINE" -> Icons.Default.AutoAwesome
        "QUIZ_ACTIVITY" -> Icons.Default.Quiz
        "FIRESTORE_SYNC" -> Icons.Default.CloudSync
        else -> Icons.Default.Info
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, severityColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(severityColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = severityColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatRelativeTimestamp(log.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = log.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BentoPillTag(
                        text = log.severity,
                        containerColor = severityColor.copy(alpha = 0.15f),
                        contentColor = severityColor
                    )
                    Text(
                        text = "Actor: ${log.actor}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/**
 * Bento Tile: Quick Management Actions
 */
@Composable
fun BentoQuickActionsCard(
    onAddCategory: () -> Unit,
    onRunDiagnostic: () -> Unit,
    onForceSync: () -> Unit,
    onNavigateToApiConfig: () -> Unit = {}
) {
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Text(
            text = "Administrative Shortcuts",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAddCategory,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Subject", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onNavigateToApiConfig,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("API Config", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onForceSync,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sync Cloud", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Tab 1: Full System Logs Explorer
 */
@Composable
fun FullSystemLogsTab(
    logs: List<SystemLogItem>,
    onTriggerDiagnostic: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }

    val filteredLogs = remember(logs, searchQuery, selectedCategory) {
        logs.filter { item ->
            val matchCategory = selectedCategory == "ALL" || item.category == selectedCategory
            val matchQuery = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true) ||
                item.actor.contains(searchQuery, ignoreCase = true)
            matchCategory && matchQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search logs by event, actor, or payload...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val categories = listOf(
                "ALL" to "All (${logs.size})",
                "SECURITY" to "Security",
                "AI_ENGINE" to "Gemini AI",
                "QUIZ_ACTIVITY" to "Curriculum",
                "FIRESTORE_SYNC" to "Cloud Sync"
            )
            items(categories) { (key, title) ->
                FilterChip(
                    selected = selectedCategory == key,
                    onClick = { selectedCategory = key },
                    label = { Text(title) },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No system logs match criteria",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    SystemLogItemTile(log = log)
                }
            }
        }
    }
}

/**
 * Tab 2: User Roster & Multi-Role Access Control
 */
@Composable
fun UserRosterTab(
    users: List<UserEntity>,
    onRoleChange: (String, UserRole) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf<UserRole?>(null) }

    val filteredUsers = remember(users, searchQuery, selectedRoleFilter) {
        users.filter { user ->
            val matchRole = selectedRoleFilter == null || user.role == selectedRoleFilter
            val matchSearch = searchQuery.isBlank() ||
                user.name.contains(searchQuery, ignoreCase = true) ||
                user.email.contains(searchQuery, ignoreCase = true)
            matchRole && matchSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Filter users by name or email...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = selectedRoleFilter == null,
                onClick = { selectedRoleFilter = null },
                label = { Text("All (${users.size})") }
            )
            UserRole.entries.forEach { role ->
                val count = users.count { it.role == role }
                FilterChip(
                    selected = selectedRoleFilter == role,
                    onClick = { selectedRoleFilter = role },
                    label = { Text("${role.name} ($count)") }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(filteredUsers, key = { it.id }) { user ->
                UserRoleAdminCard(
                    user = user,
                    onRoleChange = { newRole -> onRoleChange(user.id, newRole) }
                )
            }
        }
    }
}

/**
 * Tab 3: Curriculum Category Management
 */
@Composable
fun CategoryManagementTab(
    categories: List<CategoryEntity>,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        items(categories, key = { it.id }) { category ->
            CategoryAdminCard(
                category = category,
                onDelete = { onDelete(category.id) }
            )
        }
    }
}

@Composable
fun CategoryAdminCard(category: CategoryEntity, onDelete: () -> Unit) {
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        try {
                            Color(android.graphics.Color.parseColor(category.colorHex))
                        } catch (e: Exception) {
                            BentoViolet
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Category, contentDescription = null, tint = Color.White)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = category.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = BentoRose)
            }
        }
    }
}

@Composable
fun UserRoleAdminCard(user: UserEntity, onRoleChange: (UserRole) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            UserProfileAvatar(
                name = user.name,
                photoUrl = user.photoUrl.ifBlank { null },
                size = 40.dp,
                backgroundColor = when (user.role) {
                    UserRole.ADMIN -> BentoRose
                    UserRole.TEACHER -> BentoViolet
                    UserRole.STUDENT -> BentoEmerald
                }
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Box {
                AssistChip(
                    onClick = { expanded = true },
                    label = { Text(user.role.name, fontWeight = FontWeight.Bold) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    shape = RoundedCornerShape(10.dp)
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    UserRole.entries.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role.name) },
                            onClick = {
                                onRoleChange(role)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

fun formatRelativeTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}
