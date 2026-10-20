package com.dermify.dermify

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class User(
    @DocumentId val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val tier: Int = 1,
    @get:PropertyName("isOwner") @set:PropertyName("isOwner")
    var isOwner: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// Tier access codes (academic project — hardcoded for now)
object TierCodes {
    const val SPECIALIST = "SPEC2025"   // entering this → Tier 2
    const val ADMIN = "ADMIN2025"       // entering this → Tier 3
    // no code entered → Tier 1 (Clinician)

    fun tierForCode(code: String): Int = when (code.trim()) {
        SPECIALIST -> 2
        ADMIN -> 3
        else -> 1
    }
}