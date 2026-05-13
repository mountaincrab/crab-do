package com.mountaincrab.crabdo.ui.reminders.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mountaincrab.crabdo.data.local.entity.OneOffReminderEntity
import com.mountaincrab.crabdo.data.local.entity.RecurringReminderEntity
import com.mountaincrab.crabdo.data.local.entity.ReminderStyle
import com.mountaincrab.crabdo.data.model.RecurrenceRule
import com.mountaincrab.crabdo.domain.RecurrenceEngine
import com.mountaincrab.crabdo.ui.theme.LocalAppPalette
import java.text.SimpleDateFormat
import java.util.*

// Translucent amber background used behind the alarm style indicator —
// matches the design system token rgba(245,158,11,.10).
private val AlarmTileBg = Color(0x1AF59E0B)
private val AlarmTileFg = Color(0xFFFBBF24)

@Composable
fun OneOffReminderItem(
    reminder: OneOffReminderEntity,
    onDelete: (() -> Unit)? = null,
    completed: Boolean = false,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val isSnoozed = reminder.snoozedUntilMillis != null && reminder.snoozedUntilMillis > now
    val palette = LocalAppPalette.current
    val isAlarm = reminder.reminderStyle == ReminderStyle.ALARM

    ReminderRow(
        modifier = modifier,
        leadingIcon = when {
            isSnoozed -> Icons.Default.Snooze
            isAlarm -> Icons.Default.AccessAlarm
            else -> Icons.Default.Notifications
        },
        leadingTileBg = when {
            isAlarm && !completed -> AlarmTileBg
            !completed -> palette.accentSoft
            else -> palette.surfaceHigh
        },
        leadingTileFg = when {
            completed -> MaterialTheme.colorScheme.onSurfaceVariant
            isAlarm -> AlarmTileFg
            else -> palette.accentText
        },
        title = reminder.title,
        titleColor = if (completed) MaterialTheme.colorScheme.onSurfaceVariant
                     else MaterialTheme.colorScheme.onSurface,
        meta = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(reminder.scheduledAt)),
        snoozeText = if (isSnoozed) {
            "Snoozing until " + SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date(reminder.snoozedUntilMillis!!))
        } else null,
        trailing = {
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    )
}

@Composable
fun RecurringReminderItem(
    reminder: RecurringReminderEntity,
    onToggleEnabled: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val isSnoozed = reminder.snoozedUntilMillis != null && reminder.snoozedUntilMillis > now
    val palette = LocalAppPalette.current

    val recurrenceDesc = runCatching {
        RecurrenceEngine.describe(RecurrenceRule.fromJson(reminder.recurrenceRuleJson), reminder.reminderTime)
    }.getOrElse { reminder.reminderTime }

    val nextStr = SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault()).format(Date(reminder.nextFireAt))

    ReminderRow(
        modifier = modifier,
        leadingIcon = null,
        leadingTileBg = palette.surfaceHigh,
        leadingTileFg = MaterialTheme.colorScheme.onSurfaceVariant,
        title = reminder.title,
        titleColor = if (!reminder.isEnabled) MaterialTheme.colorScheme.onSurfaceVariant
                     else MaterialTheme.colorScheme.onSurface,
        meta = recurrenceDesc,
        nextText = if (!isSnoozed) "Next: $nextStr" else null,
        snoozeText = if (isSnoozed) {
            "Snoozing until " + SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date(reminder.snoozedUntilMillis!!))
        } else null,
        trailing = {
            if (onDismiss != null && reminder.isEnabled) {
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("Dismiss", style = MaterialTheme.typography.labelMedium)
                }
            }
            Switch(
                checked = reminder.isEnabled,
                onCheckedChange = { onToggleEnabled() },
                modifier = Modifier.scale(0.85f)
            )
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    )
}

// Shared row layout for one-off + recurring reminders. Card with surface-raised
// background, subtle border, a colored leading tile, a title + meta column,
// and a trailing slot for actions. Matches the .row pattern from the design
// system mobile kit.
@Composable
private fun ReminderRow(
    modifier: Modifier,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector?,
    leadingTileBg: Color,
    leadingTileFg: Color,
    title: String,
    titleColor: Color,
    meta: String,
    nextText: String? = null,
    snoozeText: String? = null,
    trailing: @Composable RowScope.() -> Unit,
) {
    val palette = LocalAppPalette.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(leadingTileBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = leadingTileFg,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = titleColor,
                )
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (nextText != null) {
                    Text(
                        text = nextText,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.accentText,
                        fontWeight = FontWeight.Medium,
                    )
                }
                if (snoozeText != null) {
                    Text(
                        text = snoozeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.successText,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            trailing()
        }
    }
}
