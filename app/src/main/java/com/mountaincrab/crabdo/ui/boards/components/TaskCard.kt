package com.mountaincrab.crabdo.ui.boards.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mountaincrab.crabdo.data.local.entity.TaskEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskCard(
    task: TaskEntity,
    subtaskCount: Int = 0,
    completedSubtaskCount: Int = 0,
    isDragging: Boolean = false,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    Card(
        onClick = onTap,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 2.dp
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (subtaskCount > 0 || task.reminderTimeMillis != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (subtaskCount > 0) {
                        Text(
                            text = "$completedSubtaskCount/$subtaskCount ✓",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    task.reminderTimeMillis?.let { millis ->
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Reminder",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(millis)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
