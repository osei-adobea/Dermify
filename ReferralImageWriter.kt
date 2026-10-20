package com.dermify.dermify.telemedicine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream

/**
 * Builds the single image attached to a WhatsApp referral: original and
 * heatmap side by side.
 *
 * One composite rather than two attachments because ACTION_SEND_MULTIPLE
 * frequently drops EXTRA_TEXT, which would silently lose the clinical summary.
 */
object ReferralImageWriter {

    private const val PANEL = 512
    private const val LABEL_HEIGHT = 56
    private const val GAP = 8
    private const val JPEG_QUALITY = 85

    fun writeComposite(
        context: Context,
        original: Bitmap,
        heatmap: Bitmap?,
        caseId: String
    ): File? {
        return try {
            val panels = if (heatmap != null) 2 else 1
            val width = PANEL * panels + GAP * (panels - 1)
            val height = PANEL + LABEL_HEIGHT

            val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            canvas.drawColor(Color.WHITE)

            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 30f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            drawPanel(canvas, original, 0, "Original", labelPaint)
            if (heatmap != null) {
                drawPanel(canvas, heatmap, PANEL + GAP, "AI focus (heatmap)", labelPaint)
            }

            val dir = File(context.filesDir, "referrals").apply { mkdirs() }
            val file = File(dir, "referral_$caseId.jpg")
            FileOutputStream(file).use { fos ->
                out.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)
            }
            out.recycle()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun drawPanel(canvas: Canvas, src: Bitmap, xOffset: Int, label: String, paint: Paint) {
        val scaled = Bitmap.createScaledBitmap(src, PANEL, PANEL, true)
        canvas.drawBitmap(scaled, xOffset.toFloat(), 0f, null)
        canvas.drawText(
            label,
            (xOffset + PANEL / 2).toFloat(),
            (PANEL + 40).toFloat(),
            paint
        )
        if (scaled != src) scaled.recycle()
    }
}