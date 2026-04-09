package com.mountaincrab.crabdo.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class RemindersWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = RemindersWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        refreshWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            refreshWidgets(context)
        }
    }

    companion object {
        fun refreshWidgets(context: Context) {
            MainScope().launch {
                // updateAll() re-invokes provideGlance() for every placed instance of this
                // widget, which re-reads Room and re-renders. Using update(context, id)
                // individually doesn't reliably re-run provideGlance when the backing data
                // lives outside Glance's own state system.
                RemindersWidget().updateAll(context)
            }
        }
    }
}
