package com.dermify.dermify.ui.consent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.dermify.dermify.R
import com.dermify.dermify.curation.ConsentPolicy
import com.dermify.dermify.curation.ContributionRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Opt-in only. Nothing is pre-selected, and declining is a first-class outcome
 * — the user already has their result by the time they reach this screen.
 *
 * Consenting queues the contribution locally and returns immediately. The
 * upload happens later under WorkManager, so a user with no connectivity is
 * never left waiting on a spinner.
 */
class ConsentFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_consent, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imagePath = arguments?.getString("imagePath") ?: ""
        val label = arguments?.getString("label") ?: "Unknown"
        val confidence = arguments?.getFloat("confidence") ?: 0f

        view.findViewById<TextView>(R.id.tvConsentLabel).text = label
        view.findViewById<TextView>(R.id.tvConsentTerms).text = ConsentPolicy.CONSENT_TEXT
        view.findViewById<TextView>(R.id.tvConsentVersion).text =
            "Consent terms ${ConsentPolicy.CURRENT_VERSION}"

        val btnAccept = view.findViewById<Button>(R.id.btnConsentAccept)
        val btnDecline = view.findViewById<Button>(R.id.btnConsentDecline)

        btnAccept.setOnClickListener {
            btnAccept.isEnabled = false
            btnDecline.isEnabled = false
            btnAccept.text = "Preparing image…"

            lifecycleScope.launch {
                val result = ContributionRepository(requireContext())
                    .enqueue(imagePath, label, confidence)

                if (!isAdded) return@launch

                when (result) {
                    is ContributionRepository.EnqueueResult.Queued -> {
                        Toast.makeText(
                            requireContext(),
                            "Thank you — queued for upload",
                            Toast.LENGTH_SHORT
                        ).show()
                        returnToCamera()
                    }

                    // A face was found, or the image could not be stripped of
                    // metadata. Either way it is not contributed, and the user
                    // is told the reason rather than left to assume it worked.
                    is ContributionRepository.EnqueueResult.Rejected -> {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Not contributed")
                            .setMessage(result.userMessage)
                            .setPositiveButton("OK") { _, _ -> returnToCamera() }
                            .setCancelable(false)
                            .show()
                    }

                    is ContributionRepository.EnqueueResult.Failed -> {
                        Toast.makeText(
                            requireContext(),
                            "Couldn't prepare the image: ${result.userMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                        btnAccept.isEnabled = true
                        btnDecline.isEnabled = true
                        btnAccept.text = getString(R.string.consent_accept)
                    }
                }
            }
        }

        btnDecline.setOnClickListener { returnToCamera() }
    }

    private fun returnToCamera() {
        findNavController().popBackStack(R.id.cameraFragment, false)
    }
}
