package com.dermify.dermify.curation

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.dermify.dermify.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Everything the app does with a contribution before and after the network.
 *
 * The consent screen calls [enqueue] and returns immediately. It never blocks
 * on the network, and it never touches Supabase directly.
 */
class ContributionRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).contributionOutboxDao()

    sealed class EnqueueResult {
        object Queued : EnqueueResult()
        data class Rejected(val userMessage: String) : EnqueueResult()
        data class Failed(val userMessage: String) : EnqueueResult()
    }

    /**
     * De-identifies the capture, writes it to app-private storage, records the
     * consent, and asks WorkManager to upload it when the network allows.
     *
     * The Room row is written before any upload is attempted, so the consent
     * record exists even if the upload never succeeds.
     */
    suspend fun enqueue(
        sourceImagePath: String,
        predictedClass: String,
        confidence: Float
    ): EnqueueResult = withContext(Dispatchers.IO) {
        when (val deid = Deidentifier.deidentify(sourceImagePath)) {
            is Deidentifier.Result.Rejected ->
                EnqueueResult.Rejected(deid.userMessage)

            is Deidentifier.Result.Failed ->
                EnqueueResult.Failed(deid.message)

            is Deidentifier.Result.Success -> {
                val contributionId = UUID.randomUUID().toString()
                val destination = File(contributionsDir(), "$contributionId.jpg")

                try {
                    destination.writeBytes(deid.jpeg)
                } catch (e: Exception) {
                    Log.e(TAG, "Could not stage contribution $contributionId", e)
                    return@withContext EnqueueResult.Failed("Could not save the image locally")
                }

                val row = ContributionOutbox(
                    contributionId = contributionId,
                    imagePath = destination.absolutePath,
                    phash = deid.phash,
                    predictedClass = predictedClass,
                    confidence = confidence,
                    consentVersion = ConsentPolicy.CURRENT_VERSION,
                    appVersion = appVersion(),
                    createdAtDate = today(),
                    status = ContributionOutbox.PENDING
                )

                try {
                    dao.insert(row)
                } catch (e: Exception) {
                    destination.delete()
                    Log.e(TAG, "Could not record consent for $contributionId", e)
                    return@withContext EnqueueResult.Failed("Could not record your consent")
                }

                CurationSync.requestUpload(context)
                EnqueueResult.Queued
            }
        }
    }

    /**
     * Drains the outbox. Called by the worker, not by UI.
     *
     * Returns true when at least one row is still waiting, which the worker
     * turns into a WorkManager retry so backoff applies.
     */
    suspend fun drain(): Boolean = withContext(Dispatchers.IO) {
        dao.requeueStalled()

        for (row in dao.byStatus(ContributionOutbox.PENDING)) {
            dao.updateStatus(
                row.contributionId, ContributionOutbox.UPLOADING, row.attemptCount, row.lastError
            )

            when (val outcome = ContributionUploader.upload(row)) {
                is ContributionUploader.Outcome.Uploaded -> {
                    dao.updateStatus(
                        row.contributionId, ContributionOutbox.DONE, row.attemptCount, null
                    )
                    discardLocalImage(row)
                }

                is ContributionUploader.Outcome.Duplicate -> {
                    // Consent still counts and stays recorded; only the bytes go.
                    dao.updateStatus(
                        row.contributionId,
                        ContributionOutbox.REJECTED_DUPLICATE,
                        row.attemptCount,
                        null
                    )
                    discardLocalImage(row)
                }

                is ContributionUploader.Outcome.Retry -> {
                    val attempts = row.attemptCount + 1
                    val exhausted = attempts >= ContributionOutbox.MAX_ATTEMPTS
                    dao.updateStatus(
                        row.contributionId,
                        if (exhausted) ContributionOutbox.FAILED else ContributionOutbox.PENDING,
                        attempts,
                        outcome.message
                    )
                }

                is ContributionUploader.Outcome.Fatal -> {
                    dao.updateStatus(
                        row.contributionId,
                        ContributionOutbox.FAILED,
                        row.attemptCount + 1,
                        outcome.message
                    )
                }
            }
        }

        dao.byStatus(ContributionOutbox.PENDING).isNotEmpty()
    }

    suspend fun pendingCount(): Int = withContext(Dispatchers.IO) { dao.pendingCount() }

    suspend fun failedCount(): Int = withContext(Dispatchers.IO) { dao.failedCount() }

    /** Clears the attempt counter on everything that gave up, for a manual retry. */
    suspend fun retryFailed(): Int = withContext(Dispatchers.IO) {
        val count = dao.retryFailed()
        if (count > 0) CurationSync.requestUpload(context)
        count
    }

    /**
     * Removes the pre-curation `/training/` folder, which held straight copies
     * of camera files — full resolution, EXIF intact. Those predate the
     * de-identification requirement and must not linger on disk or get uploaded
     * by any future code path.
     */
    suspend fun purgeLegacyTrainingFolder() = withContext(Dispatchers.IO) {
        val legacy = File(context.filesDir, "training")
        if (!legacy.exists()) return@withContext
        val removed = legacy.deleteRecursively()
        Log.i(TAG, "Removed legacy raw training folder (success=$removed)")
        context.getSharedPreferences("dermify_uploads", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    private fun discardLocalImage(row: ContributionOutbox) {
        val file = File(row.imagePath)
        if (file.exists() && !file.delete()) {
            Log.w(TAG, "Could not delete staged image ${row.imagePath}")
        }
    }

    private fun contributionsDir(): File =
        File(context.filesDir, "contributions").apply { mkdirs() }

    private fun appVersion(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    } catch (e: PackageManager.NameNotFoundException) {
        "unknown"
    }

    /** Date only. Anything finer is a timestamp that helps re-identify a contributor. */
    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    companion object {
        private const val TAG = "ContributionRepo"
    }
}
