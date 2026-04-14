package com.mountaincrab.crabdo.ui.reminders

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.crabdo.auth.AuthRepository
import com.mountaincrab.crabdo.data.local.entity.ReminderEntity
import com.mountaincrab.crabdo.data.model.RecurrenceRule
import com.mountaincrab.crabdo.data.repository.ReminderRepository
import com.mountaincrab.crabdo.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AddEditReminderViewModel(
    val existingReminderId: String?,
    private val reminderRepository: ReminderRepository,
    private val authRepository: AuthRepository,
    private val userPrefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val userId = authRepository.currentUserId ?: ""

    var title by mutableStateOf("")
    var selectedDateTime by mutableStateOf(System.currentTimeMillis() + 3_600_000L)
    var selectedStyle by mutableStateOf(ReminderEntity.ReminderStyle.ALARM)
    var recurrenceRule by mutableStateOf<RecurrenceRule?>(null)
    var isRecurring by mutableStateOf(false)
    var isTimeInputKeyboard by mutableStateOf(false)

    init {
        viewModelScope.launch {
            isTimeInputKeyboard = userPrefsRepository.timeInputKeyboard.first()
        }
        if (existingReminderId != null) {
            viewModelScope.launch {
                val reminder = reminderRepository.getReminderById(existingReminderId) ?: return@launch
                title = reminder.title
                selectedDateTime = reminder.nextTriggerMillis
                selectedStyle = reminder.reminderStyle
                recurrenceRule = reminder.recurrenceRuleJson?.let { RecurrenceRule.fromJson(it) }
                isRecurring = recurrenceRule != null
            }
        }
    }

    fun updateTimeInputKeyboard(value: Boolean) {
        isTimeInputKeyboard = value
        viewModelScope.launch { userPrefsRepository.setTimeInputKeyboard(value) }
    }

    fun delete(onSuccess: () -> Unit) {
        val id = existingReminderId ?: return
        viewModelScope.launch {
            reminderRepository.deleteReminder(id)
            onSuccess()
        }
    }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (existingReminderId == null) {
                reminderRepository.createReminder(
                    userId = userId,
                    title = title,
                    triggerMillis = selectedDateTime,
                    style = selectedStyle,
                    recurrenceRule = if (isRecurring) recurrenceRule else null
                )
            } else {
                val existing = reminderRepository.getReminderById(existingReminderId) ?: return@launch
                reminderRepository.updateReminder(existing.copy(
                    title = title,
                    nextTriggerMillis = selectedDateTime,
                    reminderStyle = selectedStyle,
                    recurrenceRuleJson = if (isRecurring) recurrenceRule?.toJson() else null
                ))
            }
            onSuccess()
        }
    }
}
