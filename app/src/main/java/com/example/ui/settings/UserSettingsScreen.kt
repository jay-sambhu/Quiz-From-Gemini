package com.example.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.model.UserRole
import com.example.ui.QuizViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSettingsScreen(
    viewModel: QuizViewModel,
    onSwitchAccountClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val currentUser by viewModel.currentUser.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val uiEventMessage by viewModel.uiEventMessage.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    var preferredSubject by remember(currentUser) { mutableStateOf(currentUser?.preferredSubject ?: "All") }
    var emailNotifsEnabled by remember(currentUser) { mutableStateOf(currentUser?.emailNotificationsEnabled ?: true) }

    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Show feedback messages
    LaunchedEffect(uiEventMessage) {
        uiEventMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // Camera capture launcher via FileProvider
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            viewModel.updateUserProfilePhoto(tempPhotoUri.toString())
            Toast.makeText(context, "Profile photo captured and stored in Firestore!", Toast.LENGTH_SHORT).show()
        }
    }

    // Direct bitmap preview capture fallback
    val takePicturePreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            currentUser?.let { user ->
                try {
                    val photoDir = File(context.filesDir, "profile_photos").apply { mkdirs() }
                    val photoFile = File(photoDir, "avatar_${user.id}_${System.currentTimeMillis()}.jpg")
                    photoFile.outputStream().use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    val uriString = Uri.fromFile(photoFile).toString()
                    viewModel.updateUserProfilePhoto(uriString)
                    Toast.makeText(context, "Profile photo saved and stored in Firestore!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error saving photo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Photo picker launcher (PickVisualMedia)
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            currentUser?.let { user ->
                try {
                    val photoDir = File(context.filesDir, "profile_photos").apply { mkdirs() }
                    val photoFile = File(photoDir, "avatar_${user.id}_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        photoFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    val persistentUriString = Uri.fromFile(photoFile).toString()
                    viewModel.updateUserProfilePhoto(persistentUriString)
                    Toast.makeText(context, "Profile photo updated & saved to Firestore!", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    viewModel.updateUserProfilePhoto(uri.toString())
                }
            }
        }
    }

    fun launchCameraFlow() {
        val user = currentUser ?: return
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val photoDir = File(context.filesDir, "profile_photos").apply { mkdirs() }
                val photoFile = File(photoDir, "avatar_${user.id}_${System.currentTimeMillis()}.jpg")
                val photoUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                tempPhotoUri = photoUri
                takePictureLauncher.launch(photoUri)
            } catch (e: Exception) {
                // Fallback to preview contract if FileProvider has issue
                takePicturePreviewLauncher.launch(null)
            }
        } else {
            showPermissionRationale = true
        }
    }

    // Permission launcher for CAMERA
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showPermissionRationale = false
            launchCameraFlow()
        } else {
            Toast.makeText(context, "Camera permission is required to capture a profile photo", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = BentoViolet)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Profile & Preferences", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleLightDarkMode() },
                        modifier = Modifier.testTag("topbar_theme_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (themeMode == AppThemeMode.DARK) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = "Toggle Dark/Light Mode",
                            tint = if (themeMode == AppThemeMode.DARK) BentoViolet else BentoAmber
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Profile Header Card with Camera Access
            currentUser?.let { user ->
                BentoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_profile_card"),
                    backgroundColor = BentoViolet.copy(alpha = 0.12f),
                    borderColor = BentoViolet.copy(alpha = 0.35f),
                    cornerRadius = 24.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Interactive Avatar with Camera Badge
                        UserProfileAvatar(
                            name = user.name,
                            photoUrl = user.photoUrl.ifBlank { null },
                            size = 88.dp,
                            backgroundColor = BentoViolet,
                            showCameraBadge = true,
                            onCameraClick = { showPhotoSourceDialog = true },
                            modifier = Modifier.testTag("user_profile_avatar")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BentoPillTag(
                                text = "ROLE: ${user.role.name}",
                                containerColor = when (user.role) {
                                    UserRole.ADMIN -> BentoRose
                                    UserRole.TEACHER -> BentoViolet
                                    UserRole.STUDENT -> BentoEmerald
                                },
                                contentColor = Color.White,
                                icon = Icons.Default.Badge
                            )

                            if (user.photoUrl.isNotBlank()) {
                                BentoPillTag(
                                    text = "PHOTO SYNCED",
                                    containerColor = BentoEmerald.copy(alpha = 0.2f),
                                    contentColor = BentoEmerald,
                                    icon = Icons.Default.CheckCircle
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Camera Capture & Upload Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { launchCameraFlow() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("capture_profile_photo_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Capture Photo", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("upload_photo_gallery_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Choose Photo", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Stored Firestore Document Image URI Info Box
                        if (user.photoUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BentoViolet.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("firestore_photo_uri_card")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CloudDone,
                                                contentDescription = null,
                                                tint = BentoEmerald,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Firestore Profile Image URI",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = BentoViolet
                                            )
                                        }

                                        Row {
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(user.photoUrl))
                                                    Toast.makeText(context, "URI copied to clipboard", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy URI", modifier = Modifier.size(16.dp))
                                            }

                                            IconButton(
                                                onClick = {
                                                    viewModel.updateUserProfilePhoto("")
                                                    Toast.makeText(context, "Photo removed from Firestore profile", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove photo", tint = BentoRose, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = user.photoUrl,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "✓ Document collection: 'users/${user.id}' (field: photoUrl)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BentoEmerald,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Global Theme Appearance Card
            BentoSectionTitle(title = "App Appearance & Theme")

            BentoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("theme_settings_card"),
                cornerRadius = 20.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Quick Light / Dark Mode Toggle Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (themeMode == AppThemeMode.DARK) BentoViolet.copy(alpha = 0.2f) else BentoAmber.copy(alpha = 0.15f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (themeMode == AppThemeMode.DARK) Icons.Default.DarkMode else Icons.Default.LightMode,
                                        contentDescription = null,
                                        tint = if (themeMode == AppThemeMode.DARK) BentoViolet else BentoAmber,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Dark Mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when (themeMode) {
                                        AppThemeMode.DARK -> "Dark theme active across entire application"
                                        AppThemeMode.LIGHT -> "Light theme active across entire application"
                                        AppThemeMode.SYSTEM -> "Auto-following device system appearance"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = themeMode == AppThemeMode.DARK,
                            onCheckedChange = { isDark ->
                                viewModel.setThemeMode(if (isDark) AppThemeMode.DARK else AppThemeMode.LIGHT)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BentoViolet,
                                checkedTrackColor = BentoViolet.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.testTag("theme_quick_toggle_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // 3-way Mode Segmented Selector (Light, Dark, System)
                    Text(
                        text = "Theme Preference:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppThemeMode.values().forEach { mode ->
                            val isSelected = themeMode == mode
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) BentoViolet.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, BentoViolet) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.setThemeMode(mode) }
                                    .testTag("theme_mode_${mode.name.lowercase()}_btn")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = when (mode) {
                                            AppThemeMode.LIGHT -> Icons.Default.LightMode
                                            AppThemeMode.DARK -> Icons.Default.DarkMode
                                            AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) BentoViolet else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = when (mode) {
                                            AppThemeMode.LIGHT -> "Light"
                                            AppThemeMode.DARK -> "Dark"
                                            AppThemeMode.SYSTEM -> "System"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) BentoViolet else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Preferences Card
            BentoSectionTitle(title = "Learning & System Preferences")

            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Preferred Subject
                    Text("Preferred Subject Focus:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val subjects = listOf("All") + allCategories.map { it.name }
                        subjects.take(4).forEach { sub ->
                            FilterChip(
                                selected = preferredSubject == sub,
                                onClick = {
                                    preferredSubject = sub
                                    viewModel.updateUserPreferences(sub, emailNotifsEnabled)
                                },
                                label = { Text(sub, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BentoViolet,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Email Notifications Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Email Assignment Notifications", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Receive email alerts when teachers assign new question sets.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = emailNotifsEnabled,
                            onCheckedChange = {
                                emailNotifsEnabled = it
                                viewModel.updateUserPreferences(preferredSubject, it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = BentoPrimary, checkedTrackColor = BentoViolet.copy(alpha = 0.3f))
                        )
                    }
                }
            }

            // Cloud Firestore Status & Sync
            val isCloudConnected by viewModel.isCloudConnected.collectAsState()
            val cloudSyncMsg by viewModel.cloudSyncMessage.collectAsState()

            BentoSectionTitle(title = "Cloud Firestore Database")

            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = if (isCloudConnected) BentoCyan.copy(alpha = 0.08f) else BentoAmber.copy(alpha = 0.08f),
                borderColor = if (isCloudConnected) BentoCyan.copy(alpha = 0.35f) else BentoAmber.copy(alpha = 0.35f),
                cornerRadius = 20.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCloudConnected) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = if (isCloudConnected) BentoCyan else BentoAmber,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Firebase Firestore",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = cloudSyncMsg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        BentoPillTag(
                            text = if (isCloudConnected) "ONLINE" else "CACHED",
                            containerColor = if (isCloudConnected) BentoEmerald.copy(alpha = 0.15f) else BentoAmber.copy(alpha = 0.15f),
                            contentColor = if (isCloudConnected) BentoEmerald else BentoAmber,
                            icon = if (isCloudConnected) Icons.Default.CheckCircle else Icons.Default.Sync
                        )
                    }

                    Text(
                        text = "Synchronized Collections: 'users' (Profiles, Roles & Photos), 'quiz_sets' (Exams), 'questions' (Question Bank), 'quiz_attempts' (Session Data).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { viewModel.syncWithFirestoreCloud() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sync_firestore_cloud_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = BentoCyan, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync Local Database with Firestore Cloud", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Account Switch / Sign Out Button
            Button(
                onClick = {
                    viewModel.signOutUser()
                    onSwitchAccountClick()
                },
                colors = ButtonDefaults.buttonColors(containerColor = BentoRose.copy(alpha = 0.15f), contentColor = BentoRose),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("switch_google_account_btn"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out / Switch Account", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Modal dialog for selecting photo action (Camera vs Gallery vs Remove)
    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            icon = {
                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = BentoPrimary, modifier = Modifier.size(32.dp))
            },
            title = { Text("Profile Photo Options", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Capture a fresh photo using your device camera or pick an existing image from your gallery. The image URI will be stored in your Cloud Firestore profile document.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            showPhotoSourceDialog = false
                            launchCameraFlow()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_capture_camera_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Capture with Camera", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            showPhotoSourceDialog = false
                            pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_pick_gallery_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose from Gallery / Photos", fontWeight = FontWeight.SemiBold)
                    }

                    if (!currentUser?.photoUrl.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                showPhotoSourceDialog = false
                                viewModel.updateUserProfilePhoto("")
                                Toast.makeText(context, "Profile photo removed", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BentoRose),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dialog_remove_photo_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Remove Current Photo", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoSourceDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Permission Explanation / Rationale Dialog
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            icon = {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = BentoRose, modifier = Modifier.size(32.dp))
            },
            title = { Text("Camera Permission Required", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Quiz Platform needs camera permission to allow you to capture your profile photo directly within the app and upload its image URI to your user profile in Cloud Firestore.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary)
                ) {
                    Text("Grant Permission", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Not Now")
                }
            }
        )
    }
}


