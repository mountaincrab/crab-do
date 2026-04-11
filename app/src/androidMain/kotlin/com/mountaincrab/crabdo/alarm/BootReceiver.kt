package com.mountaincrab.crabdo.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mountaincrab.crabdo.data.repository.ReminderRepository
import com.mountaincrab.crabdo.data.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class BootReceiver : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context, intent: Intent) {
        val relevantActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED
        )
        if (intent.action !in relevantActions) return

        val reminderRepository: ReminderRepository = get()
        val taskRepository: TaskRepository = get()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            reminderRepository.rescheduleAllReminders()
            taskRepository.rescheduleAllTaskReminders()
        }
    }
}
