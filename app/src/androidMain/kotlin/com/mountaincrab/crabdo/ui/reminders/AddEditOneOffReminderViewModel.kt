package com.mountaincrab.crabdo.ui.reminders

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.crabdo.auth.AuthRepository
import com.mountaincrab.crabdo.data.local.entity.ReminderStyle
import com.mountaincrab.crabdo.data.repository.ReminderRepository
import com.mountaincrab.crabdo.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AddEditOneOffReminderViewModel(
    val existingReminderId: String?,
    private val reminderRepository: ReminderRepository,
    private val authRepository: AuthRepository,
    private val userPrefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val userId = authRepository.currentUserId ?: ""

    var title by mutableStateOf("")
    var selectedDateTime by mutableStateOf(System.currentTimeMillis() + 3_600_000L)
    var selectedStyle by mutableStateOf(ReminderStyle.ALARM)
    var isTimeInputKeyboard by mutableStateOf(false)

    init {
        viewModelScope.launch {
            isTimeInputKeyboard = userPrefsRepository.timeInputKeyboard.first()
        }
        if (existingReminderId != null) {
            viewModelScope.launch {
                val oneOff = reminderRepository.getOneOffById(existingReminderId)
                if (oneOff != null) {
                    title = oneOff.title
                    selectedDateTime = oneOff.scheduledAt
                    selectedStyle = oneOff.reminderStyle
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
            reminderRepository.deleteOneOff(id)
            onSuccess()
        }
    }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (existingReminderId == null) {
                reminderRepository.createOneOff(userId, title, selectedDateTime, selectedStyle)
            } else {
                val existing = reminderRepository.getOneOffById(existingReminderId) ?: return@launch
                reminderRepository.updateOneOff(existing.copy(
                    title = title,
                    scheduledAt = selectedDateTime,
                    reminderStyle = selectedStyle
                ))
            }
            onSuccess()
        }
    }
}
