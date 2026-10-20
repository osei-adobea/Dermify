package com.dermify.dermify.data

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import java.io.File
import java.io.FileOutputStream

class SkinScanRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).skinScanDao()

    val allScans: LiveData<List<SkinScan>> = dao.getAllScans()

    suspend fun saveScan(
        bitmap: Bitmap,
        predictedDisease: String,
        confidence: Float,
        severity: String,
        userCorrectedLabel: String?
    ): Long {
        val imagePath = saveBitmapToStorage(bitmap)
        val scan = SkinScan(
            imagePath = imagePath,
            predictedDisease = predictedDisease,
            confidence = confidence,
            severity = severity,
            userCorrectedLabel = userCorrectedLabel
        )
        return dao.insert(scan)
    }

    suspend fun deleteScan(scan: SkinScan) {
        File(scan.imagePath).takeIf { it.exists() }?.delete()
        dao.delete(scan)
    }
    suspend fun saveScanByPath(
        imagePath: String,
        predictedDisease: String,
        confidence: Float,
        severity: String,
        userCorrectedLabel: String?
    ): Long {
        val scan = SkinScan(
            imagePath = imagePath,
            predictedDisease = predictedDisease,
            confidence = confidence,
            severity = severity,
            userCorrectedLabel = userCorrectedLabel
        )
        return dao.insert(scan)
    }

    private fun saveBitmapToStorage(bitmap: Bitmap): String {
        val dir = File(context.filesDir, "scans").apply { mkdirs() }
        val file = File(dir, "scan_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }
}