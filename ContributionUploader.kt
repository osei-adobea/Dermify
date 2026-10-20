package com.dermify.dermify.curation

import android.util.Log
import com.dermify.dermify.data.SupabaseManager
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File

/**
 * The single network path a contributed image takes.
 *
 * Two steps, in this order and not the other:
 *
 *   1. `register_contribution` — the server checks the perceptual hash against
 *      everything already collected and either reserves a storage path or says
 *      duplicate. Nothing has been transferred at this point, so a duplicate
 *      costs one small request instead of an image upload on a metered
 *      connection.
 *   2. Storage upload to the reserved path, then `mark_contribution_stored`.
 *
 * Both RPCs are idempotent by contribution ID, so a retry after a partial
 * failure resumes rather than duplicating. The storage upload is a plain,
 * non-upsert write for a reason documented at the call site — short version,
 * upsert needs a table privilege the anon role doesn't have here.
 */
object ContributionUploader {

    private const val TAG = "ContributionUpload"

    private val json = Json { ignoreUnknownKeys = true }

    sealed class Outcome {
        object Uploaded : Outcome()
        object Duplicate : Outcome()

        /** Worth another attempt later — offline, timeout, 5xx. */
        data class Retry(val message: String) : Outcome()

        /** Will not succeed by retrying — bad request, RLS refusal, missing file. */
        data class Fatal(val message: String) : Outcome()
    }

    suspend fun upload(row: ContributionOutbox): Outcome = withContext(Dispatchers.IO) {
        val file = File(row.imagePath)
        if (!file.exists()) {
            return@withContext Outcome.Fatal("Local image missing at ${row.imagePath}")
        }

        val storagePath = when (val registration = register(row)) {
            is Registration.Duplicate -> return@withContext Outcome.Duplicate
            is Registration.Failure -> return@withContext registration.outcome
            is Registration.Accepted -> registration.storagePath
        }

        try {
            // upsert = false, deliberately. Postgres implements upsert as
            // INSERT ... ON CONFLICT DO UPDATE, and that statement shape needs
            // the executing role to hold base SELECT privilege on the table —
            // separately from and in addition to any RLS policy — just to be
            // planned, regardless of whether a conflict actually occurs at
            // runtime. `anon` has INSERT/UPDATE granted here but not SELECT
            // (confirmed empirically: a plain INSERT as anon succeeds; the
            // same request with upsert=true fails on the very first write for
            // a brand-new path, before any real conflict could exist).
            //
            // Granting SELECT to fix that would reopen exactly the privacy gap
            // this pipeline is built to avoid — a contributor able to read
            // rows would be a contributor able to browse the collection.
            // storagePath is derived from a client-generated UUID v4 that is
            // never reused across contributions, so there is normally nothing
            // to overwrite; see the 409 handling below for the one legitimate
            // case where a name collides with itself.
            SupabaseManager.storage
                .from(SupabaseManager.BUCKET)
                .upload(storagePath, file.readBytes(), upsert = false)
        } catch (e: Exception) {
            // Log the whole exception, not just the message. The open Supabase
            // upload bug (Postgres 58P01) was invisible precisely because only
            // e.message was ever logged, which drops the status code and
            // response body that say whether it is RLS, the bucket name, or auth.
            Log.e(TAG, "Storage upload failed for ${row.contributionId} -> $storagePath", e)

            if (e is RestException && e.statusCode == 409) {
                // The only way this exact, UUID-derived path can already exist
                // is that an earlier attempt for this same contribution wrote
                // it fully before some later step (mark_contribution_stored,
                // or the RPC response) failed. The bytes are already correct
                // and re-sending them would need the same privilege this row
                // doesn't have — so treat "already there" as success and move
                // on to marking it stored, rather than failing forever.
                Log.i(TAG, "${row.contributionId} already present in storage (409) — continuing")
            } else {
                return@withContext classify(e, "storage upload")
            }
        }

        try {
            SupabaseManager.postgrest.rpc(
                "mark_contribution_stored",
                buildJsonObject { put("p_id", row.contributionId) }
            )
        } catch (e: Exception) {
            // The bytes are up but the row is not marked. Retrying re-runs both
            // steps idempotently, so this is safe to treat as retryable.
            Log.e(TAG, "mark_contribution_stored failed for ${row.contributionId}", e)
            return@withContext Outcome.Retry(e.describe())
        }

        Outcome.Uploaded
    }

    private sealed class Registration {
        data class Accepted(val storagePath: String) : Registration()
        object Duplicate : Registration()
        data class Failure(val outcome: Outcome) : Registration()
    }

    private suspend fun register(row: ContributionOutbox): Registration {
        val params = buildJsonObject {
            put("p_id", row.contributionId)
            put("p_phash", row.phash)
            put("p_predicted_class", row.predictedClass)
            put("p_confidence", row.confidence)
            put("p_consent_version", row.consentVersion)
            put("p_app_version", row.appVersion)
            put("p_contributed_date", row.createdAtDate)
            put("p_region", ConsentPolicy.REGION)
        }

        val body = try {
            SupabaseManager.postgrest.rpc("register_contribution", params).data
        } catch (e: Exception) {
            Log.e(TAG, "register_contribution failed for ${row.contributionId}", e)
            return Registration.Failure(classify(e, "register_contribution"))
        }

        val parsed: JsonObject = try {
            json.parseToJsonElement(body).jsonObject
        } catch (e: Exception) {
            Log.e(TAG, "register_contribution returned unparseable body: $body", e)
            return Registration.Failure(Outcome.Fatal("Unexpected server response"))
        }

        val duplicate = parsed["duplicate"]?.jsonPrimitive?.boolean ?: false
        if (duplicate) {
            Log.i(TAG, "Server rejected ${row.contributionId} as a perceptual duplicate")
            return Registration.Duplicate
        }

        val accepted = parsed["accepted"]?.jsonPrimitive?.boolean ?: false
        val storagePath = parsed["storage_path"]?.jsonPrimitive?.contentOrNull
        if (!accepted || storagePath.isNullOrBlank()) {
            return Registration.Failure(
                Outcome.Fatal("Server declined the contribution: $body")
            )
        }
        return Registration.Accepted(storagePath)
    }

    /**
     * Transport problems are worth retrying; refusals are not. Retrying an RLS
     * denial forever would drain battery and hide the real fix, which is a
     * policy change on the server — so a 401/403 lands in the outbox as FAILED
     * with the server's own wording attached, where the Profile screen can show
     * it.
     *
     * 408 and 429 are the exceptions: the server answered, but it answered
     * "later", so those stay retryable alongside every 5xx.
     */
    private fun classify(e: Exception, stage: String): Outcome {
        if (e !is RestException) {
            // HttpRequestException and anything else non-HTTP: offline, DNS,
            // timeout. Always worth another attempt.
            return Outcome.Retry("$stage failed: ${e.describe()}")
        }
        val retryable = e.statusCode >= 500 || e.statusCode == 408 || e.statusCode == 429
        val text = "$stage HTTP ${e.statusCode} ${e.error}" +
                (e.description?.let { " — $it" } ?: "")
        return if (retryable) Outcome.Retry(text) else Outcome.Fatal(text)
    }

    /** Class name included — a bare message is often empty on network exceptions. */
    private fun Exception.describe(): String =
        "${this::class.java.simpleName}: ${message ?: "no message"}"
}
