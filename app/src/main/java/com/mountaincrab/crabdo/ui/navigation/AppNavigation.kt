package com.mountaincrab.crabdo.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mountaincrab.crabdo.ui.boards.*
import com.mountaincrab.crabdo.ui.reminders.AddEditReminderScreen
import com.mountaincrab.crabdo.ui.reminders.RemindersScreen
import com.mountaincrab.crabdo.ui.settings.SettingsScreen

@Composable
fun AppNavigation(navController: NavHostController, startDestination: String) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == Screen.PinnedBoard.route,
                    onClick = {
                        navController.navigate(Screen.PinnedBoard.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Pinned Board") },
                    label = { Text("Board") }
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
                Screen.AddEditReminder.route,
                arguments = listOf(navArgument("reminderId") {
                    type = NavType.StringType; nullable = true; defaultValue = null
                })
            ) { backStackEntry ->
                AddEditReminderScreen(
                    reminderId = backStackEntry.arguments?.getString("reminderId"),
                    navController = navController
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }
        }
    }
}
