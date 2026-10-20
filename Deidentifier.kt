package com.dermify.dermify.curation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume

/**
 * Turns a captured camera file into bytes that are safe to upload.
 *
 * Runs before every upload, with no exception path. Privacy failures here are
 * silent — an EXIF field that survives produces no error and no crash — so the
 * checks are written as assertions that fail the contribution rather than as
 * best-effort cleanup.
 *
 * Cost on the Samsung A320 budget: one decode, one downscale to 512, one JPEG
 * encode, one ML Kit fast-mode face pass, one DCT over 32x32. All off the main
 * thread. No new TFLite interpreter enters ModelCache.
 */
object Deidentifier {

    private const val TAG = "Deidentify"

    /** Plenty for retraining at 224, and it discards incidental background detail. */
    private const val MAX_DIMENSION = 512
    private const val JPEG_QUALITY = 90

    sealed class Result {
        /** [jpeg] carries no EXIF, no GPS and no face. [phash] hashes exactly these bytes. */
        data class Success(val jpeg: ByteArray, val phash: String) : Result()

        /** The image is disqualified from contribution. The user is told why. */
        data class Rejected(val reason: String, val userMessage: String) : Result()

        /** Something went wrong; nothing is uploaded. */
        data class Failed(val message: String) : Result()
    }

    suspend fun deidentify(sourcePath: String): Result = withContext(Dispatchers.Default) {
        val file = File(sourcePath)
        if (!file.exists()) return@withContext Result.Failed("Source image no longer exists")

        val decoded = decodeDownscaled(file)
            ?: return@withContext Result.Failed("Could not decode the captured image")

        try {
            // 1. Downscale. Done before face detection so the detector runs on the
            //    same pixels that would be uploaded, not on a larger original.
            val scaled = scaleToBound(decoded, MAX_DIMENSION)

            // 2. Face rejection. Dermify's terms exclude face images from training.
            val faceCount = countFaces(scaled)
            if (faceCount < 0) {
                return@withContext Result.Failed("Face check could not run")
            }
            if (faceCount > 0) {
                return@withContext Result.Rejected(
                    reason = "face_detected",
                    userMessage = "This photo contains a face, so it can't be added to " +
                            "the training set. Your result is unaffected."
                )
            }

            // 3. Re-encode. Decoding and re-encoding is the reliable EXIF strip:
            //    a Bitmap carries no metadata, so the output has none to inherit.
            //    The original camera file is never uploaded.
            val jpeg = ByteArrayOutputStream().use { out ->
                if (!scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) {
                    return@withContext Result.Failed("JPEG encoding failed")
                }
                out.toByteArray()
            }

            // 4. Assert the strip actually happened rather than assuming it.
            val leak = findMetadataLeak(jpeg)
            if (leak != null) {
                Log.e(TAG, "Refusing to upload: metadata survived re-encode ($leak)")
                return@withContext Result.Failed("Image metadata could not be removed")
            }

            // 5. Hash the encoded bytes, not the in-memory bitmap, so the hash the
            //    server dedups on is the hash of the file that actually gets stored.
            //    That keeps collection and training agreeing on what a duplicate is.
            val stored = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
                ?: return@withContext Result.Failed("Could not re-read the encoded image")
            val phash = try {
                PerceptualHash.compute(stored)
            } finally {
                stored.recycle()
                scaled.recycle()
            }

            Result.Success(jpeg, phash)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory during de-identification", e)
            Result.Failed("Not enough memory to prepare the image")
        } catch (e: Exception) {
            Log.e(TAG, "De-identification failed", e)
            Result.Failed(e.message ?: "De-identification failed")
        }
    }

    /**
     * Reads the file at roughly the size wanted, so a 12 MP capture never lands
     * fully decoded in memory on a low-end device.
     */
    private fun decodeDownscaled(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_DIMENSION &&
            bounds.outHeight / (sample * 2) >= MAX_DIMENSION
        ) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private fun scaleToBound(source: Bitmap, bound: Int): Bitmap {
        val longest = maxOf(source.width, source.height)
        if (longest <= bound) return source
        val ratio = bound.toDouble() / longest
        val width = (source.width * ratio).toInt().coerceAtLeast(1)
        val height = (source.height * ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(source, width, height, true)
        if (scaled != source) source.recycle()
        return scaled
    }

    /** Returns the face count, or -1 if detection could not complete. */
    private suspend fun countFaces(bitmap: Bitmap): Int = suspendCancellableCoroutine { cont ->
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
        val detector = FaceDetection.getClient(options)
        detector.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { faces ->
                detector.close()
                if (cont.isActive) cont.resume(faces.size)
            }
            .addOnFailureListener { e ->
                detector.close()
                Log.e(TAG, "Face detection failed", e)
                // Fail closed. An undetectable face is a reason not to contribute,
                // not a reason to upload anyway.
                if (cont.isActive) cont.resume(-1)
            }
    }

    /**
     * Assertion, not cleanup. Names the first identifying tag found, or null when
     * the payload is clean.
     */
    private fun findMetadataLeak(jpeg: ByteArray): String? {
        return try {
            val exif = ByteArrayInputStream(jpeg).use { ExifInterface(it) }
            val forbidden = listOf(
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_TIMESTAMP,
                ExifInterface.TAG_GPS_DATESTAMP,
                ExifInterface.TAG_GPS_PROCESSING_METHOD,
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_DATETIME_DIGITIZED,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_IMAGE_UNIQUE_ID,
                ExifInterface.TAG_SUBJECT_LOCATION,
                ExifInterface.TAG_ARTIST,
                ExifInterface.TAG_CAMERA_OWNER_NAME,
                ExifInterface.TAG_BODY_SERIAL_NUMBER,
                ExifInterface.TAG_SOFTWARE
            )
            forbidden.firstOrNull { exif.getAttribute(it) != null }
                ?: if (exif.latLong != null) "resolved lat/long" else null
        } catch (e: Exception) {
            // A JPEG with no EXIF segment at all is the expected outcome and can
            // throw here on some Android versions. Treat unreadable as clean only
            // because step 3 built these bytes from a metadata-free Bitmap.
            Log.d(TAG, "No readable EXIF segment (expected): ${e.message}")
            null
        }
    }
}
