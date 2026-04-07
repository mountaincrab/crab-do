package com.mountaincrab.crabdo.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.mountaincrab.crabdo.MainActivity
import com.mountaincrab.crabdo.data.local.dao.ReminderDao
import com.mountaincrab.crabdo.data.local.entity.ReminderEntity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.text.SimpleDateFormat
import java.util.*

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun reminderDao(): ReminderDao
}

class RemindersWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = EntryPointAccessors.fromApplication(
            context.applicationContext, WidgetEntryPoint::class.java
        ).reminderDao()

        val isNight = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val now = System.currentTimeMillis()
        val reminders = dao.getAllActiveReminders()
            .filter { !it.isDeleted && it.isEnabled }
            .sortedWith(compareBy(
                { it.snoozedUntilMillis != null && it.snoozedUntilMillis > now },
                { it.nextTriggerMillis }
            ))
            .take(6)

        provideContent {
            WidgetContent(context, reminders, isNight)
        }
    }
}

@Composable
private fun WidgetContent(context: Context, reminders: List<ReminderEntity>, isDark: Boolean) {
    val bgColor = ColorProvider(Color(0xFF0A1020))
    val textColor = ColorProvider(Color(0xFFF3F4F6))
    val subtextColor = ColorProvider(Color(0xFF9CA3AF))
    val accentColor = ColorProvider(Color(0xFF4F7CFF))

    val addIntent = Intent(context, MainActivity::class.java).apply {
        putExtra("open_add_reminder", true)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .padding(8.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "REMINDERS",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = textColor
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = "+",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = accentColor
                ),
                modifier = GlanceModifier
                    .clickable(actionStartActivity(addIntent))
                    .padding(horizontal = 4.dp)
            )
        }

        if (reminders.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No upcoming reminders",
                    style = TextStyle(fontSize = 12.sp, color = subtextColor)
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(reminders) { reminder ->
                    val editIntent = Intent(context, MainActivity::class.java).apply {
                        putExtra("open_reminder_id", reminder.id)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    ReminderWidgetRow(
                        reminder = reminder,
                        textColor = textColor,
                        subtextColor = subtextColor,
                        modifier = GlanceModifier.clickable(actionStartActivity(editIntent))
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderWidgetRow(
    reminder: ReminderEntity,
    textColor: ColorProvider,
    subtextColor: ColorProvider,
    modifier: GlanceModifier = GlanceModifier
) {
    val now = System.currentTimeMillis()
    val isSnoozed = reminder.snoozedUntilMillis != null && reminder.snoozedUntilMillis > now
    val timeMillis = if (isSnoozed) reminder.snoozedUntilMillis!! else reminder.nextTriggerMillis
    val timeStr = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(timeMillis))
    val prefix = if (isSnoozed) "💤 " else ""

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            text = reminder.title,
            style = TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = textColor
            ),
            maxLines = 1
        )
        Text(
            text = "$prefix$timeStr",
            style = TextStyle(
                fontSize = 11.sp,
                color = subtextColor
            )
        )
    }
}
