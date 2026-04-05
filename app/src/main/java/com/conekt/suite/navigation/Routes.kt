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

    // Chat
    const val CHAT        = "chat"
    const val CHAT_THREAD = "chat_thread/{convId}/{otherId}/{name}"
    const val USER_PROFILE = "user_profile/{userId}"

    fun chatThread(convId: String, otherId: String, name: String): String =
        "chat_thread/${enc(convId)}/${enc(otherId)}/${enc(name)}"

    fun userProfile(userId: String): String = "user_profile/${enc(userId)}"

    private fun enc(s: String): String =
        java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")
}