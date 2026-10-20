package com.dermify.dermify.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

object SupabaseManager {

    private const val SUPABASE_URL = "https://wurmuevgvlcqnypmpfdr.supabase.co"

    // The anon key is a public identifier and is expected to ship in the app —
    // it grants only what Row Level Security allows. The service_role key must
    // never appear here, in the repository, or in any log. Privileged work goes
    // in an Edge Function.
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Ind1cm11ZXZndmxjcW55cG1wZmRyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODM3MzExMzIsImV4cCI6MjA5OTMwNzEzMn0.Eg273gag5V5bcf4ef-53hQUAIg_AUZgRERyRjnCBvD0"

    /**
     * Bucket name as created in the Supabase dashboard. Case-sensitive — a
     * mismatch here surfaces as an opaque storage error, not a clear 404.
     */
    const val BUCKET = "scans"

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Storage)
        install(Postgrest)
    }

    val storage get() = client.storage
    val postgrest get() = client.postgrest
}
