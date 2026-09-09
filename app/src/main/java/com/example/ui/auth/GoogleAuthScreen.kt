package com.example.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.UserEntity
import com.example.data.model.UserRole
import com.example.ui.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleAuthScreen(
    viewModel: QuizViewModel,
    onLoginSuccess: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()

    var showAccountDialog by remember { mutableStateOf(false) }
    var showCustomLoginDialog by remember { mutableStateOf(false) }

    var inputEmail by remember { mutableStateOf("") }
    var inputName by remember { mutableStateOf("") }
    var inputRole by remember { mutableStateOf(UserRole.STUDENT) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
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
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Quiz Platform",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Interactive Learning & Classroom Competition",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Current Session Card if logged in
            currentUser?.let { user ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.name.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = user.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when (user.role) {
                                    UserRole.ADMIN -> Color(0xFFD32F2F)
                                    UserRole.TEACHER -> Color(0xFF1976D2)
                                    UserRole.STUDENT -> Color(0xFF388E3C)
                                }
                            ) {
                                Text(
                                    text = user.role.name,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onLoginSuccess,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("continue_dashboard_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Continue as ${user.name.split(" ").firstOrNull() ?: user.name}")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sign In / Switch Account Buttons
            OutlinedButton(
                onClick = { showAccountDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("google_login_btn"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (currentUser == null) "Sign in with Google Account" else "Switch / Choose Account Profile",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { showCustomLoginDialog = true },
                modifier = Modifier.testTag("add_new_google_user_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add New Google User")
            }
        }
    }

    // Account Selector Dialog
    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose Account Profile")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Select an existing user profile from the database:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    allUsers.forEach { user ->
                        val isSelected = currentUser?.id == user.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.switchActiveUser(user)
                                    showAccountDialog = false
                                    onLoginSuccess()
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
                TextButton(onClick = { showAccountDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Add Custom Google User Dialog
    if (showCustomLoginDialog) {
        AlertDialog(
            onDismissRequest = { showCustomLoginDialog = false },
            title = { Text("Sign In with Google Account") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputEmail,
                        onValueChange = { inputEmail = it },
                        label = { Text("Google Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Select User Role:", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UserRole.entries.forEach { role ->
                            FilterChip(
                                selected = inputRole == role,
                                onClick = { inputRole = role },
                                label = { Text(role.name) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputName.isNotBlank() && inputEmail.isNotBlank()) {
                            viewModel.loginWithGoogleAccount(
                                email = inputEmail.trim(),
                                name = inputName.trim(),
                                role = inputRole
                            )
                            showCustomLoginDialog = false
                            onLoginSuccess()
                        }
                    }
                ) {
                    Text("Sign In & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomLoginDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
