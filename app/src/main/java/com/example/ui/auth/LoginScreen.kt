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
import androidx.compose.material.icons.automirrored.filled.Login
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: QuizViewModel,
    onLoginSuccess: (UserRole) -> Unit
) {
    val isCloudConnected by viewModel.isCloudConnected.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()
    val authErrorMessage by viewModel.authErrorMessage.collectAsState()

    var isSignUpMode by remember { mutableStateOf(false) }

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetPasswordEmail by remember { mutableStateOf("") }
    var resetPasswordStatus by remember { mutableStateOf<String?>(null) }
    var isSendingReset by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // App Identity & Shield Icon
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Secure Authentication",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Quiz Platform",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Secure Role-Based Portal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Auth Mode Toggle (Sign In vs Sign Up)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    // Sign In Tab
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isSignUpMode) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (!isSignUpMode) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                isSignUpMode = false
                                viewModel.clearAuthError()
                            }
                    ) {
                        Text(
                            text = "Sign In",
                            fontWeight = if (!isSignUpMode) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isSignUpMode) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    // Create Account Tab
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSignUpMode) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (isSignUpMode) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                isSignUpMode = true
                                viewModel.clearAuthError()
                            }
                    ) {
                        Text(
                            text = "Create Account",
                            fontWeight = if (isSignUpMode) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSignUpMode) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error Message Banner
            authErrorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Credential Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Full Name (Only in Sign-Up mode)
                    AnimatedVisibility(visible = isSignUpMode) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Full Name") },
                            placeholder = { Text("e.g. John Doe") },
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

                    // Email Address
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            if (authErrorMessage != null) viewModel.clearAuthError()
                        },
                        label = { Text("Email Address") },
                        placeholder = { Text("name@domain.com") },
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

                    // Password
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            if (authErrorMessage != null) viewModel.clearAuthError()
                        },
                        label = { Text("Password") },
                        placeholder = { Text("Minimum 6 characters") },
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
                            imeAction = if (isSignUpMode) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            onDone = { focusManager.clearFocus() }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input")
                    )

                    // Confirm Password (Only in Sign-Up mode)
                    AnimatedVisibility(visible = isSignUpMode) {
                        OutlinedTextField(
                            value = confirmPasswordInput,
                            onValueChange = { confirmPasswordInput = it },
                            label = { Text("Confirm Password") },
                            placeholder = { Text("Re-enter password") },
                            leadingIcon = {
                                Icon(Icons.Default.LockReset, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            isError = confirmPasswordInput.isNotEmpty() && confirmPasswordInput != passwordInput,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Forgot Password link (Sign In mode only)
                    if (!isSignUpMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    resetPasswordEmail = emailInput
                                    resetPasswordStatus = null
                                    showForgotPasswordDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Primary Submit Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (isSignUpMode) {
                                if (confirmPasswordInput != passwordInput) {
                                    return@Button
                                }
                                viewModel.signUpWithFirebase(
                                    email = emailInput.trim(),
                                    password = passwordInput.trim(),
                                    name = nameInput.trim(),
                                    role = UserRole.STUDENT,
                                    onSuccess = onLoginSuccess
                                )
                            } else {
                                viewModel.signInWithFirebase(
                                    email = emailInput.trim(),
                                    password = passwordInput.trim(),
                                    onSuccess = onLoginSuccess
                                )
                            }
                        },
                        enabled = !isAuthLoading && emailInput.isNotBlank() && passwordInput.isNotBlank() &&
                                (!isSignUpMode || (nameInput.isNotBlank() && confirmPasswordInput == passwordInput)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("auth_submit_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isAuthLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isSignUpMode) Icons.Default.PersonAdd else Icons.AutoMirrored.Filled.Login,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSignUpMode) "Create Account" else "Sign In",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Mode switch toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSignUpMode) "Already registered?" else "New user?",
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
                                text = if (isSignUpMode) "Sign In here" else "Create an account",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Reset Password", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Enter your registered email address. We will send you instructions to securely reset your password.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = resetPasswordEmail,
                        onValueChange = { resetPasswordEmail = it },
                        label = { Text("Account Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    resetPasswordStatus?.let { status ->
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (status.contains("sent", ignoreCase = true)) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSendingReset = true
                        viewModel.sendPasswordResetEmail(resetPasswordEmail) { success, msg ->
                            isSendingReset = false
                            resetPasswordStatus = msg
                        }
                    },
                    enabled = !isSendingReset && resetPasswordEmail.isNotBlank(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isSendingReset) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Send Reset Link")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
