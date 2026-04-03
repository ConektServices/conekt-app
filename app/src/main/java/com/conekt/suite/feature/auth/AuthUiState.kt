package com.conekt.suite.feature.auth

enum class AuthMode { SIGN_IN, SIGN_UP }

data class AuthUiState(
    val mode: AuthMode = AuthMode.SIGN_IN,

    // shared fields
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,

    // sign-up only
    val confirmPassword: String = "",
    val confirmPasswordVisible: Boolean = false,
    val username: String = "",
    val displayName: String = "",

    // async state
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)
