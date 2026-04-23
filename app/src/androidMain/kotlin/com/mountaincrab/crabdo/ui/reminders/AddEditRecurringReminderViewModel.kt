package com.mountaincrab.crabdo.ui.reminders

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.crabdo.auth.AuthRepository
import com.mountaincrab.crabdo.data.local.entity.ReminderStyle
import com.mountaincrab.crabdo.data.model.RecurrenceRule
import com.mountaincrab.crabdo.data.repository.ReminderRepository
import com.mountaincrab.crabdo.data.repository.midnightOf
import com.mountaincrab.crabdo.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class AddEditRecurringReminderViewModel(
    val existingReminderId: String?,
    private val reminderRepository: ReminderRepository,
    private val authRepository: AuthRepository,
    private val userPrefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val userId = authRepository.currentUserId ?: ""

    var title by mutableStateOf("")
    var selectedDateTime by mutableStateOf(System.currentTimeMillis() + 3_600_000L)
    var selectedStyle by mutableStateOf(ReminderStyle.ALARM)
    var recurrenceRule by mutableStateOf<RecurrenceRule?>(RecurrenceRule.daily())
    var isTimeInputKeyboard by mutableStateOf(false)

    init {
        viewModelScope.launch {
            isTimeInputKeyboard = userPrefsRepository.timeInputKeyboard.first()
        }
        if (existingReminderId != null) {
            viewModelScope.launch {
                val recurring = reminderRepository.getRecurringById(existingReminderId)
                if (recurring != null) {
                    title = recurring.title
                    selectedStyle = recurring.reminderStyle
                    recurrenceRule = runCatching { RecurrenceRule.fromJson(recurring.recurrenceRuleJson) }.getOrNull()
                        ?: RecurrenceRule.daily()
                    val parts = recurring.reminderTime.split(":")
                    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
                    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    selectedDateTime = Calendar.getInstance().apply {
                        timeInMillis = recurring.startDate
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                    }.timeInMillis
                }
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
            reminderRepository.deleteRecurring(id)
            onSuccess()
        }
    }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val rule = recurrenceRule ?: return@launch
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDateTime }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            val reminderTime = String.format("%02d:%02d", hour, minute)
            val startDate = midnightOf(selectedDateTime)

            if (existingReminderId == null) {
                reminderRepository.createRecurring(userId, title, rule, startDate, reminderTime, selectedStyle)
            } else {
                val existing = reminderRepository.getRecurringById(existingReminderId) ?: return@launch
                reminderRepository.updateRecurring(existing.copy(
                    title = title,
                    recurrenceRuleJson = rule.toJson(),
                    startDate = startDate,
                    reminderTime = reminderTime,
                    reminderStyle = selectedStyle
                ))
            }
            onSuccess()
        }
    }
}
