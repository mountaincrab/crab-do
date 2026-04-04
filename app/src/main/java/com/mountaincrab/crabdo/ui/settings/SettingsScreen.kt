package com.mountaincrab.crabdo.ui.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val boards by viewModel.boards.collectAsStateWithLifecycle()
    val pinnedBoardId by viewModel.pinnedBoardId.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Account section
            Text(
                "Account",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            HorizontalDivider()
            ListItem(
                headlineContent = {
                    Text(if (viewModel.isAnonymous) "Anonymous user" else viewModel.userEmail ?: "")
                },
                supportingContent = {
                    Text(if (viewModel.isAnonymous) "Link a Google account to back up your data"
                         else "Signed in with Google")
                },
                trailingContent = {
                    if (viewModel.isAnonymous) {
                        TextButton(onClick = { /* TODO: trigger Google Sign-In */ }) {
                            Text("Link Google")
                        }
                    }
                }
            )

            // Exact alarm permission banner
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !viewModel.canScheduleExactAlarms) {
                HorizontalDivider()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            )
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Exact alarms not permitted",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("Tap to grant permission for precise reminder timing",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // Pinned board
            Spacer(Modifier.height(8.dp))
            Text(
                "Pinned Board",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            HorizontalDivider()
            var expanded by remember { mutableStateOf(false) }
            val pinnedBoard = boards.find { it.id == pinnedBoardId }
            ListItem(
                headlineContent = { Text("Pinned board") },
                supportingContent = { Text(pinnedBoard?.title ?: "None") },
                trailingContent = {
                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Text("Change")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = { viewModel.setPinnedBoard(null); expanded = false }
                            )
                            boards.forEach { board ->
                                DropdownMenuItem(
                                    text = { Text(board.title) },
                                    onClick = { viewModel.setPinnedBoard(board.id); expanded = false }
                                )
                            }
                        }
                    }
                }
            )

            // About
            Spacer(Modifier.height(8.dp))
            Text(
                "About",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Crabban") },
                supportingContent = { Text("Version 1.0") }
            )
        }
    }
}
