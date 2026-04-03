package com.conekt.suite.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conekt.suite.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // ── Field updates ────────────────────────────────────────────────────────

    fun onModeChange(mode: AuthMode) {
        _uiState.value = _uiState.value.copy(
            mode = mode,
            errorMessage = null
        )
    }

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value.trim(), errorMessage = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
    }

    fun onPasswordVisibilityToggle() {
        _uiState.value = _uiState.value.copy(
            passwordVisible = !_uiState.value.passwordVisible
        )
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = value, errorMessage = null)
    }

    fun onConfirmPasswordVisibilityToggle() {
        _uiState.value = _uiState.value.copy(
            confirmPasswordVisible = !_uiState.value.confirmPasswordVisible
        )
    }

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(
            username = value.trim().lowercase(),
            errorMessage = null
        )
    }

    fun onDisplayNameChange(value: String) {
        _uiState.value = _uiState.value.copy(displayName = value, errorMessage = null)
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    fun submit() {
        when (_uiState.value.mode) {
            AuthMode.SIGN_IN -> signIn()
            AuthMode.SIGN_UP -> signUp()
        }
    }

    private fun signIn() {
        val state = _uiState.value

        val error = when {
            state.email.isBlank() -> "Email is required."
            !state.email.contains("@") -> "Enter a valid email address."
            state.password.isBlank() -> "Password is required."
            else -> null
        }

        if (error != null) {
            _uiState.value = state.copy(errorMessage = error)
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)

            repository.signIn(state.email, state.password)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message?.friendlyAuthError()
                            ?: "Sign-in failed. Please try again."
                    )
                }
        }
    }

    private fun signUp() {
        val state = _uiState.value

        val error = when {
            state.displayName.isBlank() -> "Display name is required."
            state.username.isBlank() -> "Username is required."
            state.username.length < 3 -> "Username must be at least 3 characters."
            !state.username.matches(Regex("^[a-z0-9._]+$")) ->
                "Username can only contain letters, numbers, dots and underscores."
            state.email.isBlank() -> "Email is required."
            !state.email.contains("@") -> "Enter a valid email address."
            state.password.length < 6 -> "Password must be at least 6 characters."
            state.password != state.confirmPassword -> "Passwords do not match."
            else -> null
        }

        if (error != null) {
            _uiState.value = state.copy(errorMessage = error)
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)

            repository.signUp(
                email = state.email,
                password = state.password,
                username = state.username,
                displayName = state.displayName
            ).onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message?.friendlyAuthError()
                        ?: "Sign-up failed. Please try again."
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun String.friendlyAuthError(): String = when {
    contains("Invalid login credentials", ignoreCase = true) ->
        "Incorrect email or password."
    contains("Email not confirmed", ignoreCase = true) ->
        "Please confirm your email before signing in."
    contains("User already registered", ignoreCase = true) ->
        "An account with this email already exists."
    contains("Password should be", ignoreCase = true) ->
        "Password must be at least 6 characters."
    contains("duplicate key", ignoreCase = true) &&
            contains("username", ignoreCase = true) ->
        "That username is already taken."
    contains("network", ignoreCase = true) ||
            contains("Unable to resolve", ignoreCase = true) ->
        "No connection. Check your internet and try again."
    else -> this
}
