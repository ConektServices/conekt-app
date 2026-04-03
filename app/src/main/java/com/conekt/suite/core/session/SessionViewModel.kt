package com.conekt.suite.core.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conekt.suite.core.supabase.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SessionViewModel : ViewModel() {

    private val supabase = SupabaseProvider.client

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                _uiState.value = when (status) {
                    SessionStatus.Initializing -> {
                        SessionUiState(isLoading = true)
                    }

                    is SessionStatus.Authenticated -> {
                        val userId = supabase.auth.currentSessionOrNull()?.user?.id
                        SessionUiState(
                            isLoading = false,
                            isSignedIn = true,
                            userId = userId
                        )
                    }

                    is SessionStatus.NotAuthenticated,
                    is SessionStatus.RefreshFailure -> {
                        SessionUiState(
                            isLoading = false,
                            isSignedIn = false,
                            userId = null
                        )
                    }
                }
            }
        }
    }
}