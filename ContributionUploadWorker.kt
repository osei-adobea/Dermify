package com.dermify.dermify.curation

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Drains the contribution outbox off the hot path.
 *
 * Returning [Result.retry] hands the schedule back to WorkManager, which
 * applies the exponential backoff configured in [CurationSync]. Per-row attempt
 * counting lives in the repository so a row that keeps failing is eventually
 * marked FAILED and surfaced to the user instead of retrying forever.
 */
class ContributionUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val stillPending = ContributionRepository(applicationContext).drain()
            if (stillPending) Result.retry() else Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Outbox drain failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "ContributionWorker"
    }
}
