package com.mountaincrab.crabdo.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.crabdo.auth.AuthRepository
import com.mountaincrab.crabdo.data.local.entity.ReminderEntity
import com.mountaincrab.crabdo.data.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val userId = authRepository.currentUserId ?: ""

    val reminders: StateFlow<List<ReminderEntity>> =
        reminderRepository.observeReminders(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch { reminderRepository.deleteReminder(reminderId) }
    }

    fun toggleEnabled(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderRepository.updateReminder(reminder.copy(isEnabled = !reminder.isEnabled))
        }
    }
}
