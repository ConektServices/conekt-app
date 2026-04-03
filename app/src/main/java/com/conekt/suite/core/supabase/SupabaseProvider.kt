package com.conekt.suite.core.supabase

import com.conekt.suite.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object SupabaseProvider {

    val client by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Auth) {
                scheme = "conekt"
                host = "auth"
                alwaysAutoRefresh = true
                autoLoadFromStorage = true
            }

            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }
}