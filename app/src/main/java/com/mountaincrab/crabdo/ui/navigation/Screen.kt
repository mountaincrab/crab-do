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
    object AddEditReminder : Screen("reminder/edit?reminderId={reminderId}") {
        fun createRoute(reminderId: String? = null) =
            if (reminderId != null) "reminder/edit?reminderId=$reminderId"
            else "reminder/edit"
    }
    object Settings : Screen("settings")
}
