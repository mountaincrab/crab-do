package com.mountaincrab.crabdo.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val relevantActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED
        )
        if (intent.action !in relevantActions) return

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext, ReceiverEntryPoint::class.java)
            entryPoint.reminderRepository().rescheduleAllReminders()
            entryPoint.taskRepository().rescheduleAllTaskReminders()
        }
    }
}
