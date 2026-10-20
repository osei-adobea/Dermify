package com.dermify.dermify.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skin_scans")
data class SkinScan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imagePath: String,
    val predictedDisease: String,
    val confidence: Float,
    val severity: String,
    val userCorrectedLabel: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)