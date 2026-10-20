package com.dermify.dermify.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log

/**
 * Two-layer "should this photo reach the disease classifier?" check.
 *
 *   Layer 1 - SkinHeuristic (colour maths, ~50ms, no model)
 *       catches: rooms, walls, sky, grass, grey electronics, documents
 *       misses:  wood, cardboard, beige/tan surfaces (read as skin-coloured)
 *
 *   Layer 2 - NotSkinGate (ImageNet MobileNet)
 *       catches: recognisable objects - desks, tables, mugs, chargers, keyboards
 *       misses:  arbitrary scenes and featureless surfaces (not ImageNet classes)
 *
 * The MobileNet model is obtained from ModelCache (loaded once, reused across scans)
 * rather than constructed and closed on every call - that reload was a big chunk of
 * per-scan latency. We therefore do NOT close it here; ModelCache owns its lifecycle.
 */
class CaptureGate(private val context: Context) {

    sealed class Verdict {
        object Accepted : Verdict()
        data class Rejected(val reason: String, val userMessage: String) : Verdict()
    }

    fun check(bitmap: Bitmap): Verdict {
        // ---- Layer 1: colour heuristic (cheap, no model load) ----
        val ratio = SkinHeuristic.skinRatio(bitmap)
        if (ratio < SkinHeuristic.DEFAULT_THRESHOLD) {
            Log.d(TAG, "rejected by SkinHeuristic (skinRatio=$ratio)")
            return Verdict.Rejected(
                reason = "low_skin_ratio",
                userMessage = "Dermify couldn't find skin in this image. Make sure the " +
                        "affected area fills most of the frame, is well lit, and is in focus."
            )
        }

        // ---- Layer 2: ImageNet object check (reused instance, not reloaded) ----
        try {
            val gate = ModelCache.notSkinGate(context)
            if (gate != null && gate.isLikelyNotSkin(bitmap)) {
                Log.d(TAG, "rejected by NotSkinGate (recognised as an object)")
                return Verdict.Rejected(
                    reason = "recognised_object",
                    userMessage = "This looks like an object rather than skin. Please retake, " +
                            "framing the affected area closely."
                )
            }
        } catch (e: Exception) {
            // If the gate is unavailable, don't block the user - layer 1 already passed.
            Log.w(TAG, "NotSkinGate unavailable, relying on SkinHeuristic only", e)
        }
        // ---- Layer 3: binary skin detector (reused instance, not reloaded) ----
        // Backstop for non-skin (e.g. wood, cardboard, tan surfaces) that Layer 1's
        // colour maths and Layer 2's ImageNet check both miss.
        try {
            val skinGate = ModelCache.skinBinaryGate(context)
            if (skinGate != null && skinGate.isLikelyNotSkin(bitmap)) {
                Log.d(TAG, "rejected by SkinBinaryGate (binary detector says not skin)")
                return Verdict.Rejected(
                    reason = "binary_not_skin",
                    userMessage = "This doesn't look like skin. Please retake, framing the " +
                            "affected area closely and in good light."
                )
            }
        } catch (e: Exception) {
            // Gate unavailable -> don't block; Layers 1 & 2 already passed.
            Log.w(TAG, "SkinBinaryGate unavailable, relying on earlier gates", e)
        }

        Log.d(TAG, "accepted (skinRatio=$ratio)")
        return Verdict.Accepted
    }

    companion object {
        private const val TAG = "CaptureGate"
    }
}