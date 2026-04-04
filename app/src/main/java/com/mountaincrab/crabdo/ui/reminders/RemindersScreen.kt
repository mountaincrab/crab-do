package com.mountaincrab.crabdo.ui.reminders

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mountaincrab.crabdo.ui.navigation.Screen
import com.mountaincrab.crabdo.ui.reminders.components.ReminderItem

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
                onClick = { navController.navigate(Screen.AddEditReminder.createRoute()) }
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap + to create a reminder",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(scaffoldPadding)
            ) {
                items(reminders, key = { it.id }) { reminder ->
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
                        Card(
                            onClick = {
                                navController.navigate(
                                    Screen.AddEditReminder.createRoute(reminder.id)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ReminderItem(
                                reminder = reminder,
                                onToggleEnabled = { viewModel.toggleEnabled(reminder) }
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
