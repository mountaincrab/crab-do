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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.mountaincrab.crabdo.auth.AuthRepository
import com.mountaincrab.crabdo.ui.navigation.AppNavigation
import com.mountaincrab.crabdo.ui.navigation.ReminderTarget
import com.mountaincrab.crabdo.ui.navigation.Screen
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mountaincrab.crabdo.notification.AppPermissions
import com.mountaincrab.crabdo.ui.theme.CrabbanTheme
import com.mountaincrab.crabdo.ui.theme.ThemeViewModel
import org.koin.android.ext.android.inject
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {

    private val authRepository: AuthRepository by inject()

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* continue either way */ }

    private var openAddReminder by mutableStateOf<ReminderTarget?>(null)
    private var openReminderId by mutableStateOf<String?>(null)
    private var openReminderType by mutableStateOf<ReminderTarget?>(null)
    private var openTaskId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        applyReminderIntent(intent)
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
                    val shouldOpenReminderType = openReminderType
                    val shouldOpenTaskId = openTaskId
                    LaunchedEffect(shouldOpenAddReminder) {
                        if (shouldOpenAddReminder != null) openAddReminder = null
                    }
                    LaunchedEffect(shouldOpenReminderId) {
                        if (shouldOpenReminderId != null) {
                            openReminderId = null
                            openReminderType = null
                        }
                    }
                    LaunchedEffect(shouldOpenTaskId) {
                        if (shouldOpenTaskId != null) openTaskId = null
                    }
                    AppNavigation(
                        navController = navController,
                        startDestination = if (isSignedIn) Screen.PinnedBoard.route else Screen.Login.route,
                        openAddReminder = shouldOpenAddReminder,
                        openReminderId = shouldOpenReminderId,
                        openReminderType = shouldOpenReminderType,
                        openTaskId = shouldOpenTaskId,
                    )

                    if (isSignedIn) FullScreenIntentPrompt()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        applyReminderIntent(intent)
    }

    private fun applyReminderIntent(intent: Intent?) {
        if (intent == null) return
        val type = intent.getStringExtra("reminder_type")?.let { raw ->
            runCatching { ReminderTarget.valueOf(raw) }.getOrNull()
        }
        if (intent.getBooleanExtra("open_add_reminder", false)) {
            openAddReminder = type ?: ReminderTarget.ONE_OFF
        }
        intent.getStringExtra("open_reminder_id")?.let {
            openReminderId = it
            openReminderType = type ?: ReminderTarget.ONE_OFF
        }
        intent.getStringExtra("open_task_id")?.let {
            openTaskId = it
        }
    }

    /**
     * Full-screen notifications (Android 14+) can't be requested with a runtime
     * dialog and are sometimes revoked at install, so explain and deep-link to the
     * toggle. Re-checked on every resume; "Not now" hides it until next launch.
     */
    @Composable
    private fun FullScreenIntentPrompt() {
        var canUseFullScreen by remember { mutableStateOf(AppPermissions.canUseFullScreenIntent(this)) }
        var dismissed by rememberSaveable { mutableStateOf(false) }
        LifecycleResumeEffect(Unit) {
            canUseFullScreen = AppPermissions.canUseFullScreenIntent(this@MainActivity)
            onPauseOrDispose { }
        }
        if (canUseFullScreen || dismissed) return
        AlertDialog(
            onDismissRequest = { dismissed = true },
            title = { Text("Allow full-screen alarms") },
            text = {
                Text(
                    "Crab Do needs the \"Full screen notifications\" permission to show alarm " +
                        "reminders over the lock screen. Without it, alarms only appear as a " +
                        "regular notification."
                )
            },
            confirmButton = {
                TextButton(onClick = { AppPermissions.openFullScreenIntentSettings(this@MainActivity) }) {
                    Text("Open settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { dismissed = true }) { Text("Not now") }
            }
        )
    }
}
