package com.mountaincrab.crabdo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.mountaincrab.crabdo.auth.AuthRepository
import com.mountaincrab.crabdo.ui.navigation.AppNavigation
import com.mountaincrab.crabdo.ui.navigation.Screen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mountaincrab.crabdo.ui.theme.CrabbanTheme
import com.mountaincrab.crabdo.ui.theme.ThemeViewModel
import org.koin.android.ext.android.inject
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {

    private val authRepository: AuthRepository by inject()

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
            val themeViewModel: ThemeViewModel = koinViewModel()
            val appTheme by themeViewModel.appTheme.collectAsStateWithLifecycle()
            CrabbanTheme(appTheme = appTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authUser by authRepository.observeAuthState()
                        .collectAsStateWithLifecycle(initialValue = authRepository.currentUser)
                    val isSignedIn = authUser != null

                    val navController = rememberNavController()
                    val shouldOpenAddReminder = openAddReminder
                    val shouldOpenReminderId = openReminderId
                    LaunchedEffect(shouldOpenAddReminder) {
                        if (shouldOpenAddReminder) openAddReminder = false
                    }
                    LaunchedEffect(shouldOpenReminderId) {
                        if (shouldOpenReminderId != null) openReminderId = null
                    }
                    AppNavigation(
                        navController = navController,
                        startDestination = if (isSignedIn) Screen.PinnedBoard.route else Screen.Login.route,
                        openAddReminder = shouldOpenAddReminder,
                        openReminderId = shouldOpenReminderId
                    )
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
