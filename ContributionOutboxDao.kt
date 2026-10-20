package com.dermify.dermify.curation

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ContributionOutboxDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(row: ContributionOutbox)

    @Query("SELECT * FROM contribution_outbox WHERE status = :status ORDER BY createdAtDate ASC")
    suspend fun byStatus(status: String): List<ContributionOutbox>

    @Query("SELECT * FROM contribution_outbox WHERE contributionId = :id")
    suspend fun byId(id: String): ContributionOutbox?

    /**
     * Anything left UPLOADING belongs to a worker that was killed mid-flight.
     * Resetting it on startup is what makes an interrupted upload resume rather
     * than sit in limbo.
     */
    @Query("UPDATE contribution_outbox SET status = '${ContributionOutbox.PENDING}' WHERE status = '${ContributionOutbox.UPLOADING}'")
    suspend fun requeueStalled(): Int

    @Query("UPDATE contribution_outbox SET status = :status, attemptCount = :attempts, lastError = :error WHERE contributionId = :id")
    suspend fun updateStatus(id: String, status: String, attempts: Int, error: String?)

    @Query("UPDATE contribution_outbox SET status = '${ContributionOutbox.PENDING}', attemptCount = 0, lastError = NULL WHERE status = '${ContributionOutbox.FAILED}'")
    suspend fun retryFailed(): Int

    @Query("SELECT COUNT(*) FROM contribution_outbox WHERE status IN ('${ContributionOutbox.PENDING}', '${ContributionOutbox.UPLOADING}')")
    suspend fun pendingCount(): Int

    @Query("SELECT COUNT(*) FROM contribution_outbox WHERE status = '${ContributionOutbox.FAILED}'")
    suspend fun failedCount(): Int

    @Query("SELECT * FROM contribution_outbox ORDER BY createdAtDate DESC")
    fun observeAll(): LiveData<List<ContributionOutbox>>

    @Query("DELETE FROM contribution_outbox WHERE contributionId = :id")
    suspend fun deleteById(id: String)
}
