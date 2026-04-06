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
    const val CHAT         = "chat"

    // Args are passed as query params to avoid URL encoding issues with names
    const val CHAT_THREAD  = "chat_thread?convId={convId}&otherId={otherId}&name={name}&avatar={avatar}"
    const val USER_PROFILE = "user_profile/{userId}"

    fun chatThread(convId: String, otherId: String, name: String, avatar: String = "") =
        "chat_thread?convId=${enc(convId)}&otherId=${enc(otherId)}&name=${enc(name)}&avatar=${enc(avatar)}"

    fun userProfile(userId: String) = "user_profile/${enc(userId)}"

    private fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")
}