package com.conekt.suite.feature.auth

data class PhoneSetupUiState(
    val phone: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isDone: Boolean = false       // true on save OR skip
)
