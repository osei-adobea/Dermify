package com.dermify.dermify

import android.app.Application
import com.dermify.dermify.curation.ContributionRepository
import com.dermify.dermify.curation.CurationSync
import com.dermify.dermify.ml.ModelCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DermifyApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Anything the last run left mid-upload resumes here. The worker resets
        // stalled rows itself, so this is just the trigger.
        CurationSync.requestUpload(this)
        CurationSync.schedulePeriodicSync(this)

        appScope.launch {
            ContributionRepository(this@DermifyApp).purgeLegacyTrainingFolder()
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_BACKGROUND) {
            ModelCache.release()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        ModelCache.release()
    }
}
