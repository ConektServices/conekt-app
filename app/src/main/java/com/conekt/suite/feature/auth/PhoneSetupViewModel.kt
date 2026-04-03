package com.conekt.suite.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conekt.suite.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PhoneSetupViewModel(
    private val repository: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhoneSetupUiState())
    val uiState: StateFlow<PhoneSetupUiState> = _uiState.asStateFlow()

    fun onPhoneChange(value: String) {
        // Keep only digits, +, spaces, hyphens, parentheses
        val filtered = value.filter { it.isDigit() || it in "+() -" }
        _uiState.value = _uiState.value.copy(phone = filtered, errorMessage = null)
    }

    fun save() {
        val phone = _uiState.value.phone.trim()

        if (phone.isBlank()) {
            skip()
            return
        }

        // Basic length check: international numbers are 7–15 digits
        val digits = phone.filter { it.isDigit() }
        if (digits.length < 7 || digits.length > 15) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Enter a valid phone number."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { repository.updatePhone(phone) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false, isDone = true)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to save phone number."
                    )
                }
        }
    }

    fun skip() {
        _uiState.value = _uiState.value.copy(isDone = true)
    }
}
