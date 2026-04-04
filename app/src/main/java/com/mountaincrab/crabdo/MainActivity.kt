package com.mountaincrab.crabdo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.mountaincrab.crabdo.auth.AuthRepository
import com.mountaincrab.crabdo.ui.navigation.AppNavigation
import com.mountaincrab.crabdo.ui.navigation.Screen
import com.mountaincrab.crabdo.ui.theme.CrabbanTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CrabbanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isAuthReady by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        authRepository.ensureAuthenticated()
                        isAuthReady = true
                    }

                    if (!isAuthReady) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val navController = rememberNavController()
                        AppNavigation(
                            navController = navController,
                            startDestination = Screen.PinnedBoard.route
                        )
                    }
                }
            }
        }
    }
}
