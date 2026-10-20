package com.dermify.dermify.curation

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Scheduling policy for contribution uploads. */
object CurationSync {

    private const val ONE_SHOT_WORK = "dermify-contribution-upload"
    private const val PERIODIC_WORK = "dermify-contribution-upload-periodic"

    private const val PREFS = "dermify_curation"
    private const val KEY_ALLOW_METERED = "allow_metered_upload"

    /**
     * WiFi by default. Mobile data in Ghana is bought in bundles, so uploading
     * clinical photographs over it without asking is not a decision the app
     * gets to make — the user opts in from Settings.
     */
    fun isMeteredUploadAllowed(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ALLOW_METERED, false)

    fun setMeteredUploadAllowed(context: Context, allowed: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ALLOW_METERED, allowed).apply()
        // Constraints are baked into an enqueued request, so re-enqueue to pick
        // the new one up rather than leaving work pinned to the old constraint.
        requestUpload(context, replace = true)
        schedulePeriodicSync(context, replace = true)
    }

    /** Queue a drain now, subject to the network constraint. */
    fun requestUpload(context: Context, replace: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<ContributionUploadWorker>()
            .setConstraints(constraints(context))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_SHOT_WORK,
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    /**
     * Safety net for a phone that was offline when everything else gave up.
     * Also the reason an outbox left over from a previous install-run drains
     * without the user doing anything.
     */
    fun schedulePeriodicSync(context: Context, replace: Boolean = false) {
        val request = PeriodicWorkRequestBuilder<ContributionUploadWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints(context))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun constraints(context: Context): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(
                if (isMeteredUploadAllowed(context)) NetworkType.CONNECTED else NetworkType.UNMETERED
            )
            .build()
}
