package com.mountaincrab.crabdo.ui.reminders.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    modifier: Modifier = Modifier
) {
    val recurrenceDesc = reminder.recurrenceRuleJson
        ?.let { runCatching { RecurrenceRule.fromJson(it) }.getOrNull() }
        ?.let { RecurrenceEngine.describe(it) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (reminder.reminderStyle == ReminderEntity.ReminderStyle.ALARM)
                Icons.Default.Notifications else Icons.Default.Vibration,
            contentDescription = null,
            tint = if (reminder.isEnabled) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (reminder.isEnabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            if (recurrenceDesc != null) {
                Text(
                    text = recurrenceDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
                        .format(Date(reminder.nextTriggerMillis)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = reminder.isEnabled,
            onCheckedChange = { onToggleEnabled() }
        )
    }
}
