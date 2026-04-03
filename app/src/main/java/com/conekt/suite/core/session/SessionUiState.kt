package com.conekt.suite.core.session

data class SessionUiState(
    val isLoading: Boolean = true,
    val isSignedIn: Boolean = false,
    val userId: String? = null
)