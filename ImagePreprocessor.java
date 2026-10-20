package com.dermify.dermify.ml;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Converts a Bitmap into the input format a TFLite model expects.
 *
 * IMPORTANT - two different schemes, do not mix them up:
 *
 *  bitmapToByteBufferRaw()  -> raw 0-255 floats.  USE FOR THE DISEASE MODEL.
 *      The Keras EfficientNetB0 export has Rescaling(1/255) + Normalization
 *      baked in as its first layers, so it expects RAW pixel values.
 *      Verified empirically: raw input gives mean top-confidence 0.514,
 *      while pre-dividing by 255 collapses it to 0.333 (chance = 0.25).
 *
 *  bitmapToByteBuffer()     -> [0,1] floats.  For the skin-gatekeeper model,
 *      which was trained separately with external rescaling.
 */
public final class ImagePreprocessor {

    public static final int IMAGE_SIZE = 224;

    private ImagePreprocessor() {}

    /** RAW 0-255. Use for the 4-class disease model / HeatmapGenerator. */
    public static ByteBuffer bitmapToByteBufferRaw(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, IMAGE_SIZE, IMAGE_SIZE, true);

        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * IMAGE_SIZE * IMAGE_SIZE * 3);
        buffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[IMAGE_SIZE * IMAGE_SIZE];
        resized.getPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE);

        for (int pixel : pixels) {
            buffer.putFloat((float) ((pixel >> 16) & 0xFF)); // R
            buffer.putFloat((float) ((pixel >> 8) & 0xFF));  // G
            buffer.putFloat((float) (pixel & 0xFF));         // B
        }
        buffer.rewind();
        return buffer;
    }

    /** [0,1] normalized. Use for the skin-gatekeeper model only. */
    public static ByteBuffer bitmapToByteBuffer(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, IMAGE_SIZE, IMAGE_SIZE, true);

        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * IMAGE_SIZE * IMAGE_SIZE * 3);
        buffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[IMAGE_SIZE * IMAGE_SIZE];
        resized.getPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE);

        for (int pixel : pixels) {
            buffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f);
            buffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);
            buffer.putFloat((pixel & 0xFF) / 255.0f);
        }
        buffer.rewind();
        return buffer;
    }
}
