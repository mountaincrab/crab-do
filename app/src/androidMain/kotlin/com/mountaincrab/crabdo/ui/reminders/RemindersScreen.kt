package com.mountaincrab.crabdo.ui.reminders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mountaincrab.crabdo.data.local.entity.OneOffReminderEntity
import com.mountaincrab.crabdo.data.local.entity.RecurringReminderEntity
import com.mountaincrab.crabdo.ui.navigation.Screen
import com.mountaincrab.crabdo.ui.reminders.components.OneOffReminderItem
import com.mountaincrab.crabdo.ui.reminders.components.RecurringReminderItem
import java.util.*
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    navController: NavController,
    innerPadding: PaddingValues = PaddingValues(),
    viewModel: RemindersViewModel = koinViewModel()
) {
    val oneOffReminders by viewModel.oneOffReminders.collectAsStateWithLifecycle()
    val completedOneOffs by viewModel.completedOneOffs.collectAsStateWithLifecycle()
    val deletedOneOffs by viewModel.deletedOneOffs.collectAsStateWithLifecycle()
    val recurringReminders by viewModel.recurringReminders.collectAsStateWithLifecycle()
    val deletedRecurring by viewModel.deletedRecurring.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Reminders") })
                TabRow(selectedTabIndex = pagerState.currentPage) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        text = { Text("One-off") }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        text = { Text("Recurring") }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val route = if (pagerState.currentPage == 0) {
                        Screen.AddEditOneOffReminder.createRoute()
                    } else {
                        Screen.AddEditRecurringReminder.createRoute()
                    }
                    navController.navigate(route)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add reminder")
            }
        }
    ) { scaffoldPadding ->
        PullToRefreshBox(
            isRefreshing = isSyncing,
            onRefresh = { viewModel.sync() },
            modifier = Modifier.fillMaxSize().padding(scaffoldPadding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> OneOffTab(oneOffReminders, completedOneOffs, deletedOneOffs, navController, viewModel)
                    1 -> RecurringTab(recurringReminders, deletedRecurring, navController, viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OneOffTab(
    reminders: List<OneOffReminderEntity>,
    completedReminders: List<OneOffReminderEntity>,
    deletedReminders: List<OneOffReminderEntity>,
    navController: NavController,
    viewModel: RemindersViewModel
) {
    var showCompleted by rememberSaveable { mutableStateOf(false) }
    var showDeleted by rememberSaveable { mutableStateOf(false) }

    if (reminders.isEmpty() && completedReminders.isEmpty() && deletedReminders.isEmpty()) {
        EmptyState("No one-off reminders", "Tap + to add one")
        return
    }

    val now = System.currentTimeMillis()
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val tomorrowStart = todayStart + 86_400_000L

    val todayItems = reminders.filter { it.scheduledAt in todayStart until tomorrowStart }
    val upcomingItems = reminders.filter { it.scheduledAt >= tomorrowStart }
    val pastItems = reminders.filter { it.scheduledAt < todayStart }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        if (todayItems.isNotEmpty()) {
            item { SectionHeader("Today") }
            items(todayItems, key = { it.id }) { reminder ->
                OneOffRow(reminder, navController, viewModel)
            }
        }
        if (upcomingItems.isNotEmpty()) {
            item { SectionHeader("Upcoming") }
            items(upcomingItems, key = { it.id }) { reminder ->
                OneOffRow(reminder, navController, viewModel)
            }
        }
        if (pastItems.isNotEmpty()) {
            item { SectionHeader("Past") }
            items(pastItems, key = { it.id }) { reminder ->
                OneOffRow(reminder, navController, viewModel)
            }
        }
        if (completedReminders.isNotEmpty()) {
            item {
                TextButton(
                    onClick = { showCompleted = !showCompleted },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(if (showCompleted) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (showCompleted) "Hide completed" else "Show completed (${completedReminders.size})")
                }
            }
            if (showCompleted) {
                item { SectionHeader("Completed") }
                items(completedReminders, key = { it.id }) { reminder ->
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        OneOffReminderItem(
                            reminder = reminder,
                            onToggleEnabled = {},
                            onDelete = { viewModel.deleteOneOff(reminder.id) },
                            completed = true
                        )
                    }
                }
            }
        }
        if (deletedReminders.isNotEmpty()) {
            item {
                TextButton(
                    onClick = { showDeleted = !showDeleted },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(if (showDeleted) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (showDeleted) "Hide deleted" else "Show deleted (${deletedReminders.size})")
                }
            }
            if (showDeleted) {
                item { SectionHeader("Deleted") }
                items(deletedReminders, key = { it.id }) { reminder ->
                    DeletedOneOffRow(reminder, viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringTab(
    reminders: List<RecurringReminderEntity>,
    deletedReminders: List<RecurringReminderEntity>,
    navController: NavController,
    viewModel: RemindersViewModel
) {
    var showDeleted by rememberSaveable { mutableStateOf(false) }

    if (reminders.isEmpty() && deletedReminders.isEmpty()) {
        EmptyState("No recurring reminders", "Tap + to add one")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        items(reminders, key = { it.id }) { reminder ->
            RecurringRow(reminder, navController, viewModel)
        }
        if (deletedReminders.isNotEmpty()) {
            item {
                TextButton(
                    onClick = { showDeleted = !showDeleted },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(if (showDeleted) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (showDeleted) "Hide deleted" else "Show deleted (${deletedReminders.size})")
                }
            }
            if (showDeleted) {
                item { SectionHeader("Deleted") }
                items(deletedReminders, key = { it.id }) { reminder ->
                    DeletedRecurringRow(reminder, viewModel)
                }
            }
        }
    }
}

@Composable
private fun OneOffRow(
    reminder: OneOffReminderEntity,
    navController: NavController,
    viewModel: RemindersViewModel
) {
    Surface(
        onClick = { navController.navigate(Screen.AddEditOneOffReminder.createRoute(reminder.id)) },
        color = MaterialTheme.colorScheme.surface
    ) {
        OneOffReminderItem(
            reminder = reminder,
            onToggleEnabled = { viewModel.toggleOneOffEnabled(reminder) },
            onDelete = { viewModel.deleteOneOff(reminder.id) }
        )
    }
}

@Composable
private fun RecurringRow(
    reminder: RecurringReminderEntity,
    navController: NavController,
    viewModel: RemindersViewModel
) {
    Surface(
        onClick = { navController.navigate(Screen.AddEditRecurringReminder.createRoute(reminder.id)) },
        color = MaterialTheme.colorScheme.surface
    ) {
        RecurringReminderItem(
            reminder = reminder,
            onToggleEnabled = { viewModel.toggleRecurringEnabled(reminder) },
            onDelete = { viewModel.deleteRecurring(reminder.id) }
        )
    }
}

@Composable
private fun DeletedOneOffRow(
    reminder: OneOffReminderEntity,
    viewModel: RemindersViewModel
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.restoreOneOff(reminder.id) }) {
                Icon(
                    Icons.Default.Restore,
                    contentDescription = "Restore",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DeletedRecurringRow(
    reminder: RecurringReminderEntity,
    viewModel: RemindersViewModel
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.restoreRecurring(reminder.id) }) {
                Icon(
                    Icons.Default.Restore,
                    contentDescription = "Restore",
                    tint = MaterialTheme.colorScheme.primary
                )
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
