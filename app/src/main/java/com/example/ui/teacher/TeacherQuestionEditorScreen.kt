package com.example.ui.teacher

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
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
import com.example.data.local.entities.QuestionEntity
import com.example.data.local.entities.QuizSetEntity
import com.example.ui.QuizViewModel
import com.example.ui.components.BentoCard
import com.example.ui.components.BentoPillTag
import com.example.ui.theme.*
import java.util.UUID

/**
 * Teacher-specific interface for viewing, manually creating, editing, and managing
 * quiz questions within a Quiz Set, with integrated Google Gemini AI content suggestion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherQuestionEditorScreen(
    quizSet: QuizSetEntity,
    viewModel: QuizViewModel,
    onBack: () -> Unit
) {
    val questionsList by viewModel.getQuestionsForQuizSet(quizSet.id).collectAsState(initial = emptyList())
    val isAiSuggesting by viewModel.isAiSuggestingQuestion.collectAsState()
    val aiSuggestionStatus by viewModel.aiSuggestionStatus.collectAsState()
    val isSavingQuestion by viewModel.isSavingQuestion.collectAsState()

    // State for managing active edit or creation mode
    var activeEditingQuestion by remember { mutableStateOf<QuestionEntity?>(null) }
    var isCreatingNewQuestion by remember { mutableStateOf(false) }
    var showAiPromptDialog by remember { mutableStateOf(false) }
    var showImportCsvDialog by remember { mutableStateOf(false) }
    var questionToDelete by remember { mutableStateOf<QuestionEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (activeEditingQuestion != null) {
                                "Edit Question"
                            } else if (isCreatingNewQuestion) {
                                "New Question"
                            } else {
                                "Question Bank"
                            },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${quizSet.title} • ${quizSet.categoryName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (activeEditingQuestion != null || isCreatingNewQuestion) {
                                activeEditingQuestion = null
                                isCreatingNewQuestion = false
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("teacher_question_editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (activeEditingQuestion == null && !isCreatingNewQuestion) {
                        FilledTonalButton(
                            onClick = { showImportCsvDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = BentoEmerald.copy(alpha = 0.15f),
                                contentColor = BentoEmerald
                            ),
                            modifier = Modifier.testTag("header_import_csv_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import CSV", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        FilledTonalButton(
                            onClick = { isCreatingNewQuestion = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = BentoPrimary.copy(alpha = 0.15f),
                                contentColor = BentoPrimary
                            ),
                            modifier = Modifier.testTag("header_add_question_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Question", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            )
        },
        floatingActionButton = {
            if (activeEditingQuestion == null && !isCreatingNewQuestion) {
                ExtendedFloatingActionButton(
                    onClick = { isCreatingNewQuestion = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Create Question", fontWeight = FontWeight.Bold) },
                    containerColor = BentoPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("teacher_add_question_fab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (activeEditingQuestion != null || isCreatingNewQuestion) {
                // Form for creating or editing a question
                val initialQuestion = activeEditingQuestion ?: QuestionEntity(
                    id = "q_man_${UUID.randomUUID().toString().take(8)}",
                    quizSetId = quizSet.id,
                    questionText = "",
                    optionA = "",
                    optionB = "",
                    optionC = "",
                    optionD = "",
                    correctOptionIndex = 0,
                    explanation = ""
                )

                QuestionFormEditor(
                    quizSet = quizSet,
                    initialQuestion = initialQuestion,
                    isEditingExisting = activeEditingQuestion != null,
                    isAiSuggesting = isAiSuggesting,
                    aiSuggestionStatus = aiSuggestionStatus,
                    isSaving = isSavingQuestion,
                    onOpenAiSuggestDialog = { showAiPromptDialog = true },
                    onAutoDistractors = { qText ->
                        viewModel.requestAiDistractorsSuggestion(
                            questionText = qText,
                            topic = "${quizSet.title}, ${quizSet.categoryName}",
                            difficulty = quizSet.difficulty
                        ) { suggestion ->
                            // Handled via callback inside QuestionFormEditor
                        }
                    },
                    onRequestAiSuggestion = { topicHint, diff, draft, callback ->
                        viewModel.requestAiQuestionSuggestion(
                            topic = if (topicHint.isNotBlank()) topicHint else "${quizSet.title} (${quizSet.categoryName})",
                            promptHint = topicHint,
                            difficulty = diff,
                            currentDraft = draft,
                            onResult = callback
                        )
                    },
                    onRequestDistractors = { qText, callback ->
                        viewModel.requestAiDistractorsSuggestion(
                            questionText = qText,
                            topic = "${quizSet.title}, ${quizSet.categoryName}",
                            difficulty = quizSet.difficulty,
                            onResult = callback
                        )
                    },
                    onSave = { updatedQuestion ->
                        viewModel.saveQuestion(updatedQuestion) {
                            activeEditingQuestion = null
                            isCreatingNewQuestion = false
                        }
                    },
                    onCancel = {
                        activeEditingQuestion = null
                        isCreatingNewQuestion = false
                    },
                    onDelete = { qId ->
                        viewModel.deleteQuestion(qId, quizSet.id)
                        activeEditingQuestion = null
                        isCreatingNewQuestion = false
                    }
                )
            } else {
                // Question Bank List View
                QuestionBankListView(
                    quizSet = quizSet,
                    questions = questionsList,
                    onAddQuestion = { isCreatingNewQuestion = true },
                    onEditQuestion = { question -> activeEditingQuestion = question },
                    onOpenAiSuggestDialog = { showAiPromptDialog = true },
                    onImportCsv = { showImportCsvDialog = true },
                    onDeleteQuestion = { question -> questionToDelete = question },
                    onDuplicateQuestion = { question ->
                        val duplicate = question.copy(
                            id = "q_dup_${UUID.randomUUID().toString().take(8)}",
                            questionText = "${question.questionText} (Copy)"
                        )
                        viewModel.saveQuestion(duplicate)
                    }
                )
            }

            // Confirmation Dialog for Question Deletion
            questionToDelete?.let { q ->
                AlertDialog(
                    onDismissRequest = { questionToDelete = null },
                    title = { Text("Delete Question", fontWeight = FontWeight.Bold) },
                    text = {
                        Text("Are you sure you want to delete this question? This change will synchronize across the question bank and cloud database.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteQuestion(q.id, quizSet.id)
                                questionToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BentoRose)
                        ) {
                            Text("Delete", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { questionToDelete = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // AI Content Generation Prompt Dialog
            if (showAiPromptDialog) {
                AiQuestionPromptModalDialog(
                    quizSet = quizSet,
                    isGenerating = isAiSuggesting,
                    onDismiss = { showAiPromptDialog = false },
                    onGenerate = { topicPrompt, difficulty ->
                        showAiPromptDialog = false
                        viewModel.requestAiQuestionSuggestion(
                            topic = topicPrompt.ifBlank { "${quizSet.title} (${quizSet.categoryName})" },
                            promptHint = topicPrompt,
                            difficulty = difficulty,
                            currentDraft = activeEditingQuestion
                        ) { generated ->
                            // When generated from list view, open editor directly with generated question!
                            activeEditingQuestion = QuestionEntity(
                                id = "q_ai_gen_${UUID.randomUUID().toString().take(8)}",
                                quizSetId = quizSet.id,
                                questionText = generated.questionText,
                                optionA = generated.optionA,
                                optionB = generated.optionB,
                                optionC = generated.optionC,
                                optionD = generated.optionD,
                                correctOptionIndex = generated.correctOptionIndex,
                                explanation = generated.explanation
                            )
                            isCreatingNewQuestion = false
                        }
                    }
                )
            }

            // Bulk CSV Import Dialog for this Quiz Set
            if (showImportCsvDialog) {
                val allCategories by viewModel.allCategories.collectAsState(initial = emptyList())
                ImportQuizCsvModalDialog(
                    categories = allCategories,
                    targetQuizSet = quizSet,
                    availableQuizzes = listOf(quizSet),
                    viewModel = viewModel,
                    onDismiss = { showImportCsvDialog = false },
                    onImportSuccess = { quizTitle, count ->
                        showImportCsvDialog = false
                    }
                )
            }
        }
    }
}

/**
 * Question Bank List View showing all questions in the Quiz Set.
 */
@Composable
private fun QuestionBankListView(
    quizSet: QuizSetEntity,
    questions: List<QuestionEntity>,
    onAddQuestion: () -> Unit,
    onEditQuestion: (QuestionEntity) -> Unit,
    onOpenAiSuggestDialog: () -> Unit,
    onImportCsv: () -> Unit,
    onDeleteQuestion: (QuestionEntity) -> Unit,
    onDuplicateQuestion: (QuestionEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp)
    ) {
        // Bento Summary Header Card
        item {
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                cornerRadius = 20.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = quizSet.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BentoPillTag(
                                text = quizSet.categoryName,
                                containerColor = BentoViolet.copy(alpha = 0.15f),
                                contentColor = BentoViolet
                            )
                            BentoPillTag(
                                text = quizSet.difficulty,
                                containerColor = BentoPrimary.copy(alpha = 0.15f),
                                contentColor = BentoPrimary
                            )
                            BentoPillTag(
                                text = "${questions.size} Questions",
                                containerColor = BentoEmerald.copy(alpha = 0.15f),
                                contentColor = BentoEmerald
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BentoPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null,
                            tint = BentoPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Gemini AI Assistant Banner
        item {
            BentoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_suggestion_banner_card"),
                backgroundColor = BentoViolet.copy(alpha = 0.08f),
                borderColor = BentoViolet.copy(alpha = 0.35f),
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(BentoViolet.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = BentoViolet,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Generator for Question Content",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = BentoViolet
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Trigger Gemini AI to draft pedagogy-calibrated questions, distractors, or explanations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenAiSuggestDialog,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BentoViolet,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("trigger_ai_generator_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Suggest Question", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onImportCsv,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_csv_questions_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import CSV", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onAddQuestion,
                        modifier = Modifier.testTag("manual_create_question_button")
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Manual", fontSize = 12.sp)
                    }
                }
            }
        }

        if (questions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = BentoViolet.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No questions in this quiz set yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Bulk import from CSV cache or create with Gemini AI.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = onImportCsv,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = BentoEmerald.copy(alpha = 0.15f),
                                    contentColor = BentoEmerald
                                ),
                                modifier = Modifier.testTag("empty_state_import_csv_button")
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import from CSV", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onAddQuestion,
                                modifier = Modifier.testTag("empty_state_manual_add_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Question")
                            }
                        }
                    }
                }
            }
        } else {
            itemsIndexed(questions, key = { _, q -> q.id }) { index, question ->
                QuestionListItemCard(
                    index = index + 1,
                    question = question,
                    onEdit = { onEditQuestion(question) },
                    onDelete = { onDeleteQuestion(question) },
                    onDuplicate = { onDuplicateQuestion(question) }
                )
            }
        }
    }
}

/**
 * Question card item in the list.
 */
@Composable
private fun QuestionListItemCard(
    index: Int,
    question: QuestionEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    var expandedExplanation by remember { mutableStateOf(false) }

    BentoCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("question_item_card_${question.id}"),
        cornerRadius = 18.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BentoPrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Q$index",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = BentoPrimary
                    )
                }

                Text(
                    text = question.questionText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            Row {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp).testTag("edit_question_${question.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Question",
                        tint = BentoPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDuplicate,
                    modifier = Modifier.size(36.dp).testTag("duplicate_question_${question.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Duplicate Question",
                        tint = BentoCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp).testTag("delete_question_${question.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Question",
                        tint = BentoRose,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Options listing with correct answer highlighted
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val options = listOf(
                "A" to question.optionA,
                "B" to question.optionB,
                "C" to question.optionC,
                "D" to question.optionD
            )

            options.forEachIndexed { optIndex, (letter, text) ->
                val isCorrect = question.correctOptionIndex == optIndex
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isCorrect) BentoEmerald.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = if (isCorrect) androidx.compose.foundation.BorderStroke(1.dp, BentoEmerald.copy(alpha = 0.5f)) else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isCorrect) BentoEmerald else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = letter,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCorrect) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isCorrect) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isCorrect) BentoEmerald else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        if (isCorrect) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Correct Answer",
                                tint = BentoEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        if (question.explanation.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BentoViolet.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedExplanation = !expandedExplanation }
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = BentoViolet,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pedagogical Explanation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = BentoViolet
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = question.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expandedExplanation) Int.MAX_VALUE else 2
                    )
                }
            }
        }
    }
}

/**
 * Question Form Editor Composable for creating or editing an individual question,
 * with options to trigger Gemini AI for suggested content.
 */
@Composable
private fun QuestionFormEditor(
    quizSet: QuizSetEntity,
    initialQuestion: QuestionEntity,
    isEditingExisting: Boolean,
    isAiSuggesting: Boolean,
    aiSuggestionStatus: String?,
    isSaving: Boolean = false,
    onOpenAiSuggestDialog: () -> Unit,
    onAutoDistractors: (String) -> Unit,
    onRequestAiSuggestion: (String, String, QuestionEntity?, (com.example.data.gemini.GeneratedQuizQuestion) -> Unit) -> Unit,
    onRequestDistractors: (String, (com.example.data.gemini.GeneratedQuizQuestion) -> Unit) -> Unit,
    onSave: (QuestionEntity) -> Unit,
    onCancel: () -> Unit,
    onDelete: (String) -> Unit
) {
    var questionText by remember { mutableStateOf(initialQuestion.questionText) }
    var optionA by remember { mutableStateOf(initialQuestion.optionA) }
    var optionB by remember { mutableStateOf(initialQuestion.optionB) }
    var optionC by remember { mutableStateOf(initialQuestion.optionC) }
    var optionD by remember { mutableStateOf(initialQuestion.optionD) }
    var correctIndex by remember { mutableIntStateOf(initialQuestion.correctOptionIndex) }
    var explanation by remember { mutableStateOf(initialQuestion.explanation) }

    var validationError by remember { mutableStateOf<String?>(null) }
    var showLocalAiDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // AI Suggestion Banner
        BentoCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_suggestion_editor_card"),
            backgroundColor = BentoViolet.copy(alpha = 0.08f),
            borderColor = BentoViolet.copy(alpha = 0.4f),
            cornerRadius = 18.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BentoViolet.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = BentoViolet,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gemini AI Question Assistant",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = BentoViolet
                    )
                    Text(
                        text = "Trigger AI to suggest entire question or auto-generate plausible distractors.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isAiSuggesting) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BentoViolet.copy(alpha = 0.15f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = BentoViolet
                    )
                    Text(
                        text = aiSuggestionStatus ?: "Gemini AI is generating question content...",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = BentoViolet
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showLocalAiDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BentoViolet,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_suggest_full_question_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Suggest Question", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            if (questionText.isNotBlank()) {
                                onRequestDistractors(questionText) { generated ->
                                    optionA = generated.optionA
                                    optionB = generated.optionB
                                    optionC = generated.optionC
                                    optionD = generated.optionD
                                    correctIndex = generated.correctOptionIndex
                                    if (generated.explanation.isNotBlank()) {
                                        explanation = generated.explanation
                                    }
                                }
                            } else {
                                validationError = "Please enter a question statement first to auto-generate distractors."
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_suggest_distractors_button")
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto Distractors", fontSize = 12.sp)
                    }
                }
            }
        }

        // Validation error banner if any
        validationError?.let { err ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = BentoRose.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, BentoRose.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = BentoRose, modifier = Modifier.size(18.dp))
                    Text(err, color = BentoRose, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Question Statement Field
        OutlinedTextField(
            value = questionText,
            onValueChange = {
                questionText = it
                validationError = null
            },
            label = { Text("Question Statement") },
            placeholder = { Text("e.g., Which protocol operates at the Transport layer of the OSI model?") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("question_text_input"),
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(14.dp)
        )

        // Options Section
        Text(
            text = "Answer Options (4 Choices):",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
        )

        val optionsList = listOf(
            Triple("A", optionA, { v: String -> optionA = v }),
            Triple("B", optionB, { v: String -> optionB = v }),
            Triple("C", optionC, { v: String -> optionC = v }),
            Triple("D", optionD, { v: String -> optionD = v })
        )

        optionsList.forEachIndexed { index, (letter, textVal, onValChange) ->
            val isSelectedAsCorrect = correctIndex == index
            OutlinedTextField(
                value = textVal,
                onValueChange = {
                    onValChange(it)
                    validationError = null
                },
                label = { Text("Option $letter ${if (isSelectedAsCorrect) "(Correct Answer)" else ""}") },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isSelectedAsCorrect) BentoEmerald else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isSelectedAsCorrect) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                trailingIcon = {
                    IconButton(
                        onClick = { correctIndex = index },
                        modifier = Modifier.testTag("select_correct_option_$letter")
                    ) {
                        Icon(
                            imageVector = if (isSelectedAsCorrect) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Set as correct answer",
                            tint = if (isSelectedAsCorrect) BentoEmerald else MaterialTheme.colorScheme.outline
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("option_${letter.lowercase()}_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Correct Answer Choice Indicator
        Text(
            text = "Select Correct Answer Key:",
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("A" to 0, "B" to 1, "C" to 2, "D" to 3).forEach { (label, idx) ->
                val isSelected = correctIndex == idx
                FilterChip(
                    selected = isSelected,
                    onClick = { correctIndex = idx },
                    label = { Text("Option $label", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BentoEmerald.copy(alpha = 0.2f),
                        selectedLabelColor = BentoEmerald,
                        selectedLeadingIconColor = BentoEmerald
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("correct_choice_$label")
                )
            }
        }

        // Pedagogical Explanation
        OutlinedTextField(
            value = explanation,
            onValueChange = { explanation = it },
            label = { Text("Explanation & Feedback (Optional)") },
            placeholder = { Text("Explain why the selected choice is correct and debunk the distractors.") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("explanation_input"),
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Action Buttons: Save, Cancel, Delete
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .testTag("cancel_edit_question_button")
            ) {
                Text("Cancel")
            }

            if (isEditingExisting) {
                OutlinedButton(
                    onClick = { onDelete(initialQuestion.id) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BentoRose),
                    modifier = Modifier.testTag("delete_question_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            Button(
                onClick = {
                    if (questionText.isBlank()) {
                        validationError = "Question statement cannot be empty."
                        return@Button
                    }
                    if (optionA.isBlank() || optionB.isBlank()) {
                        validationError = "Please provide at least Option A and Option B."
                        return@Button
                    }

                    val updated = initialQuestion.copy(
                        quizSetId = quizSet.id,
                        questionText = questionText.trim(),
                        optionA = optionA.trim(),
                        optionB = optionB.trim(),
                        optionC = if (optionC.isBlank()) "None of the above" else optionC.trim(),
                        optionD = if (optionD.isBlank()) "All of the above" else optionD.trim(),
                        correctOptionIndex = correctIndex,
                        explanation = explanation.trim()
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                enabled = !isSaving,
                modifier = Modifier
                    .weight(1.5f)
                    .testTag("save_question_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Saving...",
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEditingExisting) "Save Changes" else "Add to Set",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Local AI dialog invoked from within editor
    if (showLocalAiDialog) {
        AiQuestionPromptModalDialog(
            quizSet = quizSet,
            isGenerating = isAiSuggesting,
            currentDraftText = questionText,
            onDismiss = { showLocalAiDialog = false },
            onGenerate = { promptHint, diff ->
                showLocalAiDialog = false
                val draft = initialQuestion.copy(questionText = questionText)
                onRequestAiSuggestion(promptHint, diff, draft) { generated ->
                    questionText = generated.questionText
                    optionA = generated.optionA
                    optionB = generated.optionB
                    optionC = generated.optionC
                    optionD = generated.optionD
                    correctIndex = generated.correctOptionIndex
                    explanation = generated.explanation
                }
            }
        )
    }
}

/**
 * Dialog prompting teacher for optional topic/concept hint and difficulty to guide Gemini AI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiQuestionPromptModalDialog(
    quizSet: QuizSetEntity,
    isGenerating: Boolean,
    currentDraftText: String = "",
    onDismiss: () -> Unit,
    onGenerate: (topicPrompt: String, difficulty: String) -> Unit
) {
    var promptInput by remember {
        mutableStateOf(currentDraftText.ifBlank { "${quizSet.title} - ${quizSet.categoryName}" })
    }
    var selectedDifficulty by remember { mutableStateOf(quizSet.difficulty) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BentoViolet)
                Text("Gemini AI Question Suggester", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Provide a topic, concept, or specific instructional goal for the AI to generate a question, 4 plausible options, and explanation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    label = { Text("Topic / Focus Prompt") },
                    placeholder = { Text("e.g. Memory complexity of Quicksort vs Mergesort") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_prompt_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "Cognitive Difficulty:",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Easy", "Medium", "Hard").forEach { diff ->
                        FilterChip(
                            selected = selectedDifficulty.equals(diff, ignoreCase = true),
                            onClick = { selectedDifficulty = diff },
                            label = { Text(diff) },
                            modifier = Modifier.testTag("ai_diff_chip_$diff")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onGenerate(promptInput.trim(), selectedDifficulty)
                },
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = BentoViolet),
                modifier = Modifier.testTag("confirm_ai_generate_button")
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generating...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate Suggestion")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_ai_dialog_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
