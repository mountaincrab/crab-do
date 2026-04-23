package com.mountaincrab.crabdo.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.google.firebase.auth.FirebaseAuth
import com.mountaincrab.crabdo.data.local.dao.OneOffReminderDao
import com.mountaincrab.crabdo.ui.navigation.ReminderTarget
import org.koin.core.context.GlobalContext

class OneOffRemindersWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val koin = GlobalContext.get()
        val oneOffDao = koin.get<OneOffReminderDao>()
        val userId = koin.get<FirebaseAuth>().currentUser?.uid ?: ""

        val remindersFlow = oneOffDao.observeAllActive(userId)

        provideContent {
            val reminders by remindersFlow.collectAsState(initial = emptyList())
            val now = System.currentTimeMillis()
            val items = reminders.map { r ->
                WidgetReminderItem(
                    id = r.id,
                    title = r.title,
                    nextFireAt = r.snoozedUntilMillis?.takeIf { it > now } ?: r.scheduledAt,
                    snoozedUntilMillis = r.snoozedUntilMillis,
                )
            }.sortedWith(compareBy(
                { it.snoozedUntilMillis != null && it.snoozedUntilMillis > now },
                { it.nextFireAt }
            )).take(6)

            ReminderWidgetBody(
                context = context,
                header = "ONE-OFF REMINDERS",
                reminderType = ReminderTarget.ONE_OFF.name,
                reminders = items,
            )
        }
    }
}
