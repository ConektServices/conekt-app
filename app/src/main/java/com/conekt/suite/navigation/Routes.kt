package com.conekt.suite.navigation

object Routes {
    const val PULSE        = "pulse"
    const val VAULT        = "vault"
    const val CANVAS       = "canvas"
    const val MUSIC        = "music"
    const val PROFILE      = "profile"
    const val PHONE_SETUP  = "phone_setup"
    const val EDIT_PROFILE = "edit_profile"
    const val CREATE_POST  = "create_post"

    // ── Chat ──────────────────────────────────────────────────────────────────
    const val CHAT         = "chat"                    // conversations list
    const val CHAT_THREAD  = "chat_thread/{convId}/{otherId}/{name}"  // individual thread
    const val USER_PROFILE = "user_profile/{userId}"   // another user's profile

    fun chatThread(convId: String, otherId: String, name: String) =
        "chat_thread/$convId/$otherId/${name.encodeForRoute()}"

    fun userProfile(userId: String) = "user_profile/$userId"

    private fun String.encodeForRoute() = java.net.URLEncoder.encode(this, "UTF-8")
}