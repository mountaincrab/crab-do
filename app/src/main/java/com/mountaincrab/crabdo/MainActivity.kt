package com.mountaincrab.crabdo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.mountaincrab.crabdo.auth.AuthRepository
import com.mountaincrab.crabdo.ui.navigation.AppNavigation
import com.mountaincrab.crabdo.ui.navigation.Screen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mountaincrab.crabdo.ui.theme.CrabbanTheme
import com.mountaincrab.crabdo.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var authRepository: AuthRepository

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* continue either way */ }

    private var openAddReminder by mutableStateOf(false)
    private var openReminderId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (intent?.getBooleanExtra("open_add_reminder", false) == true) {
            openAddReminder = true
        }
        intent?.getStringExtra("open_reminder_id")?.let { openReminderId = it }
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val appTheme by themeViewModel.appTheme.collectAsStateWithLifecycle()
            CrabbanTheme(appTheme = appTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isAuthReady by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        runCatching { authRepository.ensureAuthenticated() }
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
                        val shouldOpenAddReminder = openAddReminder
                        val shouldOpenReminderId = openReminderId
                        LaunchedEffect(shouldOpenAddReminder) {
                            if (shouldOpenAddReminder) {
                                openAddReminder = false
                            }
                        }
                        LaunchedEffect(shouldOpenReminderId) {
                            if (shouldOpenReminderId != null) {
                                openReminderId = null
                            }
                        }
                        AppNavigation(
                            navController = navController,
                            startDestination = Screen.PinnedBoard.route,
                            openAddReminder = shouldOpenAddReminder,
                            openReminderId = shouldOpenReminderId
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("open_add_reminder", false)) {
            openAddReminder = true
        }
        intent.getStringExtra("open_reminder_id")?.let { openReminderId = it }
    }
}
