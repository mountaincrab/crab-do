package com.mountaincrab.crabdo.ui.boards.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mountaincrab.crabdo.data.local.entity.SubtaskEntity

@Composable
fun SubtaskItem(
    subtask: SubtaskEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    dragHandleModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (subtask.isCompleted) 0.45f else 1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Drag to reorder",
            modifier = dragHandleModifier
                .padding(start = 4.dp)
                .size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Checkbox(
            checked = subtask.isCompleted,
            onCheckedChange = onToggle,
            modifier = Modifier.size(36.dp)
        )
        Text(
            text = subtask.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough
                                 else TextDecoration.None
            ),
            modifier = Modifier.weight(1f).padding(start = 4.dp)
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
}
