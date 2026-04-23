package com.mountaincrab.crabdo.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.google.firebase.auth.FirebaseAuth
import com.mountaincrab.crabdo.data.local.dao.RecurringReminderDao
import com.mountaincrab.crabdo.ui.navigation.ReminderTarget
import org.koin.core.context.GlobalContext

class RecurringRemindersWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val koin = GlobalContext.get()
        val recurringDao = koin.get<RecurringReminderDao>()
        val userId = koin.get<FirebaseAuth>().currentUser?.uid ?: ""

        val remindersFlow = recurringDao.observeAllActive(userId)

        provideContent {
            val reminders by remindersFlow.collectAsState(initial = emptyList())
            val now = System.currentTimeMillis()
            val items = reminders.map { r ->
                WidgetReminderItem(
                    id = r.id,
                    title = r.title,
                    nextFireAt = r.snoozedUntilMillis?.takeIf { it > now } ?: r.nextFireAt,
                    snoozedUntilMillis = r.snoozedUntilMillis,
                )
            }.sortedWith(compareBy(
                { it.snoozedUntilMillis != null && it.snoozedUntilMillis > now },
                { it.nextFireAt }
            )).take(6)

            ReminderWidgetBody(
                context = context,
                header = "RECURRING REMINDERS",
                reminderType = ReminderTarget.RECURRING.name,
                reminders = items,
            )
        }
    }
}
