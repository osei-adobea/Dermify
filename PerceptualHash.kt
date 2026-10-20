package com.dermify.dermify.curation

import android.graphics.Bitmap
import kotlin.math.PI
import kotlin.math.cos

/**
 * DCT-based perceptual hash — the Kotlin port of the algorithm the training
 * notebook uses (CELL 3), which is `imagehash.phash` with default parameters.
 *
 * The training pipeline found 30 cross-split near-duplicates with this hash at
 * Hamming <= 5. Collection has to use the *same* definition of "duplicate" or
 * the pipeline reintroduces the problem it was built to solve, so this is a
 * deliberate port rather than a fresh implementation.
 *
 * imagehash.phash(image, hash_size=8, highfreq_factor=4):
 *   1. convert to 8-bit grayscale (ITU-R 601-2 luma)
 *   2. resize to 32x32
 *   3. 2-D DCT-II
 *   4. keep the top-left 8x8 low-frequency block (DC term included)
 *   5. bit = coefficient > median(block)
 *   6. pack row-major, MSB first, as 16 hex chars
 *
 * Fidelity note: PIL resamples with LANCZOS; this uses box/area averaging,
 * which is the closest cheap equivalent for a large downscale. A handful of
 * low-order bits can differ between the two for the same image. That is well
 * inside the Hamming-5 threshold (measured separation in the training data:
 * true duplicates at 0, distinct images at >= 14), but it is a real difference
 * and the reason the threshold is not tightened.
 */
object PerceptualHash {

    private const val HASH_SIZE = 8
    private const val HIGHFREQ_FACTOR = 4
    private const val IMG_SIZE = HASH_SIZE * HIGHFREQ_FACTOR  // 32

    /** cos table: COS[k * IMG_SIZE + n] = cos(pi * (2n + 1) * k / (2 * IMG_SIZE)) */
    private val COS: DoubleArray = DoubleArray(IMG_SIZE * IMG_SIZE).also { table ->
        for (k in 0 until IMG_SIZE) {
            for (n in 0 until IMG_SIZE) {
                table[k * IMG_SIZE + n] = cos(PI * (2 * n + 1) * k / (2.0 * IMG_SIZE))
            }
        }
    }

    /** 16 lowercase hex characters, big-endian, matching `imagehash` string form. */
    fun compute(bitmap: Bitmap): String {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return compute(pixels, bitmap.width, bitmap.height)
    }

    /**
     * Pixel-array overload. Exists so the hash can be exercised in a plain JVM
     * unit test — android.graphics.Bitmap is a stub off-device, and an untested
     * hash is exactly the kind of thing that silently disagrees with the
     * training pipeline.
     *
     * [pixels] is row-major ARGB_8888, the layout Bitmap.getPixels produces.
     */
    fun compute(pixels: IntArray, width: Int, height: Int): String {
        require(width > 0 && height > 0) { "Image must have non-zero dimensions" }
        require(pixels.size >= width * height) { "Pixel buffer smaller than $width x $height" }

        val small = grayscaleAndResize(pixels, width, height)
        val dct = dct2d(small)

        val low = DoubleArray(HASH_SIZE * HASH_SIZE)
        for (row in 0 until HASH_SIZE) {
            for (col in 0 until HASH_SIZE) {
                low[row * HASH_SIZE + col] = dct[row * IMG_SIZE + col]
            }
        }
        val median = median(low)

        // Row-major, first bit is the most significant bit of the 64-bit value.
        // Mirrors imagehash._binary_array_to_hex.
        var bits = 0L
        for (i in low.indices) {
            bits = bits shl 1
            if (low[i] > median) bits = bits or 1L
        }
        return java.lang.String.format("%016x", bits)
    }

    /** Hamming distance between two 16-char hex hashes. -1 if either is malformed. */
    fun hammingDistance(a: String, b: String): Int {
        val left = a.toULongOrNullHex() ?: return -1
        val right = b.toULongOrNullHex() ?: return -1
        return java.lang.Long.bitCount((left xor right).toLong())
    }

    private fun String.toULongOrNullHex(): ULong? =
        if (length != 16) null else try { trim().toULong(16) } catch (e: NumberFormatException) { null }

    /**
     * Grayscale first, then resize — the order imagehash uses.
     * Box averaging over the source pixels mapped to each of the 32x32 cells.
     */
    private fun grayscaleAndResize(pixels: IntArray, width: Int, height: Int): DoubleArray {
        val sums = DoubleArray(IMG_SIZE * IMG_SIZE)
        val counts = IntArray(IMG_SIZE * IMG_SIZE)

        for (y in 0 until height) {
            val cellY = (y.toLong() * IMG_SIZE / height).toInt().coerceAtMost(IMG_SIZE - 1)
            val rowBase = y * width
            for (x in 0 until width) {
                val cellX = (x.toLong() * IMG_SIZE / width).toInt().coerceAtMost(IMG_SIZE - 1)
                val p = pixels[rowBase + x]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                // PIL "L" conversion: integer ITU-R 601-2 luma, truncated.
                val luma = (r * 299 + g * 587 + b * 114) / 1000
                val idx = cellY * IMG_SIZE + cellX
                sums[idx] += luma.toDouble()
                counts[idx]++
            }
        }

        val out = DoubleArray(IMG_SIZE * IMG_SIZE)
        for (i in out.indices) {
            out[i] = if (counts[i] > 0) sums[i] / counts[i] else 0.0
        }
        return out
    }

    /**
     * Separable 2-D DCT-II, unnormalised.
     *
     * scipy.fftpack.dct(..., norm=None) carries a uniform factor of 2 per axis
     * that this drops. Bits are decided by comparison against the median of the
     * same block, and a uniform positive scale preserves that ordering, so the
     * resulting hash is identical.
     */
    private fun dct2d(input: DoubleArray): DoubleArray {
        val afterRows = DoubleArray(IMG_SIZE * IMG_SIZE)
        for (row in 0 until IMG_SIZE) {
            val base = row * IMG_SIZE
            for (k in 0 until IMG_SIZE) {
                var acc = 0.0
                val cosBase = k * IMG_SIZE
                for (n in 0 until IMG_SIZE) acc += input[base + n] * COS[cosBase + n]
                afterRows[base + k] = acc
            }
        }

        val out = DoubleArray(IMG_SIZE * IMG_SIZE)
        for (col in 0 until IMG_SIZE) {
            for (k in 0 until IMG_SIZE) {
                var acc = 0.0
                val cosBase = k * IMG_SIZE
                for (n in 0 until IMG_SIZE) acc += afterRows[n * IMG_SIZE + col] * COS[cosBase + n]
                out[k * IMG_SIZE + col] = acc
            }
        }
        return out
    }

    /** numpy.median semantics: mean of the two central values when the count is even. */
    private fun median(values: DoubleArray): Double {
        val sorted = values.copyOf()
        sorted.sort()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2.0 else sorted[mid]
    }
}
