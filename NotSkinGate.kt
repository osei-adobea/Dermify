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
 * A cheap "is this even worth classifying?" gate that runs BEFORE SkinClassifier.
 *
 * It uses a generic pretrained ImageNet MobileNet model (NO training required).
 *
 * WHY IT WORKS:
 *   ImageNet knows ~1000 everyday objects (laptop, banana, dog, keyboard...) but has
 *   NO skin-condition classes. So:
 *     - A photo of a real object -> MobileNet is CONFIDENT about some class -> reject.
 *     - A skin close-up          -> matches nothing well, low max prob      -> pass.
 *
 * WHAT IT CATCHES:  obvious non-skin objects (shoe, wall poster, pet, phone).
 * WHAT IT MISSES:   healthy-vs-diseased skin (both pass -> that's the job of the
 *                   confidence threshold on SkinClassifier), and featureless same-tone
 *                   surfaces (a plain beige wall may score low and slip through too).
 *
 * Treat it as a mitigation that removes the worst failures, not a guarantee.
 */
class NotSkinGate(context: Context) {

    private val interpreter: Interpreter

    init {
        interpreter = Interpreter(loadModelFile(context, MODEL_FILE))
    }

    /**
     * Returns true if the image looks like a recognizable non-skin object and should
     * be rejected before reaching the skin classifier.
     */
    fun isLikelyNotSkin(bitmap: Bitmap): Boolean {
        val resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val input = preprocess(resized)

        val output = Array(1) { FloatArray(NUM_CLASSES) }
        interpreter.run(input, output)

        val scores = output[0]
        var maxProb = scores[0]
        for (v in scores) if (v > maxProb) maxProb = v

        // Confident about some ImageNet object => it's a recognizable thing, not skin.
        return maxProb >= OBJECT_CONFIDENCE_THRESHOLD
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
            // MobileNet v1 FLOAT preprocessing: scale each channel to [-1, 1].
            buffer.putFloat((r - 127.5f) / 127.5f)
            buffer.putFloat((g - 127.5f) / 127.5f)
            buffer.putFloat((b - 127.5f) / 127.5f)
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
        private const val MODEL_FILE = "mobilenet_v1.tflite"  // app/src/main/assets/
        private const val INPUT_SIZE = 224                    // MobileNet v1 1.0_224 input
        private const val NUM_CLASSES = 1001                  // 1000 ImageNet + 1 background
        // TUNE THIS on real captures. Higher = more lenient (fewer rejections).
        private const val OBJECT_CONFIDENCE_THRESHOLD = 0.40f
    }
}