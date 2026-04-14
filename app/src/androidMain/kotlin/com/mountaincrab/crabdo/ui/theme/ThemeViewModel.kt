package com.mountaincrab.crabdo.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.crabdo.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(
    private val prefs: UserPreferencesRepository
) : ViewModel() {

    val appTheme: StateFlow<AppTheme> = prefs.appTheme
        .map { AppTheme.fromName(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppTheme.DEEP_NAVY)

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { prefs.setAppTheme(theme.name) }
    }
}
