package com.dermify.dermify.ml

import android.content.Context
import android.util.Log

/**
 * Loads each TFLite-backed helper ONCE and reuses it across scans.
 *
 * WHY: previously CaptureGate did `NotSkinGate(context)...close()` and ResultFragment
 * did `HeatmapGenerator(...)...close()` on EVERY scan. Loading a model (memory-mapping
 * a 5-16 MB file + allocating tensors) is often as costly as the inference itself, so
 * doing it per-scan doubled the work. Here they load lazily on first use and stay cached.
 *
 * MEMORY: the cached models hold ~5 MB (heatmap) + ~16 MB (MobileNet) resident. On low-end
 * devices that can matter, so call release() from Application.onTrimMemory() /
 * onLowMemory() to free them; they'll lazily reload on next use.
 *
 * THREADING: getters are synchronized so two background threads can't both build the same
 * model at once. Do the actual inference off the main thread (see ResultFragment).
 */
object ModelCache {

    private const val TAG = "ModelCache"

    private const val HEATMAP_MODEL = "dermify_model_with_heatmap.tflite"
    private const val CAM_WEIGHTS = "cam_class_weights.json"

    @Volatile private var heatmap: HeatmapGenerator? = null
    @Volatile private var notSkin: NotSkinGate? = null
    @Volatile private var skinBinary: SkinBinaryGate? = null


    /** The disease classifier + heatmap generator. Null only if assets are missing/broken. */
    @Synchronized
    fun heatmapGenerator(context: Context): HeatmapGenerator? {
        heatmap?.let { return it }
        return try {
            HeatmapGenerator(context.applicationContext, HEATMAP_MODEL, CAM_WEIGHTS).also {
                heatmap = it
                Log.d(TAG, "HeatmapGenerator loaded")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load HeatmapGenerator", e)
            null
        }
    }

    /** The ImageNet object gate. Null if the MobileNet asset is missing/broken. */
    @Synchronized
    fun notSkinGate(context: Context): NotSkinGate? {
        notSkin?.let { return it }
        return try {
            NotSkinGate(context.applicationContext).also {
                notSkin = it
                Log.d(TAG, "NotSkinGate loaded")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load NotSkinGate", e)
            null
        }
    }

    /** The binary skin detector (Hannah's domain-trained gate). Null if the asset is missing/broken. */
    @Synchronized
    fun skinBinaryGate(context: Context): SkinBinaryGate? {
        skinBinary?.let { return it }
        return try {
            SkinBinaryGate(context.applicationContext).also {
                skinBinary = it
                Log.d(TAG, "SkinBinaryGate loaded")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load SkinBinaryGate", e)
            null
        }
    }

    /** Free all cached models. Safe to call anytime; they reload lazily on next use. */
    @Synchronized
    fun release() {
        try { heatmap?.close() } catch (_: Exception) {}
        try { notSkin?.close() } catch (_: Exception) {}
        try { skinBinary?.close() } catch (_: Exception) {}
        heatmap = null
        notSkin = null
        skinBinary = null
        Log.d(TAG, "released")
    }
}