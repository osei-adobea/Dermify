package com.dermify.dermify.ui.verify

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.dermify.dermify.R
import com.dermify.dermify.data.SkinScanRepository
import com.dermify.dermify.databinding.FragmentVerifyBinding
import kotlinx.coroutines.launch
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog

class VerifyFragment : Fragment() {
    companion object {
        private const val CONFIDENCE_THRESHOLD = 0.70f
    }

    private var _binding: FragmentVerifyBinding? = null
    private val binding get() = _binding!!

    private val diseaseOptions = listOf(
        "Acne", "Actinic Keratosis", "Atopic Dermatitis",
        "Basal Cell Carcinoma", "Dermatofibroma", "Eczema",
        "Melanoma", "Nevus (Mole)", "Psoriasis", "Rosacea",
        "Seborrheic Keratosis", "Squamous Cell Carcinoma",
        "Tinea (Ringworm)", "Urticaria (Hives)", "Vitiligo",
        "Other / Unknown"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerifyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Warn user if they try to leave without confirming or correcting
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            AlertDialog.Builder(requireContext())
                .setTitle("Discard scan?")
                .setMessage("You haven't confirmed this result. If you leave now, the scan won't be saved.")
                .setPositiveButton("Discard") { _, _ ->
                    findNavController().popBackStack(R.id.cameraFragment, false)
                }
                .setNegativeButton("Stay", null)
                .setCancelable(false)
                .show()
        }

        val imagePath = arguments?.getString("imagePath") ?: ""
        val predictedDisease = arguments?.getString("predictedDisease") ?: "Unknown"
        val confidence = arguments?.getFloat("confidence") ?: 0f
        val severity = arguments?.getString("severity") ?: "Unknown"
        val isFaceScan = arguments?.getBoolean("isFaceScan", false) ?: false

        binding.tvDisease.text = predictedDisease

        // Set up spinner
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_spinner_selected,
            diseaseOptions
        ).also {
            it.setDropDownViewResource(R.layout.item_spinner_dropdown)
        }
        binding.spinnerCorrection.adapter = adapter

        val index = diseaseOptions.indexOfFirst {
            it.equals(predictedDisease, ignoreCase = true)
        }.takeIf { it >= 0 } ?: 0
        binding.spinnerCorrection.setSelection(index)
        // Disable Submit Correction if user picks the same diagnosis as the prediction
        binding.spinnerCorrection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = diseaseOptions[position]
                val isSame = selected.equals(predictedDisease, ignoreCase = true)
                binding.btnSubmitCorrection.isEnabled = !isSame
                binding.btnSubmitCorrection.alpha = if (isSame) 0.5f else 1.0f
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Toggle correction panel
        binding.btnCorrect.setOnClickListener {
            val visible = binding.layoutCorrection.visibility == View.VISIBLE
            binding.layoutCorrection.visibility =
                if (visible) View.GONE else View.VISIBLE
            binding.btnCorrect.text =
                if (visible) "Correct the Label" else "Cancel"
        }

        // Confirm — AI was correct
        binding.btnConfirm.setOnClickListener {
            saveScan(imagePath, predictedDisease, confidence, severity, correctedLabel = null, isFaceScan = isFaceScan)
        }

        // Submit correction
        binding.btnSubmitCorrection.setOnClickListener {
            val selected = binding.spinnerCorrection.selectedItem as String
            saveScan(imagePath, predictedDisease, confidence, severity, correctedLabel = selected, isFaceScan = isFaceScan)
        }
    }

    private fun saveScan(
        imagePath: String,
        disease: String,
        confidence: Float,
        severity: String,
        correctedLabel: String?,
        isFaceScan: Boolean
    ) {
        val repository = SkinScanRepository(requireContext())
        lifecycleScope.launch {
            val saveSucceeded = try {
                repository.saveScanByPath(
                    imagePath = imagePath,
                    predictedDisease = disease,
                    confidence = confidence,
                    severity = severity,
                    userCorrectedLabel = correctedLabel
                )
                true
            } catch (e: Exception) {
                android.util.Log.e("VerifyFragment", "Save failed", e)
                Toast.makeText(
                    requireContext(),
                    "Failed to save scan: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                false
            }

            if (!saveSucceeded) return@launch

            Toast.makeText(
                requireContext(),
                "Scan saved!",
                Toast.LENGTH_SHORT
            ).show()

            // Face scans NEVER enter the training dataset, even when verified
            val shouldAskConsent = !isFaceScan
            val finalLabel = correctedLabel ?: disease

            if (shouldAskConsent) {
                val bundle = Bundle().apply {
                    putString("imagePath", imagePath)
                    putString("label", finalLabel)
                    putFloat("confidence", confidence)
                }
                findNavController().navigate(R.id.action_verify_to_consent, bundle)
            } else {
                findNavController().popBackStack(R.id.cameraFragment, false)
            }
            }

        }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}