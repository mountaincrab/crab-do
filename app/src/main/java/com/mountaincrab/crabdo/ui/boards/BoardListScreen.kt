package com.mountaincrab.crabdo.ui.boards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mountaincrab.crabdo.data.local.entity.BoardEntity
import com.mountaincrab.crabdo.data.model.Invitation
import com.mountaincrab.crabdo.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardListScreen(
    navController: NavController,
    innerPadding: PaddingValues = PaddingValues(),
    viewModel: BoardListViewModel = hiltViewModel()
) {
    val boards by viewModel.boards.collectAsStateWithLifecycle()
    val pinnedBoardId by viewModel.pinnedBoardId.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val pendingInvitations by viewModel.pendingInvitations.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newBoardTitle by remember { mutableStateOf("") }
    var shareBoardTarget by remember { mutableStateOf<BoardEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Boards") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create board")
            }
        }
    ) { scaffoldPadding ->
        PullToRefreshBox(
            isRefreshing = isSyncing,
            onRefresh = { viewModel.sync() },
            modifier = Modifier.fillMaxSize().padding(scaffoldPadding)
        ) {
            if (boards.isEmpty() && pendingInvitations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No boards yet",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Tap + to create your first board",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (pendingInvitations.isNotEmpty()) {
                        item(key = "invitations_header") {
                            Text(
                                text = "Invitations",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(pendingInvitations, key = { "inv_${it.id}" }) { invitation ->
                            InvitationRow(
                                invitation = invitation,
                                onAccept = { viewModel.acceptInvitation(invitation) },
                                onDecline = { viewModel.declineInvitation(invitation) }
                            )
                        }
                        item(key = "invitations_divider") {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                    items(boards, key = { it.id }) { board ->
                        BoardRow(
                            board = board,
                            isPinned = board.id == pinnedBoardId,
                            onTap = { navController.navigate(Screen.KanbanBoard.createRoute(board.id)) },
                            onPin = {
                                if (board.id == pinnedBoardId) viewModel.unpinBoard()
                                else viewModel.pinBoard(board.id)
                            },
                            onDelete = { viewModel.deleteBoard(board.id) },
                            onRename = { viewModel.renameBoard(board, it) },
                            onShare = { shareBoardTarget = board }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false; newBoardTitle = "" },
            title = { Text("New Board") },
            text = {
                OutlinedTextField(
                    value = newBoardTitle,
                    onValueChange = { newBoardTitle = it },
                    label = { Text("Board name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newBoardTitle.isNotBlank()) {
                            viewModel.createBoard(newBoardTitle.trim())
                        }
                        showCreateDialog = false
                        newBoardTitle = ""
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false; newBoardTitle = "" }) {
                    Text("Cancel")
                }
            }
        )
    }

    shareBoardTarget?.let { board ->
        ShareBoardDialog(
            boardTitle = board.title,
            onShare = { email ->
                viewModel.shareBoard(board.id, board.title, email)
                shareBoardTarget = null
            },
            onDismiss = { shareBoardTarget = null }
        )
    }
}

@Composable
private fun BoardRow(
    board: BoardEntity,
    isPinned: Boolean,
    onTap: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
    onShare: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(board.title) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Tag,
            contentDescription = null,
            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = board.title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (board.isShared) {
            Icon(
                imageVector = Icons.Filled.People,
                contentDescription = "Shared",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
        }
        if (isPinned) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Pinned",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text(if (isPinned) "Unpin" else "Pin") },
                    onClick = { showMenu = false; onPin() }
                )
                DropdownMenuItem(
                    text = { Text("Share") },
                    onClick = { showMenu = false; onShare() }
                )
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = {
                        showMenu = false
                        renameText = board.title
                        showRenameDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = { showMenu = false; onDelete() }
                )
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Board") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameText.isNotBlank()) onRename(renameText.trim())
                    showRenameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun InvitationRow(
    invitation: Invitation,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = invitation.boardTitle,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "from ${invitation.ownerDisplayName.ifEmpty { "someone" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onAccept, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Accept",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(onClick = onDecline, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Decline",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShareBoardDialog(
    boardTitle: String,
    onShare: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share \"$boardTitle\"") },
        text = {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email address") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (email.isNotBlank()) onShare(email.trim()) },
                enabled = email.isNotBlank()
            ) { Text("Invite") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
