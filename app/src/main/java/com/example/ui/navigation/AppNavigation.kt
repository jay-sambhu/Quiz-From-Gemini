package com.example.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.data.model.UserRole
import com.example.ui.QuizViewModel
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.auth.LoginScreen
import com.example.ui.leaderboard.LeaderboardScreen
import com.example.ui.settings.UserSettingsScreen
import com.example.ui.student.AttemptHistoryScreen
import com.example.ui.student.QuizResultScreen
import com.example.ui.student.StudentDashboardScreen
import com.example.ui.student.TakeQuizScreen
import com.example.ui.teacher.TeacherDashboardScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Auth : Screen("auth", "Sign In", Icons.Default.AccountCircle)
    object StudentDashboard : Screen("student_dashboard", "Student", Icons.Default.MenuBook)
    object TeacherDashboard : Screen("teacher_dashboard", "Teacher", Icons.Default.School)
    object AdminDashboard : Screen("admin_dashboard", "Admin", Icons.Default.AdminPanelSettings)
    object Leaderboard : Screen("leaderboard", "Leaderboard", Icons.Default.Leaderboard)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object TakeQuiz : Screen("take_quiz", "Take Quiz", Icons.Default.PlayArrow)
    object QuizResult : Screen("quiz_result", "Results", Icons.Default.CheckCircle)
    object AttemptHistory : Screen("attempt_history", "History", Icons.Default.History)
}

@Composable
fun AppNavigation(viewModel: QuizViewModel) {
    val navController = rememberNavController()
    val currentUser by viewModel.currentUser.collectAsState()
    val uiMessage by viewModel.uiEventMessage.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUiMessage()
        }
    }

    val currentNavBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentNavBackStackEntry?.destination?.route

    // Dynamic role-based navigation destinations
    val navItems = remember(currentUser) {
        val list = mutableListOf<Screen>()
        when (currentUser?.role) {
            UserRole.ADMIN -> {
                list.add(Screen.AdminDashboard)
                list.add(Screen.TeacherDashboard)
                list.add(Screen.StudentDashboard)
                list.add(Screen.Leaderboard)
                list.add(Screen.Settings)
            }
            UserRole.TEACHER -> {
                list.add(Screen.TeacherDashboard)
                list.add(Screen.StudentDashboard)
                list.add(Screen.Leaderboard)
                list.add(Screen.Settings)
            }
            UserRole.STUDENT -> {
                list.add(Screen.StudentDashboard)
                list.add(Screen.Leaderboard)
                list.add(Screen.Settings)
            }
            null -> {
                // When unauthenticated, no bottom bar
            }
        }
        list
    }

    // Auto-navigate to appropriate role dashboard when currentUser state changes (e.g. login or switch)
    LaunchedEffect(currentUser) {
        val user = currentUser
        if (user == null) {
            if (currentRoute != Screen.Auth.route) {
                navController.navigate(Screen.Auth.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else if (currentRoute == Screen.Auth.route) {
            val destination = when (user.role) {
                UserRole.ADMIN -> Screen.AdminDashboard.route
                UserRole.TEACHER -> Screen.TeacherDashboard.route
                UserRole.STUDENT -> Screen.StudentDashboard.route
            }
            navController.navigate(destination) {
                popUpTo(Screen.Auth.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val showBottomBar = currentRoute in navItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    navItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (currentUser != null) {
                when (currentUser?.role) {
                    UserRole.ADMIN -> Screen.AdminDashboard.route
                    UserRole.TEACHER -> Screen.TeacherDashboard.route
                    else -> Screen.StudentDashboard.route
                }
            } else Screen.Auth.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Auth.route) {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = { verifiedRole ->
                        val destination = when (verifiedRole) {
                            UserRole.ADMIN -> Screen.AdminDashboard.route
                            UserRole.TEACHER -> Screen.TeacherDashboard.route
                            UserRole.STUDENT -> Screen.StudentDashboard.route
                        }
                        navController.navigate(destination) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.StudentDashboard.route) {
                StudentDashboardScreen(
                    viewModel = viewModel,
                    onStartQuiz = { quizSet ->
                        viewModel.startQuiz(quizSet)
                        navController.navigate(Screen.TakeQuiz.route)
                    },
                    onViewHistory = {
                        navController.navigate(Screen.AttemptHistory.route)
                    }
                )
            }

            composable(Screen.TeacherDashboard.route) {
                TeacherDashboardScreen(viewModel = viewModel)
            }

            composable(Screen.AdminDashboard.route) {
                AdminDashboardScreen(viewModel = viewModel)
            }

            composable(Screen.Leaderboard.route) {
                LeaderboardScreen(viewModel = viewModel)
            }

            composable(Screen.Settings.route) {
                UserSettingsScreen(
                    viewModel = viewModel,
                    onSwitchAccountClick = {
                        navController.navigate(Screen.Auth.route)
                    }
                )
            }

            composable(Screen.TakeQuiz.route) {
                TakeQuizScreen(
                    viewModel = viewModel,
                    onQuizSubmitted = {
                        navController.navigate(Screen.QuizResult.route) {
                            popUpTo(Screen.TakeQuiz.route) { inclusive = true }
                        }
                    },
                    onCancel = {
                        viewModel.exitQuizSession()
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.QuizResult.route) {
                QuizResultScreen(
                    viewModel = viewModel,
                    onDone = {
                        viewModel.exitQuizSession()
                        navController.navigate(Screen.StudentDashboard.route) {
                            popUpTo(Screen.StudentDashboard.route) { inclusive = true }
                        }
                    },
                    onReviewHistory = {
                        navController.navigate(Screen.AttemptHistory.route)
                    }
                )
            }

            composable(Screen.AttemptHistory.route) {
                AttemptHistoryScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onRetakeQuiz = { quizSet ->
                        viewModel.startQuiz(quizSet)
                        navController.navigate(Screen.TakeQuiz.route)
                    }
                )
            }
        }
    }
}
