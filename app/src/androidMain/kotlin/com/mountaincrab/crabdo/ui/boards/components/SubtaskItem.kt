package com.mountaincrab.crabdo.ui.boards.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.getSelectedText
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mountaincrab.crabdo.data.local.entity.SubtaskEntity
import com.mountaincrab.crabdo.ui.util.AddLinkDialog
import com.mountaincrab.crabdo.ui.util.LinkedText
import com.mountaincrab.crabdo.ui.util.insertMarkdownLink

@Composable
fun SubtaskItem(
    subtask: SubtaskEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRenameDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (subtask.isCompleted) 0.45f else 1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = subtask.isCompleted,
            onCheckedChange = onToggle,
            modifier = Modifier.size(36.dp)
        )
        LinkedText(
            text = subtask.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough
                                 else TextDecoration.None
            ),
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .clickable(enabled = !subtask.isCompleted) { showRenameDialog = true }
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete subtask",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }

    if (showRenameDialog) {
        var newTitle by remember { mutableStateOf(TextFieldValue(subtask.title)) }
        var showLinkDialog by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        val commit = {
            if (newTitle.text.isNotBlank()) {
                onRename(newTitle.text.trim())
                showRenameDialog = false
            }
        }

        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename subtask") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showLinkDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Add link",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    keyboardActions = KeyboardActions(onDone = { commit() })
                )
            },
            confirmButton = {
                TextButton(onClick = commit, enabled = newTitle.text.isNotBlank()) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )

        if (showLinkDialog) {
            AddLinkDialog(
                initialLabel = newTitle.getSelectedText().text,
                onDismiss = { showLinkDialog = false },
                onConfirm = { label, url ->
                    newTitle = insertMarkdownLink(newTitle, label, url)
                    showLinkDialog = false
                },
            )
        }
    }
}
