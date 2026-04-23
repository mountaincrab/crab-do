package com.mountaincrab.crabdo.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mountaincrab.crabdo.ui.boards.BoardListViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mountaincrab.crabdo.ui.auth.LoginScreen
import com.mountaincrab.crabdo.ui.boards.*
import com.mountaincrab.crabdo.ui.reminders.AddEditOneOffReminderScreen
import com.mountaincrab.crabdo.ui.reminders.AddEditRecurringReminderScreen
import com.mountaincrab.crabdo.ui.reminders.RemindersScreen
import com.mountaincrab.crabdo.ui.settings.SettingsScreen
import org.koin.compose.viewmodel.koinViewModel

enum class ReminderTarget { ONE_OFF, RECURRING }

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    openAddReminder: ReminderTarget? = null,
    openReminderId: String? = null,
    openReminderType: ReminderTarget? = null,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val tabRoutes = setOf(
        Screen.PinnedBoard.route,
        Screen.BoardList.route,
        Screen.Reminders.route,
        Screen.Settings.route,
    )
    val showBottomBar = (currentRoute in tabRoutes || currentRoute == Screen.KanbanBoard.route) &&
            currentRoute != Screen.Login.route

    val boardListViewModel: BoardListViewModel = koinViewModel()
    val pinnedBoardId by boardListViewModel.pinnedBoardId.collectAsStateWithLifecycle()
    val boards by boardListViewModel.boards.collectAsStateWithLifecycle()
    val pinnedBoardTitle = boards.firstOrNull { it.id == pinnedBoardId }?.title ?: "Board"

    LaunchedEffect(openAddReminder) {
        when (openAddReminder) {
            ReminderTarget.ONE_OFF ->
                navController.navigate(Screen.AddEditOneOffReminder.createRoute(fromWidget = true))
            ReminderTarget.RECURRING ->
                navController.navigate(Screen.AddEditRecurringReminder.createRoute(fromWidget = true))
            null -> Unit
        }
    }
    LaunchedEffect(openReminderId, openReminderType) {
        if (openReminderId != null && openReminderType != null) {
            val route = when (openReminderType) {
                ReminderTarget.ONE_OFF ->
                    Screen.AddEditOneOffReminder.createRoute(reminderId = openReminderId, fromWidget = true)
                ReminderTarget.RECURRING ->
                    Screen.AddEditRecurringReminder.createRoute(reminderId = openReminderId, fromWidget = true)
            }
            navController.navigate(route)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = currentRoute == Screen.PinnedBoard.route,
                        onClick = {
                            navController.navigate(Screen.PinnedBoard.route) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            }
                        },
                        icon = { Icon(Icons.Default.Star, contentDescription = "Pinned Board") },
                        label = { Text(pinnedBoardTitle, maxLines = 1) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.BoardList.route,
                        onClick = {
                            navController.navigate(Screen.BoardList.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.GridView, contentDescription = "All Boards") },
                        label = { Text("Boards") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Reminders.route,
                        onClick = {
                            navController.navigate(Screen.Reminders.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Notifications, contentDescription = "Reminders") },
                        label = { Text("Reminders") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Settings.route,
                        onClick = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
        ) {
            composable(Screen.PinnedBoard.route) {
                PinnedBoardScreen(navController = navController, innerPadding = padding)
            }
            composable(Screen.BoardList.route) {
                BoardListScreen(navController = navController, innerPadding = padding)
            }
            composable(Screen.Reminders.route) {
                RemindersScreen(navController = navController, innerPadding = padding)
            }
            composable(
                Screen.KanbanBoard.route,
                arguments = listOf(navArgument("boardId") { type = NavType.StringType })
            ) { backStackEntry ->
                KanbanBoardScreen(
                    boardId = backStackEntry.arguments!!.getString("boardId")!!,
                    navController = navController
                )
            }
            composable(
                Screen.TaskDetail.route,
                arguments = listOf(navArgument("taskId") { type = NavType.StringType })
            ) { backStackEntry ->
                TaskDetailScreen(
                    taskId = backStackEntry.arguments!!.getString("taskId")!!,
                    navController = navController
                )
            }
            composable(
                Screen.AddEditOneOffReminder.route,
                arguments = listOf(
                    navArgument("reminderId") {
                        type = NavType.StringType; nullable = true; defaultValue = null
                    },
                    navArgument("fromWidget") {
                        type = NavType.BoolType; defaultValue = false
                    }
                )
            ) { backStackEntry ->
                AddEditOneOffReminderScreen(
                    reminderId = backStackEntry.arguments?.getString("reminderId"),
                    fromWidget = backStackEntry.arguments?.getBoolean("fromWidget") ?: false,
                    navController = navController
                )
            }
            composable(
                Screen.AddEditRecurringReminder.route,
                arguments = listOf(
                    navArgument("reminderId") {
                        type = NavType.StringType; nullable = true; defaultValue = null
                    },
                    navArgument("fromWidget") {
                        type = NavType.BoolType; defaultValue = false
                    }
                )
            ) { backStackEntry ->
                AddEditRecurringReminderScreen(
                    reminderId = backStackEntry.arguments?.getString("reminderId"),
                    fromWidget = backStackEntry.arguments?.getBoolean("fromWidget") ?: false,
                    navController = navController
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    onSignedIn = {
                        navController.navigate(Screen.PinnedBoard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}
