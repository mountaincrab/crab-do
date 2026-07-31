package com.mountaincrab.crabdo.ui.boards.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.getSelectedText
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mountaincrab.crabdo.data.local.entity.SubtaskEntity
import com.mountaincrab.crabdo.ui.util.AddLinkDialog
import com.mountaincrab.crabdo.ui.util.LinkedText
import com.mountaincrab.crabdo.ui.util.insertMarkdownLink

/**
 * Vertical gap between checklist rows. Shared by every checklist so the
 * drag-reorder slot maths (`itemHeight + spacing`) matches what is drawn.
 */
val SubtaskRowSpacing = 2.dp

/** Height of the row's tap targets, and so of a single-line checklist row. */
private val ControlSize = 30.dp

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
            .heightIn(min = ControlSize)
            .alpha(if (subtask.isCompleted) 0.45f else 1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Material's Checkbox/IconButton each reserve a 48dp minimum interactive
        // box that a size() modifier can't shrink (the minimum-touch-target
        // modifier takes the max of the two), which made a one-line checklist row
        // ~48dp tall. Drawing the box/cross as icons inside our own 30dp tap
        // targets gives the same affordances at a little over half the height.
        Box(
            modifier = Modifier
                .size(ControlSize)
                .clip(CircleShape)
                .toggleable(
                    value = subtask.isCompleted,
                    role = Role.Checkbox,
                    onValueChange = onToggle
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (subtask.isCompleted) Icons.Default.CheckBox
                              else Icons.Default.CheckBoxOutlineBlank,
                contentDescription = if (subtask.isCompleted) "Mark subtask incomplete"
                                    else "Mark subtask complete",
                tint = if (subtask.isCompleted) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        LinkedText(
            text = subtask.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 18.sp,
                textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough
                                 else TextDecoration.None
            ),
            modifier = Modifier
                .weight(1f)
                .padding(start = 2.dp, top = 4.dp, bottom = 4.dp)
                .clickable(enabled = !subtask.isCompleted) { showRenameDialog = true }
        )
        Box(
            modifier = Modifier
                .size(ControlSize)
                .clip(CircleShape)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete subtask",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
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
