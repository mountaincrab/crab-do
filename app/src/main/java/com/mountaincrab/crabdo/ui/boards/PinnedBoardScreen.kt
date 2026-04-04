package com.mountaincrab.crabdo.ui.boards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mountaincrab.crabdo.ui.navigation.Screen

@Composable
fun PinnedBoardScreen(
    navController: NavController,
    innerPadding: PaddingValues = PaddingValues(),
    viewModel: BoardListViewModel = hiltViewModel()
) {
    val pinnedBoardId by viewModel.pinnedBoardId.collectAsStateWithLifecycle()
    val boards by viewModel.boards.collectAsStateWithLifecycle()

    // Verify the pinned board still exists
    val boardExists = pinnedBoardId != null && boards.any { it.id == pinnedBoardId }

    if (boardExists && pinnedBoardId != null) {
        KanbanBoardScreen(
            boardId = pinnedBoardId!!,
            navController = navController
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No board pinned",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Go to Boards and tap ⭐ to pin one",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { navController.navigate(Screen.BoardList.route) }) {
                    Text("Go to Boards")
                }
            }
        }
    }
}
