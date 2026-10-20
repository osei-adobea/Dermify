package com.dermify.dermify.ml

/**
 * Maps model confidence to a severity label.
 * Lifted out of SkinClassifier.getSeverity() - thresholds unchanged.
 */
object Severity {

    fun fromConfidence(confidence: Float): String = when {
        confidence > 0.85f -> "Severe"
        confidence > 0.65f -> "Moderate"
        else -> "Mild"
    }
}