package com.mountaincrab.crabdo.ui.boards.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mountaincrab.crabdo.data.local.entity.SubtaskEntity

@Composable
fun SubtaskItem(
    subtask: SubtaskEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = subtask.isCompleted,
            onCheckedChange = onToggle
        )
        Text(
            text = subtask.title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(start = 8.dp)
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete subtask",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
