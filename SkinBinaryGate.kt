package com.dermify.dermify.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Third CaptureGate stage — a domain-trained binary skin detector, run AFTER
 * SkinHeuristic and NotSkinGate as a backstop for non-skin that slips past them.
 *
 * Model: skin_gate.tflite  (Hannah, trained on everyday non-skin images)
 *   input  [1,224,224,3] float32, NOT quantized
 *   output [1,1] sigmoid
 *
 * Empirically pinned on a 6-image probe (Python & Android TFLite are bit-identical,
 * so these transfer exactly):
 *   preprocessing = pixel / 255f  -> [0,1]     (raw 0-255 fails; [-1,1] separates worse)
 *   output HIGH  = SKIN     (dermatology refs scored 0.87-1.00)
 *   output LOW   = NOT-skin (wood surfaces scored 0.07-0.21)
 *   => reject when skinScore < THRESHOLD
 *
 * Returns true = "reject", matching NotSkinGate.isLikelyNotSkin so CaptureGate can
 * call all three gates with the same idiom. Run off the main thread (as the others are).
 */
class SkinBinaryGate(context: Context) {

    private val interpreter: Interpreter

    init {
        interpreter = Interpreter(loadModelFile(context, MODEL_FILE))
    }

    /** true if the binary detector thinks this is NOT skin and it should be rejected. */
    fun isLikelyNotSkin(bitmap: Bitmap): Boolean {
        val resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val input = preprocess(resized)

        val output = Array(1) { FloatArray(1) }   // single sigmoid, not 1001 classes
        interpreter.run(input, output)

        val skinScore = output[0][0]               // HIGH = skin, LOW = not-skin
        return skinScore < SKIN_CONFIDENCE_THRESHOLD
    }

    private fun preprocess(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        for (pixel in pixels) {
            val r = (pixel shr 16 and 0xFF)
            val g = (pixel shr 8 and 0xFF)
            val b = (pixel and 0xFF)
            // skin_gate FLOAT preprocessing: scale each channel to [0, 1].
            buffer.putFloat(r / 255f)
            buffer.putFloat(g / 255f)
            buffer.putFloat(b / 255f)
        }
        buffer.rewind()
        return buffer
    }

    private fun loadModelFile(context: Context, name: String): MappedByteBuffer {
        val afd = context.assets.openFd(name)
        FileInputStream(afd.fileDescriptor).use { input ->
            return input.channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
        }
    }

    fun close() = interpreter.close()

    companion object {
        private const val MODEL_FILE = "skin_gate.tflite"   // app/src/main/assets/
        private const val INPUT_SIZE = 224
        // TUNE on real captures. skin>=0.87, non-skin<=0.21 on the probe, so 0.50 is mid-gap.
        // Lower if real skin photos get falsely rejected; raise to catch more borderline non-skin.
        private const val SKIN_CONFIDENCE_THRESHOLD = 0.50f
    }
}