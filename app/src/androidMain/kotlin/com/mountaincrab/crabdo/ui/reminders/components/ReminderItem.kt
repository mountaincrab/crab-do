package com.mountaincrab.crabdo.ui.reminders.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.mountaincrab.crabdo.data.local.entity.ReminderEntity
import com.mountaincrab.crabdo.data.model.RecurrenceRule
import com.mountaincrab.crabdo.domain.RecurrenceEngine
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReminderItem(
    reminder: ReminderEntity,
    onToggleEnabled: () -> Unit,
    onDelete: (() -> Unit)? = null,
    completed: Boolean = false,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val isSnoozed = reminder.snoozedUntilMillis != null && reminder.snoozedUntilMillis > now

    val recurrenceDesc = reminder.recurrenceRuleJson
        ?.let { runCatching { RecurrenceRule.fromJson(it) }.getOrNull() }
        ?.let { RecurrenceEngine.describe(it) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
        Icon(
            imageVector = when {
                isSnoozed -> Icons.Default.Snooze
                reminder.reminderStyle == ReminderEntity.ReminderStyle.ALARM -> Icons.Default.Notifications
                else -> Icons.Default.Vibration
            },
            contentDescription = null,
            tint = when {
                completed -> mutedColor
                isSnoozed -> MaterialTheme.colorScheme.tertiary
                reminder.isEnabled -> MaterialTheme.colorScheme.primary
                else -> mutedColor
            },
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (completed || !reminder.isEnabled) mutedColor
                        else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(1.dp))
            when {
                isSnoozed -> Text(
                    text = "Snoozing until ${SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(Date(reminder.snoozedUntilMillis!!))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
                recurrenceDesc != null -> Text(
                    text = recurrenceDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> Text(
                    text = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
                        .format(Date(reminder.nextTriggerMillis)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!completed) {
            Switch(
                checked = reminder.isEnabled,
                onCheckedChange = { onToggleEnabled() },
                modifier = Modifier.scale(0.85f)
            )
        }
        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}
