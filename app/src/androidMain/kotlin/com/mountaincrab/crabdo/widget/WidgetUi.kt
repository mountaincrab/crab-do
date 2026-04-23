package com.mountaincrab.crabdo.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mountaincrab.crabdo.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WidgetReminderItem(
    val id: String,
    val title: String,
    val nextFireAt: Long,
    val snoozedUntilMillis: Long?
)

@Composable
internal fun ReminderWidgetBody(
    context: Context,
    header: String,
    reminderType: String,
    reminders: List<WidgetReminderItem>,
) {
    val bodyBg = ColorProvider(Color(0x991A2A6E))
    val headerBg = ColorProvider(Color(0xFF0D1840))
    val textColor = ColorProvider(Color(0xFFF3F4F6))
    val subtextColor = ColorProvider(Color(0xFF9CA3AF))
    val accentColor = ColorProvider(Color(0xFF4F7CFF))
    val dividerColor = ColorProvider(Color(0x22FFFFFF))

    val addIntent = Intent(context, MainActivity::class.java).apply {
        putExtra("open_add_reminder", true)
        putExtra("reminder_type", reminderType)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    Column(modifier = GlanceModifier.fillMaxSize().background(bodyBg)) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(headerBg)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = header,
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textColor),
                modifier = GlanceModifier.defaultWeight()
            )
            Box(
                modifier = GlanceModifier
                    .background(accentColor)
                    .cornerRadius(6.dp)
                    .padding(horizontal = 9.dp, vertical = 3.dp)
                    .clickable(actionStartActivity(addIntent)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ColorProvider(Color.White))
                )
            }
        }

        if (reminders.isEmpty()) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No reminders", style = TextStyle(fontSize = 12.sp, color = subtextColor))
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(reminders) { reminder ->
                    val editIntent = Intent(context, MainActivity::class.java).apply {
                        putExtra("open_reminder_id", reminder.id)
                        putExtra("reminder_type", reminderType)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    ReminderWidgetRow(
                        reminder = reminder,
                        textColor = textColor,
                        subtextColor = subtextColor,
                        dividerColor = dividerColor,
                        modifier = GlanceModifier.clickable(actionStartActivity(editIntent))
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderWidgetRow(
    reminder: WidgetReminderItem,
    textColor: ColorProvider,
    subtextColor: ColorProvider,
    dividerColor: ColorProvider,
    modifier: GlanceModifier = GlanceModifier
) {
    val now = System.currentTimeMillis()
    val isSnoozed = reminder.snoozedUntilMillis != null && reminder.snoozedUntilMillis > now
    val timeStr = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(reminder.nextFireAt))
    val prefix = if (isSnoozed) "💤 " else ""

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(dividerColor)) {}
        Column(modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text(
                text = reminder.title,
                style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, color = textColor),
                maxLines = 1
            )
            Text(
                text = "$prefix$timeStr",
                style = TextStyle(fontSize = 11.sp, color = subtextColor)
            )
        }
    }
}
