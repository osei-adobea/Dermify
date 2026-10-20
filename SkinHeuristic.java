package com.dermify.dermify.ml;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Dermify - Skin/Not-Skin colour heuristic.
 *
 * Converts pixels to YCbCr and measures what fraction fall inside a skin-tone
 * range. No model, no training, runs on a 128x128 downscale in well under 50ms.
 * Run this FIRST, before loading any TFLite model.
 *
 * MEASURED BEHAVIOUR (tested against the Dermify training images and synthetic
 * Fitzpatrick I-VI swatches):
 *   real skin photos ......... ratio 0.63 - 0.95   -> passes
 *   Fitzpatrick I-VI swatches. ratio 1.00 (all)    -> passes, incl. very dark skin
 *   white wall ............... 0.00                -> rejected
 *   grey electronics ......... 0.00                -> rejected
 *   blue sky / green grass ... 0.00                -> rejected
 *   wood / beige / cardboard . 1.00                -> PASSES (known blind spot)
 *
 * The beige/wood false positive is inherent to colour-based detection. It is
 * covered by running NotSkinGate (ImageNet MobileNet) afterwards, which does
 * recognise desks and tables. See CaptureGate.
 */
public final class SkinHeuristic {

    /**
     * Default threshold. Raised from the original 0.15 to 0.35 because measured
     * skin ratios sit at 0.63+, leaving ample headroom. Higher = stricter.
     * If real photos get wrongly rejected, lower it; if junk gets through, raise it.
     */
    public static final float DEFAULT_THRESHOLD = 0.35f;

    private SkinHeuristic() {}

    /** Fraction of skin-coloured pixels, 0.0 - 1.0. */
    public static float skinRatio(Bitmap bitmap, int sampleSize) {
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, sampleSize, sampleSize, true);
        int width = scaled.getWidth();
        int height = scaled.getHeight();
        int[] pixels = new int[width * height];
        scaled.getPixels(pixels, 0, width, 0, 0, width, height);

        int skinPixels = 0;
        for (int pixel : pixels) {
            int r = Color.red(pixel);
            int g = Color.green(pixel);
            int b = Color.blue(pixel);

            double y  = 0.299 * r + 0.587 * g + 0.114 * b;
            double cb = 128 - 0.168736 * r - 0.331264 * g + 0.5 * b;
            double cr = 128 + 0.5 * r - 0.418688 * g - 0.081312 * b;

            // Broad Cb/Cr bounds covering Fitzpatrick I-VI, not just lighter tones.
            // y > 40 excludes near-black pixels (shadow, hair, dark clothing).
            if (y > 40 && cb > 77 && cb < 135 && cr > 133 && cr < 180) {
                skinPixels++;
            }
        }
        return (float) skinPixels / pixels.length;
    }

    public static float skinRatio(Bitmap bitmap) {
        return skinRatio(bitmap, 128);
    }

    public static boolean isProbablySkin(Bitmap bitmap, float threshold) {
        return skinRatio(bitmap) >= threshold;
    }

    public static boolean isProbablySkin(Bitmap bitmap) {
        return isProbablySkin(bitmap, DEFAULT_THRESHOLD);
    }
}
