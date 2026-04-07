package com.mountaincrab.crabdo.ui.reminders

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mountaincrab.crabdo.ui.navigation.Screen
import com.mountaincrab.crabdo.ui.reminders.components.ReminderItem
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    navController: NavController,
    innerPadding: PaddingValues = PaddingValues(),
    viewModel: RemindersViewModel = hiltViewModel()
) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Reminders") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddEditReminder.createRoute()) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add reminder")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { scaffoldPadding ->
        if (reminders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No reminders yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap + to create a reminder",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            val now = System.currentTimeMillis()
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val tomorrowStart = todayStart + 86_400_000L

            val todayReminders = reminders.filter { it.nextTriggerMillis in todayStart until tomorrowStart }
            val upcomingReminders = reminders.filter { it.nextTriggerMillis >= tomorrowStart }
            val pastReminders = reminders.filter { it.nextTriggerMillis < todayStart }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                if (todayReminders.isNotEmpty()) {
                    item { SectionHeader("Today") }
                    items(todayReminders, key = { it.id }) { reminder ->
                        ReminderRow(reminder, navController, viewModel)
                    }
                }
                if (upcomingReminders.isNotEmpty()) {
                    item { SectionHeader("Upcoming") }
                    items(upcomingReminders, key = { it.id }) { reminder ->
                        ReminderRow(reminder, navController, viewModel)
                    }
                }
                if (pastReminders.isNotEmpty()) {
                    item { SectionHeader("Past") }
                    items(pastReminders, key = { it.id }) { reminder ->
                        ReminderRow(reminder, navController, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderRow(
    reminder: com.mountaincrab.crabdo.data.local.entity.ReminderEntity,
    navController: NavController,
    viewModel: RemindersViewModel
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                viewModel.deleteReminder(reminder.id)
                true
            } else false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surface
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        Surface(
            onClick = {
                navController.navigate(
                    Screen.AddEditReminder.createRoute(reminder.id)
                )
            },
            color = MaterialTheme.colorScheme.surface
        ) {
            ReminderItem(
                reminder = reminder,
                onToggleEnabled = { viewModel.toggleEnabled(reminder) },
                onDelete = { viewModel.deleteReminder(reminder.id) }
            )
        }
    }
}
