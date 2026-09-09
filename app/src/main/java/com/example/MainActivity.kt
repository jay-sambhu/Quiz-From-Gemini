package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.QuizViewModel
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.QuizPlatformTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val quizViewModel: QuizViewModel = viewModel()
            val themeMode by quizViewModel.themeMode.collectAsState()
            val systemIsDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> systemIsDark
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
            }

            QuizPlatformTheme(darkTheme = isDarkTheme, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel = quizViewModel)
                }
            }
        }
    }
}
