package com.dermify.dermify.curation

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A consented contribution waiting to reach Supabase.
 *
 * The row is written at the moment of consent, before any network work, so the
 * consent record survives an upload failure, an app kill, or a phone that never
 * sees WiFi again. Ghana deployment assumes intermittent connectivity; uploads
 * are never fire-and-forget.
 *
 * Nothing here identifies the contributor. [contributionId] is a random UUID —
 * not derived from device ID, user ID or timestamp — and [createdAtDate] is a
 * date with no time component.
 */
@Entity(tableName = "contribution_outbox")
data class ContributionOutbox(
    @PrimaryKey val contributionId: String,   // UUID v4
    val imagePath: String,                    // app-private storage, de-identified JPEG
    val phash: String,
    val predictedClass: String,               // the model's guess, not ground truth
    val confidence: Float,
    val consentVersion: String,
    val appVersion: String,
    val createdAtDate: String,                // yyyy-MM-dd, no time
    val status: String,
    val attemptCount: Int = 0,
    val lastError: String? = null
) {
    companion object {
        const val PENDING = "PENDING"
        const val UPLOADING = "UPLOADING"
        const val DONE = "DONE"
        const val FAILED = "FAILED"
        const val REJECTED_DUPLICATE = "REJECTED_DUPLICATE"

        /** Attempts beyond this stop consuming battery and get surfaced to the user. */
        const val MAX_ATTEMPTS = 5
    }
}
