package com.example.ui.teacher

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.data.local.entities.QuizSetEntity
import com.example.ui.AiGenerationProgressState
import com.example.ui.QuizViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(viewModel: QuizViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val allQuizSets by viewModel.allQuizSets.collectAsState()
    val isGeneratingAi by viewModel.isGeneratingAiQuiz.collectAsState()
    val aiGenProgress by viewModel.aiGenerationProgress.collectAsState()
    val notificationLogs by viewModel.notificationLogs.collectAsState()
    val analyticsOverview by viewModel.teacherAnalyticsOverview.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var hasStartedAiGen by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: My Quizzes, 1: Performance Analytics (Recharts Bar Chart), 2: Email Alerts
    var selectedQuizForQuestions by remember { mutableStateOf<QuizSetEntity?>(null) }
    var showImportCsvDialog by remember { mutableStateOf(false) }
    var targetQuizForImport by remember { mutableStateOf<QuizSetEntity?>(null) }

    LaunchedEffect(isGeneratingAi) {
        if (isGeneratingAi) {
            hasStartedAiGen = true
        } else if (hasStartedAiGen) {
            showCreateDialog = false
            hasStartedAiGen = false
        }
    }

    val teacherQuizzes = remember(allQuizSets, currentUser) {
        allQuizSets.filter { it.creatorTeacherId == currentUser?.id || currentUser?.role == com.example.data.model.UserRole.ADMIN }
    }

    if (selectedQuizForQuestions != null) {
        TeacherQuestionEditorScreen(
            quizSet = selectedQuizForQuestions!!,
            viewModel = viewModel,
            onBack = { selectedQuizForQuestions = null }
        )
        return
    }

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
                            Icon(Icons.Default.School, contentDescription = null, tint = BentoViolet)
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Teacher's Dashboard", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                BentoPillTag(
                                    text = "TEACHER",
                                    containerColor = BentoViolet.copy(alpha = 0.2f),
                                    contentColor = BentoViolet,
                                    icon = Icons.Default.School
                                )
                            }
                            Text(
                                text = "Curriculum, Assessments & Classroom Analytics",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = {
                            targetQuizForImport = null
                            showImportCsvDialog = true
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = BentoEmerald.copy(alpha = 0.15f),
                            contentColor = BentoEmerald
                        ),
                        modifier = Modifier.testTag("teacher_import_quiz_topbar_btn")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import Quiz", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                text = { Text("Generate Quiz Set", fontWeight = FontWeight.Bold) },
                containerColor = BentoPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("teacher_create_quiz_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Bento Hero Banner
            BentoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                cornerRadius = 22.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_teacher_banner_1785302519709),
                        contentDescription = "Teacher Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column {
                            BentoPillTag(
                                text = "Faculty Studio",
                                containerColor = BentoViolet,
                                contentColor = Color.White,
                                icon = Icons.Default.AutoAwesome
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Welcome, ${currentUser?.name ?: "Professor"}",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Create tests or leverage Gemini AI to generate question sets instantly.",
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bento Metrics Overview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BentoStatTile(
                    label = "Quizzes",
                    value = "${teacherQuizzes.size}",
                    icon = Icons.Default.Quiz,
                    accentColor = BentoViolet,
                    modifier = Modifier.weight(1f)
                )
                BentoStatTile(
                    label = "Class Avg",
                    value = "${String.format("%.1f", analyticsOverview.classAverageScore)}%",
                    icon = Icons.Default.TrendingUp,
                    accentColor = BentoPrimary,
                    modifier = Modifier.weight(1f)
                )
                BentoStatTile(
                    label = "Emails",
                    value = "${notificationLogs.size}",
                    icon = Icons.Default.MarkEmailRead,
                    accentColor = BentoEmerald,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = BentoPrimary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Quizzes (${teacherQuizzes.size})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Quiz, contentDescription = null, tint = if (selectedTab == 0) BentoPrimary else MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.testTag("tab_teacher_quizzes")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Performance", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null, tint = if (selectedTab == 1) BentoPrimary else MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.testTag("tab_teacher_performance")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Email Alerts", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Email, contentDescription = null, tint = if (selectedTab == 2) BentoPrimary else MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.testTag("tab_teacher_alerts")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> {
                        // If Gemini AI is actively generating in foreground or background, display subtle shimmer progress card
                        if (isGeneratingAi) {
                    BentoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        backgroundColor = BentoViolet.copy(alpha = 0.06f),
                        borderColor = BentoViolet.copy(alpha = 0.4f),
                        cornerRadius = 18.dp
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
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BentoViolet.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BentoViolet, modifier = Modifier.size(18.dp))
                                }
                                Column {
                                    Text("Generating Quiz with Gemini AI...", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${aiGenProgress?.topic ?: "Custom Topic"} • ${aiGenProgress?.difficulty ?: "Medium"} (${aiGenProgress?.questionCount ?: 5} Qs)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BentoViolet
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BentoViolet.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${((aiGenProgress?.progressPercentage ?: 0.25f) * 100).toInt()}%",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoViolet
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { (aiGenProgress?.progressPercentage ?: 0.25f).coerceIn(0.05f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = BentoViolet,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = BentoCyan)
                            Text(
                                text = aiGenProgress?.statusMessage ?: "Synthesizing questions & options...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        // Shimmer placeholder representing incoming quiz card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ShimmerBox(modifier = Modifier.size(44.dp), cornerRadius = 12.dp)
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                ShimmerBox(modifier = Modifier.fillMaxWidth(0.65f).height(16.dp))
                                ShimmerBox(modifier = Modifier.fillMaxWidth(0.40f).height(12.dp))
                            }
                        }
                    }
                }

                if (teacherQuizzes.isEmpty() && !isGeneratingAi) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Quiz,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = BentoViolet.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No quizzes created yet.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Tap 'Generate Quiz Set' to create a test with Gemini AI.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { showCreateDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                                    modifier = Modifier.testTag("teacher_empty_state_create_fab")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Generate AI Quiz")
                                }
                                OutlinedButton(
                                    onClick = {
                                        targetQuizForImport = null
                                        showImportCsvDialog = true
                                    },
                                    modifier = Modifier.testTag("teacher_empty_state_import_csv")
                                ) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Import CSV")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        items(teacherQuizzes, key = { it.id }) { quizSet ->
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
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(BentoViolet.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Assignment,
                                            contentDescription = null,
                                            tint = BentoViolet,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = quizSet.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            BentoPillTag(
                                                text = quizSet.categoryName,
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            BentoPillTag(
                                                text = "${quizSet.durationMinutes}m",
                                                containerColor = BentoCyan.copy(alpha = 0.15f),
                                                contentColor = BentoCyan
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = quizSet.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                        if (quizSet.getTagList().isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                quizSet.getTagList().take(4).forEach { tag ->
                                                    Text(
                                                        text = "#$tag",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = BentoViolet,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteQuizSet(quizSet.id) },
                                        modifier = Modifier.testTag("delete_quiz_btn_${quizSet.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = BentoRose)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = { selectedQuizForQuestions = quizSet },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = BentoPrimary.copy(alpha = 0.15f),
                                            contentColor = BentoPrimary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("manage_questions_btn_${quizSet.id}")
                                    ) {
                                        Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Questions", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            targetQuizForImport = quizSet
                                            showImportCsvDialog = true
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = BentoEmerald.copy(alpha = 0.15f),
                                            contentColor = BentoEmerald
                                        ),
                                        modifier = Modifier
                                            .weight(0.9f)
                                            .testTag("import_csv_btn_${quizSet.id}")
                                    ) {
                                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Import CSV", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }

                                    FilledTonalButton(
                                        onClick = { selectedQuizForQuestions = quizSet },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = BentoViolet.copy(alpha = 0.15f),
                                            contentColor = BentoViolet
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("ai_suggest_question_btn_${quizSet.id}")
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("✨ AI", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // Student Performance Analytics View (Recharts Library Bar Chart + Native Compose)
                TeacherPerformanceDashboardTab(
                    viewModel = viewModel,
                    onSendAlertToStudent = {
                        selectedTab = 2
                    }
                )
            }
                else -> {
                    // Email Notifications Log
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        items(notificationLogs, key = { it.id }) { log ->
                            BentoCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                cornerRadius = 16.dp
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = BentoEmerald, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = log.subject, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "Recipient: ${log.recipientName} (${log.recipientEmail})", style = MaterialTheme.typography.labelSmall, color = BentoPrimary, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = log.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

    if (showCreateDialog) {
        CreateQuizModalDialog(
            categories = allCategories,
            isGeneratingAi = isGeneratingAi,
            aiGenProgress = aiGenProgress,
            isAiSuggestingQuestion = viewModel.isAiSuggestingQuestion.collectAsState().value,
            onRequestAiQuestionSuggestion = { topic, diff, onResult ->
                viewModel.requestAiQuestionSuggestion(
                    topic = topic,
                    difficulty = diff,
                    onResult = onResult
                )
            },
            onRequestAiDistractorsSuggestion = { qText, topic, diff, onResult ->
                viewModel.requestAiDistractorsSuggestion(
                    questionText = qText,
                    topic = topic,
                    difficulty = diff,
                    onResult = onResult
                )
            },
            onDismiss = { showCreateDialog = false },
            onGenerateAi = { topic, title, desc, catId, catName, count, duration, difficulty, tags ->
                viewModel.generateGeminiAiQuiz(
                    topic = topic,
                    title = title,
                    description = desc,
                    categoryId = catId,
                    categoryName = catName,
                    questionCount = count,
                    durationMinutes = duration,
                    difficulty = difficulty,
                    tags = tags
                )
                // Dialog stays open displaying AiQuestionGenerationProgress shimmer animation
                // and closes automatically once finished or when dismissed to background
            },
            onCreateManual = { title, desc, catId, catName, duration, passMark, diff, questions, tags ->
                viewModel.createManualQuiz(
                    title = title,
                    description = desc,
                    categoryId = catId,
                    categoryName = catName,
                    durationMinutes = duration,
                    passPercentage = passMark,
                    difficulty = diff,
                    questions = questions,
                    tags = tags
                )
                showCreateDialog = false
            },
            onOpenImportCsv = {
                showCreateDialog = false
                targetQuizForImport = null
                showImportCsvDialog = true
            }
        )
    }

    if (showImportCsvDialog) {
        ImportQuizCsvModalDialog(
            categories = allCategories,
            targetQuizSet = targetQuizForImport,
            availableQuizzes = teacherQuizzes,
            viewModel = viewModel,
            onDismiss = {
                showImportCsvDialog = false
                targetQuizForImport = null
            },
            onImportSuccess = { quizTitle, count ->
                showImportCsvDialog = false
                targetQuizForImport = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuizModalDialog(
    categories: List<CategoryEntity>,
    isGeneratingAi: Boolean,
    aiGenProgress: AiGenerationProgressState?,
    isAiSuggestingQuestion: Boolean = false,
    onRequestAiQuestionSuggestion: ((topic: String, diff: String, (com.example.data.gemini.GeneratedQuizQuestion) -> Unit) -> Unit)? = null,
    onRequestAiDistractorsSuggestion: ((qText: String, topic: String, diff: String, (com.example.data.gemini.GeneratedQuizQuestion) -> Unit) -> Unit)? = null,
    onDismiss: () -> Unit,
    onGenerateAi: (topic: String, title: String, desc: String, catId: String, catName: String, count: Int, duration: Int, difficulty: String, tags: String) -> Unit,
    onCreateManual: (title: String, desc: String, catId: String, catName: String, duration: Int, passMark: Int, diff: String, questions: List<QuestionEntity>, tags: String) -> Unit,
    onOpenImportCsv: (() -> Unit)? = null
) {
    var modeTab by remember { mutableIntStateOf(0) } // 0: Gemini AI, 1: Manual, 2: Import CSV

    // Form inputs
    var topicInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf(categories.firstOrNull()) }
    var questionCount by remember { mutableIntStateOf(5) }
    var durationMinutes by remember { mutableIntStateOf(10) }
    var difficulty by remember { mutableStateOf("Medium") }

    val suggestedTags = listOf("Math", "History", "Science", "Physics", "Chemistry", "Biology", "Calculus", "Algorithms", "STEM")

    // Manual Form
    var qText by remember { mutableStateOf("") }
    var optA by remember { mutableStateOf("") }
    var optB by remember { mutableStateOf("") }
    var optC by remember { mutableStateOf("") }
    var optD by remember { mutableStateOf("") }
    var correctIdx by remember { mutableIntStateOf(0) }
    var explanation by remember { mutableStateOf("") }
    val manualQuestions = remember { mutableStateListOf<QuestionEntity>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isGeneratingAi) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BentoViolet)
                    Text("Synthesizing AI Quiz Set", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Quiz, contentDescription = null, tint = BentoPrimary)
                    Text("Generate New Question Set", fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            if (isGeneratingAi) {
                AiQuestionGenerationProgress(
                    topic = topicInput.ifBlank { aiGenProgress?.topic ?: "Subject Domain" },
                    difficulty = difficulty,
                    targetCount = questionCount,
                    currentStepIndex = aiGenProgress?.currentStepIndex ?: 0,
                    progressPercentage = aiGenProgress?.progressPercentage ?: 0.15f,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TabRow(selectedTabIndex = modeTab) {
                        Tab(selected = modeTab == 0, onClick = { modeTab = 0 }, text = { Text("Gemini AI Auto") }, modifier = Modifier.testTag("tab_create_ai"))
                        Tab(selected = modeTab == 1, onClick = { modeTab = 1 }, text = { Text("Manual Entry") }, modifier = Modifier.testTag("tab_create_manual"))
                        Tab(selected = modeTab == 2, onClick = { onOpenImportCsv?.invoke() }, text = { Text("Import CSV") }, modifier = Modifier.testTag("tab_create_csv"))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (modeTab == 0) {
                        // Gemini AI Form
                        OutlinedTextField(
                            value = topicInput,
                            onValueChange = { topicInput = it },
                            label = { Text("Subject / Topic (e.g., Quantum Physics, Algorithms)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("Quiz Title (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Select Subject Category:")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = selectedCat?.id == cat.id,
                                    onClick = { selectedCat = cat },
                                    label = { Text(cat.name, fontSize = 12.sp) }
                                )
                            }
                        }

                        // Tags categorization
                        OutlinedTextField(
                            value = tagsInput,
                            onValueChange = { tagsInput = it },
                            label = { Text("Study Material Tags (e.g., Math, Science, STEM)") },
                            placeholder = { Text("Math, Calculus, STEM...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Suggested:", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterVertically))
                            suggestedTags.take(4).forEach { tag ->
                                FilterChip(
                                    selected = tagsInput.contains(tag, ignoreCase = true),
                                    onClick = {
                                        tagsInput = if (tagsInput.isBlank()) tag else "$tagsInput, $tag"
                                    },
                                    label = { Text("#$tag", fontSize = 11.sp) }
                                )
                            }
                        }

                        Text("Difficulty Level:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Easy", "Medium", "Hard").forEach { level ->
                                FilterChip(
                                    selected = difficulty.equals(level, ignoreCase = true),
                                    onClick = { difficulty = level },
                                    label = { Text(level) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = questionCount.toString(),
                                onValueChange = { questionCount = it.toIntOrNull() ?: 5 },
                                label = { Text("Count") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = durationMinutes.toString(),
                                onValueChange = { durationMinutes = it.toIntOrNull() ?: 10 },
                                label = { Text("Duration (mins)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        // Manual Questions Form
                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("Quiz Set Title") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Select Subject Category:")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = selectedCat?.id == cat.id,
                                    onClick = { selectedCat = cat },
                                    label = { Text(cat.name, fontSize = 12.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = tagsInput,
                            onValueChange = { tagsInput = it },
                            label = { Text("Tags / Topics (comma-separated)") },
                            placeholder = { Text("Math, Calculus, STEM...") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Added Questions: ${manualQuestions.size}")

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Add Question Item:", fontWeight = FontWeight.Bold)
                                    if (onRequestAiQuestionSuggestion != null) {
                                        FilledTonalButton(
                                            onClick = {
                                                val topic = titleInput.ifBlank { selectedCat?.name ?: "Academic Quiz" }
                                                onRequestAiQuestionSuggestion(topic, difficulty) { gen ->
                                                    qText = gen.questionText
                                                    optA = gen.optionA
                                                    optB = gen.optionB
                                                    optC = gen.optionC
                                                    optD = gen.optionD
                                                    correctIdx = gen.correctOptionIndex
                                                    explanation = gen.explanation
                                                }
                                            },
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = BentoViolet.copy(alpha = 0.15f),
                                                contentColor = BentoViolet
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("dialog_ai_suggest_question_button")
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("✨ AI Suggest", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                OutlinedTextField(value = qText, onValueChange = { qText = it }, label = { Text("Question Text") }, modifier = Modifier.fillMaxWidth())

                                if (qText.isNotBlank() && onRequestAiDistractorsSuggestion != null) {
                                    OutlinedButton(
                                        onClick = {
                                            val topic = titleInput.ifBlank { selectedCat?.name ?: "Academic Quiz" }
                                            onRequestAiDistractorsSuggestion(qText, topic, difficulty) { gen ->
                                                optA = gen.optionA
                                                optB = gen.optionB
                                                optC = gen.optionC
                                                optD = gen.optionD
                                                correctIdx = gen.correctOptionIndex
                                                if (gen.explanation.isNotBlank()) {
                                                    explanation = gen.explanation
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("dialog_ai_distractors_button")
                                    ) {
                                        Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("✨ Auto-Generate Choices & Distractors", fontSize = 11.sp)
                                    }
                                }

                                OutlinedTextField(value = optA, onValueChange = { optA = it }, label = { Text("Option A") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = optB, onValueChange = { optB = it }, label = { Text("Option B") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = optC, onValueChange = { optC = it }, label = { Text("Option C") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = optD, onValueChange = { optD = it }, label = { Text("Option D") }, modifier = Modifier.fillMaxWidth())

                                Text("Correct Choice:")
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("A" to 0, "B" to 1, "C" to 2, "D" to 3).forEach { (label, idx) ->
                                        FilterChip(
                                            selected = correctIdx == idx,
                                            onClick = { correctIdx = idx },
                                            label = { Text("Option $label") }
                                        )
                                    }
                                }

                                OutlinedTextField(value = explanation, onValueChange = { explanation = it }, label = { Text("Explanation") }, modifier = Modifier.fillMaxWidth())

                                Button(
                                    onClick = {
                                        if (qText.isNotBlank() && optA.isNotBlank() && optB.isNotBlank()) {
                                            manualQuestions.add(
                                                QuestionEntity(
                                                    id = "m_q_${manualQuestions.size + 1}",
                                                    quizSetId = "",
                                                    questionText = qText,
                                                    optionA = optA,
                                                    optionB = optB,
                                                    optionC = if (optC.isBlank()) "None" else optC,
                                                    optionD = if (optD.isBlank()) "All of the above" else optD,
                                                    correctOptionIndex = correctIdx,
                                                    explanation = explanation
                                                )
                                            )
                                            qText = ""
                                            optA = ""
                                            optB = ""
                                            optC = ""
                                            optD = ""
                                            explanation = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Add Question to Set")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isGeneratingAi) {
                Button(
                    onClick = {
                        val cat = selectedCat ?: categories.firstOrNull() ?: return@Button
                        if (modeTab == 0) {
                            if (topicInput.isNotBlank()) {
                                onGenerateAi(
                                    topicInput.trim(),
                                    titleInput.trim(),
                                    descInput.trim(),
                                    cat.id,
                                    cat.name,
                                    questionCount,
                                    durationMinutes,
                                    difficulty,
                                    tagsInput.trim()
                                )
                            }
                        } else {
                            if (titleInput.isNotBlank() && manualQuestions.isNotEmpty()) {
                                onCreateManual(
                                    titleInput.trim(),
                                    descInput.trim(),
                                    cat.id,
                                    cat.name,
                                    durationMinutes,
                                    60,
                                    difficulty,
                                    manualQuestions.toList(),
                                    tagsInput.trim()
                                )
                            }
                        }
                    }
                ) {
                    Text(if (modeTab == 0) "Generate with Gemini" else "Publish Quiz Set")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isGeneratingAi) "Run in Background" else "Cancel")
            }
        }
    )
}
