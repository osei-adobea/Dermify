package com.dermify.dermify.ui.result

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController
import com.dermify.dermify.R
import com.dermify.dermify.Session
import com.dermify.dermify.databinding.FragmentResultBinding
import com.dermify.dermify.ml.ModelCache
import com.dermify.dermify.ml.Severity
import com.dermify.dermify.telemedicine.ReferralImageWriter
import com.dermify.dermify.telemedicine.ReferralQueueManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    private var originalBitmap: Bitmap? = null
    private var heatmapBitmap: Bitmap? = null
    private var disease: String = "Unknown"
    private var confidence: Float = 0f
    private var severity: String = "Mild"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imagePath = arguments?.getString("imagePath") ?: ""
        val isFaceScan = arguments?.getBoolean("isFaceScan", false) ?: false

        val bitmap = BitmapFactory.decodeFile(imagePath)
        originalBitmap = bitmap
        binding.progressBar.isVisible = true
        binding.tvDisease.text = "Analysing..."

        if (bitmap == null) {
            binding.progressBar.isVisible = false
            binding.tvDisease.text = "Image unavailable"
            binding.btnRefer.isVisible = false
            return
        }

        // Inference + heatmap run on a background thread so the UI (and the spinner)
        // stays responsive. The model is reused from ModelCache, not reloaded per scan.
        lifecycleScope.launch {
            val result = try {
                withContext(Dispatchers.Default) {
                    val generator = ModelCache.heatmapGenerator(requireContext())
                        ?: return@withContext null
                    generator.predictWithHeatmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            // Back on the main thread here (lifecycleScope defaults to Main).
            if (_binding == null) return@launch  // view destroyed while we were working

            if (result == null) {
                binding.progressBar.isVisible = false
                binding.tvDisease.text = "Analysis failed"
                binding.btnRefer.isVisible = false
                return@launch
            }

            disease = result.predictedClassName
            confidence = result.confidence
            severity = Severity.fromConfidence(confidence)
            heatmapBitmap = result.heatmapOverlay

            binding.ivGradCam.setImageBitmap(result.heatmapOverlay)

            val confidencePct = (confidence * 100).toInt()
            binding.progressBar.isVisible = false
            binding.tvDisease.text = disease.split(" ").joinToString(" ") {
                it.replaceFirstChar { c -> c.uppercase() }
            }
            binding.tvConfidence.text = "Confidence: $confidencePct%"
            binding.tvSeverity.text = "Severity: $severity"
            binding.tvFaceScanBadge.isVisible = isFaceScan
            binding.tvLowConfidence.isVisible = confidence < 0.50f

            binding.btnVerify.isVisible = Session.tier >= 2
            binding.btnVerify.setOnClickListener {
                val bundle = Bundle().apply {
                    putString("imagePath", imagePath)
                    putString("predictedDisease", disease)
                    putFloat("confidence", confidence)
                    putString("severity", severity)
                    putBoolean("isFaceScan", isFaceScan)
                }
                findNavController().navigate(R.id.action_result_to_verify, bundle)
            }

            binding.btnRefer.setOnClickListener { showReferralNoteDialog() }
        }
    }

    private fun showReferralNoteDialog() {
        val input = EditText(requireContext()).apply {
            hint = "e.g. lesion present 3 weeks, itching, no fever"
            minLines = 3
            gravity = Gravity.TOP or Gravity.START
        }
        val container = FrameLayout(requireContext()).apply {
            val pad = (24 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Refer to Specialist")
            .setMessage("Add a clinical note (optional). No patient name or contact details.")
            .setView(container)
            .setPositiveButton("Queue referral") { _, _ ->
                queueReferral(input.text?.toString()?.trim().orEmpty())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun queueReferral(note: String) {
        val original = originalBitmap
        if (original == null) {
            Toast.makeText(requireContext(), "No image to refer", Toast.LENGTH_SHORT).show()
            return
        }

        val queueManager = ReferralQueueManager(requireContext())
        val caseId = UUID.randomUUID().toString().substring(0, 8)

        val composite = ReferralImageWriter.writeComposite(
            requireContext(), original, heatmapBitmap, caseId
        )

        if (composite == null) {
            Toast.makeText(requireContext(), "Could not prepare referral image", Toast.LENGTH_SHORT).show()
            return
        }

        queueManager.addCase(
            composite.absolutePath,
            disease,
            confidence,
            severity,
            note,
            "facility_001"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Referral queued")
            .setMessage(
                "Case saved and ready to send. Open the Telemedicine screen to " +
                        "send it to a specialist when you have connectivity."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}