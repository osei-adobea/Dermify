package com.dermify.dermify

// In-memory holder for the logged-in user's info.
// Lives only while the app process is alive; refilled on each launch.
object Session {
    var uid: String? = null
    var email: String? = null
    var displayName: String? = null
    var tier: Int = 1   // default to most restrictive until loaded
    var isOwner: Boolean = false

    fun clear() {
        uid = null
        email = null
        displayName = null
        tier = 1
        isOwner = false
    }
}