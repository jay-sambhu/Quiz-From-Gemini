package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.avatar.AvatarPreset
import com.example.data.avatar.CuratedAvatarCatalog
import com.example.data.local.entities.UserEntity
import com.example.data.model.UserRole
import com.example.ui.QuizViewModel
import com.example.ui.theme.*

private enum class ProfileDialogTab(val title: String) {
    AI_GENERATE("✨ Generate AI"),
    GALLERY("🎨 Select Avatar"),
    EDIT_PROFILE("👤 Profile Info")
}

/**
 * High-craft, full-featured User Profile & AI Avatar Studio Dialog.
 * Allows Students and Teachers to:
 * - Generate custom AI profile avatars via prompt & style (Gemini with procedural fallback)
 * - Browse and select curated avatars tailored to their role
 * - Edit display name, subject preferences, and notification settings
 * - Quick-access camera & gallery photo actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileAvatarDialog(
    user: UserEntity,
    viewModel: QuizViewModel,
    onDismiss: () -> Unit,
    onLaunchCamera: () -> Unit,
    onLaunchGallery: () -> Unit
) {
    val context = LocalContext.current
    val isGenerating by viewModel.isGeneratingAiAvatar.collectAsState()
    val statusMessage by viewModel.aiAvatarStatusMessage.collectAsState()

    var selectedTab by remember { mutableStateOf(ProfileDialogTab.AI_GENERATE) }

    // AI Generation State
    var customPrompt by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("3D Pixar") }
    var recentlyGeneratedUri by remember { mutableStateOf<String?>(null) }

    // Profile Details State
    var editedName by remember { mutableStateOf(user.name) }
    var editedSubject by remember { mutableStateOf(user.preferredSubject.ifBlank { "Computer Science" }) }
    var emailNotifs by remember { mutableStateOf(user.emailNotificationsEnabled) }

    val styles = listOf(
        "3D Pixar",
        "Cyberpunk Neon",
        "Anime / Manga",
        "Pixel Art",
        "Watercolor",
        "Minimalist Vector",
        "Academic Oil Painting"
    )

    val studentInspirationPrompts = listOf(
        "Astronaut student exploring Martian physics with neon helmet",
        "Anime student studying mathematics under cherry blossoms",
        "Cyberpunk coder with glowing cyan headphones and hoodie",
        "Bio-tech researcher with molecular DNA hologram",
        "Retro 8-bit pixel gamer reading an ancient spell book",
        "Curious astronomy student observing constellations through telescope"
    )

    val teacherInspirationPrompts = listOf(
        "Distinguished professor in academic regalia with golden wireframe glasses",
        "Quantum physics mentor with laser spectacles and atomic orbit display",
        "Warm literature teacher in a sunlit wooden library with vintage books",
        "Robotics faculty dean with sleek robotic helper pin",
        "Mathematics professor sketching elegant fractal geometry",
        "World history scholar holding ancient parchment and antique globe"
    )

    val inspirationPrompts = if (user.role == UserRole.TEACHER) teacherInspirationPrompts else studentInspirationPrompts

    val allPresets = remember(user.role) {
        CuratedAvatarCatalog.getAllPresets(user.role)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .border(1.dp, BentoBorder, RoundedCornerShape(24.dp))
                .testTag("user_profile_avatar_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = BentoPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = BentoPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Profile & AI Avatar Studio",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Personalize your ${user.role.name.lowercase()} profile",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dialog_close_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close dialog")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Current Profile Banner Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = BentoSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BentoBorder, RoundedCornerShape(18.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            // Avatar Glow Effect
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                if (user.role == UserRole.TEACHER) BentoAmber else BentoPrimary,
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                            UserProfileAvatar(
                                name = user.name,
                                photoUrl = user.photoUrl,
                                size = 68.dp,
                                backgroundColor = if (user.role == UserRole.TEACHER) BentoAmber else BentoViolet,
                                showCameraBadge = true,
                                badgeIcon = Icons.Default.AutoAwesome,
                                badgeContentDescription = "AI Avatar Active"
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (user.role == UserRole.TEACHER) BentoAmber.copy(alpha = 0.15f) else BentoPrimary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = user.role.name,
                                        color = if (user.role == UserRole.TEACHER) BentoAmber else BentoPrimary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (user.preferredSubject.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.School,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = BentoPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = user.preferredSubject,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = BentoPrimary
                                    )
                                }
                            }
                        }

                        // Remove Photo Option if present
                        if (!user.photoUrl.isNullOrBlank()) {
                            IconButton(
                                onClick = {
                                    viewModel.updateUserProfilePhoto("")
                                    Toast.makeText(context, "Avatar reset to initials", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("dialog_reset_avatar_btn")
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Reset Avatar",
                                    tint = BentoRose
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Tabs
                PrimaryTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = Color.Transparent,
                    divider = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ProfileDialogTab.values().forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        ProfileDialogTab.AI_GENERATE -> {
                            AiAvatarGenerateTabContent(
                                user = user,
                                customPrompt = customPrompt,
                                onPromptChange = { customPrompt = it },
                                selectedStyle = selectedStyle,
                                onStyleSelect = { selectedStyle = it },
                                styles = styles,
                                inspirationPrompts = inspirationPrompts,
                                isGenerating = isGenerating,
                                statusMessage = statusMessage,
                                recentlyGeneratedUri = recentlyGeneratedUri,
                                onGenerateClick = {
                                    viewModel.generateAiAvatar(
                                        context = context,
                                        prompt = customPrompt,
                                        style = selectedStyle
                                    ) { generatedUri ->
                                        recentlyGeneratedUri = generatedUri
                                        Toast.makeText(context, "AI Avatar applied to profile!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onApplyUri = { uri ->
                                    viewModel.selectAndApplyAvatar(uri)
                                    Toast.makeText(context, "Avatar set to profile!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        ProfileDialogTab.GALLERY -> {
                            AvatarGalleryTabContent(
                                user = user,
                                presets = allPresets,
                                onSelectPreset = { preset ->
                                    val uri = preset.resolveAvatarUri(context)
                                    viewModel.selectAndApplyAvatar(uri)
                                    Toast.makeText(context, "'${preset.title}' applied to profile!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        ProfileDialogTab.EDIT_PROFILE -> {
                            ProfileEditTabContent(
                                name = editedName,
                                onNameChange = { editedName = it },
                                subject = editedSubject,
                                onSubjectChange = { editedSubject = it },
                                emailNotifications = emailNotifs,
                                onEmailNotificationsChange = { emailNotifs = it },
                                userRole = user.role,
                                onSave = {
                                    if (editedName.isBlank()) {
                                        Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                                        return@ProfileEditTabContent
                                    }
                                    viewModel.updateUserProfile(
                                        name = editedName.trim(),
                                        preferredSubject = editedSubject,
                                        emailNotificationsEnabled = emailNotifs
                                    )
                                    Toast.makeText(context, "Profile changes saved!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Utilities: Camera / Gallery Quick Launch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Or use standard photos:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onLaunchCamera()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("dialog_quick_camera_btn")
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Camera", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onLaunchGallery()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("dialog_quick_gallery_btn")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gallery", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiAvatarGenerateTabContent(
    user: UserEntity,
    customPrompt: String,
    onPromptChange: (String) -> Unit,
    selectedStyle: String,
    onStyleSelect: (String) -> Unit,
    styles: List<String>,
    inspirationPrompts: List<String>,
    isGenerating: Boolean,
    statusMessage: String?,
    recentlyGeneratedUri: String?,
    onGenerateClick: () -> Unit,
    onApplyUri: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Prompt Input
        item {
            Column {
                Text(
                    text = "Describe your Avatar:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = customPrompt,
                    onValueChange = onPromptChange,
                    placeholder = {
                        Text(
                            if (user.role == UserRole.TEACHER)
                                "e.g. Astrophysics professor with telescope and starry chalkboard..."
                            else
                                "e.g. Space cadet student with cyan headphones and hologram..."
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_ai_avatar_prompt"),
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = {
                        if (customPrompt.isNotBlank()) {
                            IconButton(onClick = { onPromptChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    minLines = 2,
                    maxLines = 3
                )
            }
        }

        // Style Selector
        item {
            Column {
                Text(
                    text = "Select Artistic Style:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(styles) { style ->
                        val isSelected = selectedStyle == style
                        FilterChip(
                            selected = isSelected,
                            onClick = { onStyleSelect(style) },
                            label = { Text(style, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BentoPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("style_chip_$style")
                        )
                    }
                }
            }
        }

        // Inspiration Chips
        item {
            Column {
                Text(
                    text = "One-Tap Inspiration Prompts:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(inspirationPrompts) { prompt ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BentoSurface,
                            modifier = Modifier
                                .border(1.dp, BentoBorder, RoundedCornerShape(12.dp))
                                .clickable { onPromptChange(prompt) }
                                .widthIn(max = 240.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = BentoAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Generate Action Button & Status
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onGenerateClick,
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BentoPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_generate_ai_avatar")
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Generating Avatar...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (customPrompt.isBlank()) "Generate Default Role Avatar" else "Generate with AI Engine",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                AnimatedVisibility(visible = isGenerating || !statusMessage.isNullOrBlank()) {
                    Text(
                        text = statusMessage ?: "Contacting AI models...",
                        style = MaterialTheme.typography.bodySmall,
                        color = BentoPrimary,
                        modifier = Modifier.padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Recently Generated Preview Card
        if (recentlyGeneratedUri != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BentoPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = recentlyGeneratedUri,
                            contentDescription = "Generated Avatar",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(2.dp, BentoPrimary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Avatar Ready!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BentoPrimary
                            )
                            Text(
                                text = "Successfully saved and applied to your profile.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarGalleryTabContent(
    user: UserEntity,
    presets: List<AvatarPreset>,
    onSelectPreset: (AvatarPreset) -> Unit
) {
    var roleFilter by remember { mutableStateOf("ALL") }

    val filteredPresets = remember(roleFilter, presets) {
        when (roleFilter) {
            "STUDENT" -> presets.filter { it.targetRole == UserRole.STUDENT }
            "TEACHER" -> presets.filter { it.targetRole == UserRole.TEACHER }
            else -> presets
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Role filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = roleFilter == "ALL",
                onClick = { roleFilter = "ALL" },
                label = { Text("All Avatars (${presets.size})") },
                shape = RoundedCornerShape(10.dp)
            )
            FilterChip(
                selected = roleFilter == "STUDENT",
                onClick = { roleFilter = "STUDENT" },
                label = { Text("Students 🎓") },
                shape = RoundedCornerShape(10.dp)
            )
            FilterChip(
                selected = roleFilter == "TEACHER",
                onClick = { roleFilter = "TEACHER" },
                label = { Text("Teachers 👨‍🏫") },
                shape = RoundedCornerShape(10.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredPresets, key = { it.id }) { preset ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BentoBorder, RoundedCornerShape(16.dp))
                        .clickable { onSelectPreset(preset) }
                        .testTag("preset_card_${preset.id}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BentoPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = preset.emoji,
                                    fontSize = 26.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = preset.style,
                            style = MaterialTheme.typography.labelSmall,
                            color = BentoPrimary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = preset.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        FilledTonalButton(
                            onClick = { onSelectPreset(preset) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Use Avatar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileEditTabContent(
    name: String,
    onNameChange: (String) -> Unit,
    subject: String,
    onSubjectChange: (String) -> Unit,
    emailNotifications: Boolean,
    onEmailNotificationsChange: (Boolean) -> Unit,
    userRole: UserRole,
    onSave: () -> Unit
) {
    val subjectOptions = listOf(
        "Computer Science",
        "Mathematics",
        "Physics & Astronomy",
        "Chemistry & Biology",
        "World History",
        "Literature & Arts",
        "General Science"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Display Name:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_display_name")
                )
            }
        }

        item {
            Column {
                Text(
                    text = if (userRole == UserRole.TEACHER) "Teaching Specialty / Subject:" else "Favorite Subject / Focus:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(subjectOptions) { subj ->
                        val isSelected = subject == subj
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSubjectChange(subj) },
                            label = { Text(subj) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BentoPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BentoSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BentoBorder, RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Email Notifications",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Receive quiz feedback, leaderboard alerts, and updates",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = emailNotifications,
                        onCheckedChange = onEmailNotificationsChange,
                        modifier = Modifier.testTag("switch_email_notifications")
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_save_profile_details")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Profile Changes", fontWeight = FontWeight.Bold)
            }
        }
    }
}
