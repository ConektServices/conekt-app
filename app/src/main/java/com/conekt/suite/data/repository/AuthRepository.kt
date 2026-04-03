package com.conekt.suite.data.repository

import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.conekt.suite.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth

class AuthRepository {

    private val supabase = SupabaseProvider.client

    suspend fun signUp(
        email: String,
        password: String,
        username: String,
        displayName: String
    ): Result<Unit> = runCatching {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("username", username)
                put("display_name", displayName)
            }
        }
        Unit
    }

    suspend fun signIn(
        email: String,
        password: String
    ): Result<Unit> = runCatching {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        Unit
    }

    suspend fun signOut(): Result<Unit> = runCatching {
        supabase.auth.signOut()
        Unit
    }

    fun currentUserId(): String? {
        return supabase.auth.currentSessionOrNull()?.user?.id
    }

    fun isSignedIn(): Boolean = currentUserId() != null
}