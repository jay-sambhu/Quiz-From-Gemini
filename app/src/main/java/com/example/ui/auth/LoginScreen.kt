package com.example.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.UserEntity
import com.example.data.model.UserRole
import com.example.ui.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: QuizViewModel,
    onLoginSuccess: (UserRole) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val isCloudConnected by viewModel.isCloudConnected.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()
    val authErrorMessage by viewModel.authErrorMessage.collectAsState()

    var isSignUpMode by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var showQuickSwitchDialog by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    val roleColor = when (selectedRole) {
        UserRole.ADMIN -> Color(0xFFD32F2F)
        UserRole.TEACHER -> Color(0xFF1976D2)
        UserRole.STUDENT -> Color(0xFF388E3C)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Brand Header Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "App Logo",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Quiz Platform",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Secure Authentication for Admins, Teachers & Students",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Cloud Firestore storage indicator badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isCloudConnected) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCloudConnected) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = if (isCloudConnected) Color(0xFF2E7D32) else Color(0xFFEF6C00),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCloudConnected) "Stored & Synced in Firebase Firestore" else "Offline Cache Mode (Firebase Ready)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCloudConnected) Color(0xFF2E7D32) else Color(0xFFEF6C00)
                    )
                }
            }

            // Current Session Card if already active
            currentUser?.let { user ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    when (user.role) {
                                        UserRole.ADMIN -> Color(0xFFD32F2F)
                                        UserRole.TEACHER -> Color(0xFF1976D2)
                                        UserRole.STUDENT -> Color(0xFF388E3C)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.name.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${user.email} • ${user.role.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FilledTonalButton(
                            onClick = { currentUser?.let { onLoginSuccess(it.role) } },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("continue_current_session_btn")
                        ) {
                            Text("Enter")
                        }
                    }
                }
            }

            // Role Selector Chips
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Sign in as:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UserRole.entries.forEach { role ->
                            val isSelected = selectedRole == role
                            val chipColor = when (role) {
                                UserRole.ADMIN -> Color(0xFFD32F2F)
                                UserRole.TEACHER -> Color(0xFF1976D2)
                                UserRole.STUDENT -> Color(0xFF388E3C)
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedRole = role
                                    // Auto-populate convenient role template credentials for testing
                                    if (!isSignUpMode && emailInput.isBlank()) {
                                        emailInput = when (role) {
                                            UserRole.ADMIN -> "admin@quiz.com"
                                            UserRole.TEACHER -> "teacher@quiz.com"
                                            UserRole.STUDENT -> "student@quiz.com"
                                        }
                                        passwordInput = "password123"
                                    }
                                },
                                label = {
                                    Text(
                                        text = role.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (role) {
                                            UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                                            UserRole.TEACHER -> Icons.Default.School
                                            UserRole.STUDENT -> Icons.Default.Person
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipColor,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("role_chip_${role.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick 1-Tap Sign-In Buttons per Role (Tests Firestore verification & conditional routing)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "1-Tap Direct Role Portal Access:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Student Portal
                        Button(
                            onClick = {
                                selectedRole = UserRole.STUDENT
                                emailInput = "student@quiz.com"
                                passwordInput = "password123"
                                viewModel.quickSignInAsRole(UserRole.STUDENT, onLoginSuccess)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_login_student_btn"),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Student", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Teacher Dashboard
                        Button(
                            onClick = {
                                selectedRole = UserRole.TEACHER
                                emailInput = "teacher@quiz.com"
                                passwordInput = "password123"
                                viewModel.quickSignInAsRole(UserRole.TEACHER, onLoginSuccess)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_login_teacher_btn"),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Teacher", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Admin Console
                        Button(
                            onClick = {
                                selectedRole = UserRole.ADMIN
                                emailInput = "admin@quiz.com"
                                passwordInput = "password123"
                                viewModel.quickSignInAsRole(UserRole.ADMIN, onLoginSuccess)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_login_admin_btn"),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Admin", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Auth Error Banner
            authErrorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Input Fields Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Sign-up Full Name Input
                    AnimatedVisibility(visible = isSignUpMode) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Full Name") },
                            leadingIcon = {
                                Icon(Icons.Default.Badge, contentDescription = null)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_name_input")
                        )
                    }

                    // Email Input
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("user@quiz.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input")
                    )

                    // Password Input
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (emailInput.isNotBlank() && passwordInput.isNotBlank()) {
                                    if (isSignUpMode) {
                                        viewModel.signUpWithFirebase(
                                            email = emailInput.trim(),
                                            password = passwordInput.trim(),
                                            name = if (nameInput.isNotBlank()) nameInput.trim() else emailInput.substringBefore("@"),
                                            role = selectedRole,
                                            onSuccess = onLoginSuccess
                                        )
                                    } else {
                                        viewModel.signInWithFirebase(
                                            email = emailInput.trim(),
                                            password = passwordInput.trim(),
                                            role = selectedRole,
                                            onSuccess = onLoginSuccess
                                        )
                                    }
                                }
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Primary Submit Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (isSignUpMode) {
                                viewModel.signUpWithFirebase(
                                    email = emailInput.trim(),
                                    password = passwordInput.trim(),
                                    name = if (nameInput.isNotBlank()) nameInput.trim() else emailInput.substringBefore("@"),
                                    role = selectedRole,
                                    onSuccess = onLoginSuccess
                                )
                            } else {
                                viewModel.signInWithFirebase(
                                    email = emailInput.trim(),
                                    password = passwordInput.trim(),
                                    role = selectedRole,
                                    onSuccess = onLoginSuccess
                                )
                            }
                        },
                        enabled = !isAuthLoading && emailInput.isNotBlank() && passwordInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("auth_submit_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = roleColor)
                    ) {
                        if (isAuthLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isSignUpMode) Icons.Default.PersonAdd else Icons.Default.Login,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSignUpMode) "Create ${selectedRole.name} Account" else "Sign In as ${selectedRole.name}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Toggle between Login & Sign Up
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSignUpMode) "Already have an account?" else "Don't have an account?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = {
                                isSignUpMode = !isSignUpMode
                                viewModel.clearAuthError()
                            },
                            modifier = Modifier.testTag("toggle_auth_mode_btn")
                        ) {
                            Text(
                                text = if (isSignUpMode) "Sign In" else "Sign Up",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Account Switcher Button (Direct profile selection from Firestore/Room)
            OutlinedButton(
                onClick = { showQuickSwitchDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("quick_switch_profiles_btn"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Saved Profile (${allUsers.size} available)")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Quick Switch Account Dialog
    if (showQuickSwitchDialog) {
        AlertDialog(
            onDismissRequest = { showQuickSwitchDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Saved Account")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Choose an existing user stored in Firebase / Local Cache:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    allUsers.forEach { user ->
                        val isSelected = currentUser?.id == user.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.switchActiveUser(user) { resolvedRole ->
                                        showQuickSwitchDialog = false
                                        onLoginSuccess(resolvedRole)
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (user.role) {
                                                UserRole.ADMIN -> Color(0xFFD32F2F)
                                                UserRole.TEACHER -> Color(0xFF1976D2)
                                                UserRole.STUDENT -> Color(0xFF388E3C)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = user.name.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = user.role.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQuickSwitchDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
