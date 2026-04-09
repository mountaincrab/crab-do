package com.mountaincrab.crabdo.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.crabdo.R
import com.mountaincrab.crabdo.auth.AuthRepository
import com.mountaincrab.crabdo.data.repository.BoardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val boardRepository: BoardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signInWithGoogle(context: Context, onSuccess: () -> Unit) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            val clientId = context.getString(R.string.google_web_client_id)
            val result = authRepository.signInWithGoogle(context, clientId)
            _uiState.value = result.fold(
                onSuccess = {
                    boardRepository.triggerSync()
                    onSuccess()
                    LoginUiState.Idle
                },
                onFailure = { LoginUiState.Error(it.message ?: "Sign-in failed") }
            )
        }
    }

    fun dismissError() {
        _uiState.value = LoginUiState.Idle
    }
}

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Error(val message: String) : LoginUiState
}
