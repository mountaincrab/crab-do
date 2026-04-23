package com.mountaincrab.crabdo.ui.navigation

sealed class Screen(val route: String) {
    object PinnedBoard : Screen("pinned_board")
    object BoardList : Screen("board_list")
    object Reminders : Screen("reminders")

    object KanbanBoard : Screen("board/{boardId}") {
        fun createRoute(boardId: String) = "board/$boardId"
    }
    object TaskDetail : Screen("task/{taskId}") {
        fun createRoute(taskId: String) = "task/$taskId"
    }
    object AddEditOneOffReminder : Screen("oneoff_reminder/edit?reminderId={reminderId}&fromWidget={fromWidget}") {
        fun createRoute(reminderId: String? = null, fromWidget: Boolean = false): String {
            val params = mutableListOf<String>()
            if (reminderId != null) params.add("reminderId=$reminderId")
            if (fromWidget) params.add("fromWidget=true")
            return if (params.isEmpty()) "oneoff_reminder/edit" else "oneoff_reminder/edit?${params.joinToString("&")}"
        }
    }
    object AddEditRecurringReminder : Screen("recurring_reminder/edit?reminderId={reminderId}&fromWidget={fromWidget}") {
        fun createRoute(reminderId: String? = null, fromWidget: Boolean = false): String {
            val params = mutableListOf<String>()
            if (reminderId != null) params.add("reminderId=$reminderId")
            if (fromWidget) params.add("fromWidget=true")
            return if (params.isEmpty()) "recurring_reminder/edit" else "recurring_reminder/edit?${params.joinToString("&")}"
        }
    }
    object Settings : Screen("settings")
    object Login : Screen("login")
}
