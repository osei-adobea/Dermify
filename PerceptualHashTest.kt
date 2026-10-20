package com.dermify.dermify.curation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

/**
 * The perceptual hash decides what counts as a duplicate at ingest, and the
 * training pipeline uses the same definition to build group-aware splits. If
 * the two ever disagree, the leakage incident that motivated all of this comes
 * back — silently, because a wrong hash throws no error.
 *
 * These tests pin the behaviour that matters:
 *   - the DCT is right (checked against a case with a hand-derivable answer)
 *   - the output format matches imagehash's 16-hex-char form
 *   - resizing and JPEG-style perturbation leave the hash inside Hamming 5
 *   - unrelated images land far outside it
 */
class PerceptualHashTest {

    private fun gray(v: Int): Int {
        val c = v.coerceIn(0, 255)
        return (0xFF shl 24) or (c shl 16) or (c shl 8) or c
    }

    private fun image(width: Int, height: Int, f: (Int, Int) -> Int): IntArray =
        IntArray(width * height) { i -> gray(f(i % width, i / width)) }

    @Test
    fun `DC term dominates, and a featureless image hashes unstably`() {
        // Every AC coefficient of a constant image is mathematically zero, so
        // the DC term is the only one that can exceed the block median. It is
        // bit 0, which is the most significant bit.
        val flat = image(64, 64) { _, _ -> 128 }
        val hash = PerceptualHash.compute(flat, 64, 64)
        val topBitSet = (hash.toULong(16) and 0x8000000000000000uL) != 0uL
        assertTrue("top bit should be the DC term: $hash", topBitSet)

        // The remaining 63 bits are NOT all zero, and this is not a bug in the
        // implementation — imagehash behaves the same way. Those coefficients
        // are floating-point noise around zero, the median lands inside that
        // noise, and roughly half the comparisons come out true. A featureless
        // image therefore has an essentially arbitrary low-order hash.
        //
        // It does not matter here: a flat surface never reaches this code,
        // because CaptureGate layer 1 rejects it long before contribution. It
        // is recorded so nobody later "fixes" it by special-casing near-zero
        // coefficients and thereby diverges from the training pipeline.
        assertEquals(
            "hash must at least be deterministic",
            hash,
            PerceptualHash.compute(flat, 64, 64)
        )
    }

    @Test
    fun `hash is sixteen lowercase hex characters`() {
        val img = image(90, 70) { x, y -> (x * 3 + y * 5) % 256 }
        val hash = PerceptualHash.compute(img, 90, 70)
        assertEquals(16, hash.length)
        assertTrue("unexpected form: $hash", hash.matches(Regex("^[0-9a-f]{16}$")))
    }

    @Test
    fun `same content at a different resolution stays within the duplicate threshold`() {
        // This is the case that actually happened in the training data: the same
        // clinical photograph republished by a second dataset at another size
        // under a different filename.
        val pattern = { x: Int, y: Int, scale: Double ->
            (128 + 110 * sin(x / (9.0 * scale)) * sin(y / (13.0 * scale))).toInt()
        }
        val large = image(320, 320) { x, y -> pattern(x, y, 320 / 160.0) }
        val small = image(160, 160) { x, y -> pattern(x, y, 1.0) }

        val distance = PerceptualHash.hammingDistance(
            PerceptualHash.compute(large, 320, 320),
            PerceptualHash.compute(small, 160, 160)
        )
        assertTrue("resized copy scored $distance, expected <= 5", distance in 0..5)
    }

    @Test
    fun `recompression-style noise stays within the duplicate threshold`() {
        val base = image(200, 200) { x, y -> (x * 2 + y) % 256 }
        // Low-amplitude perturbation of the kind JPEG quantisation introduces.
        val noisy = IntArray(base.size) { i ->
            val v = (base[i] and 0xFF) + (if (i % 7 == 0) 4 else -3)
            gray(v)
        }

        val distance = PerceptualHash.hammingDistance(
            PerceptualHash.compute(base, 200, 200),
            PerceptualHash.compute(noisy, 200, 200)
        )
        assertTrue("recompressed copy scored $distance, expected <= 5", distance in 0..5)
    }

    @Test
    fun `unrelated images sit well outside the duplicate threshold`() {
        val diagonal = image(128, 128) { x, y -> (x + y) * 2 % 256 }
        val rings = image(128, 128) { x, y ->
            val dx = x - 64
            val dy = y - 64
            ((dx * dx + dy * dy) / 12) % 256
        }

        val distance = PerceptualHash.hammingDistance(
            PerceptualHash.compute(diagonal, 128, 128),
            PerceptualHash.compute(rings, 128, 128)
        )
        assertTrue("unrelated images scored $distance, expected > 5", distance > 5)
    }

    @Test
    fun `hamming distance handles identical and fully inverted hashes`() {
        assertEquals(0, PerceptualHash.hammingDistance("0123456789abcdef", "0123456789abcdef"))
        assertEquals(64, PerceptualHash.hammingDistance("0000000000000000", "ffffffffffffffff"))
    }

    @Test
    fun `malformed hashes report minus one rather than a plausible distance`() {
        // A silent 0 here would read as "duplicate" and drop a real contribution.
        assertEquals(-1, PerceptualHash.hammingDistance("abc", "0123456789abcdef"))
        assertEquals(-1, PerceptualHash.hammingDistance("0123456789abcdef", "zzzzzzzzzzzzzzzz"))
    }
}
