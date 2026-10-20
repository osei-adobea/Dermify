package com.dermify.dermify

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class OnboardingTermsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_onboarding_terms, container, false)
        view.findViewById<TextView>(R.id.tvTerms).text = TERMS_TEXT
        return view
    }

    companion object {
        private val TERMS_TEXT = """
            1. Medical Disclaimer. Dermify is an AI-powered screening tool intended to support, not replace, professional medical judgment. It does not provide a medical diagnosis. Results are preliminary and may be inaccurate. Always consult a qualified healthcare professional for diagnosis and treatment. Do not disregard or delay seeking professional medical advice because of information provided by this app.

            2. No Medical Relationship. Use of Dermify does not create a doctor–patient relationship between you and the app or its providers.

            3. Image & Data Collection. When a result is verified or corrected by an authorized user, the associated image and label may be stored and used to improve the accuracy of Dermify's AI model. Images flagged as face scans are excluded from this dataset. By using the app, you consent to this use of submitted images and data.

            4. Appropriate Use. You agree to use Dermify only for its intended skin-screening purpose and not to upload unlawful, harmful, or unauthorized content. You are responsible for obtaining any necessary consent before scanning another person.

            5. Accuracy & Liability. Dermify is provided "as is" without warranties of any kind. The providers are not liable for any decisions made, or harm arising, from reliance on the app's output.

            6. Data Security. Reasonable measures are taken to protect your data, but no system is completely secure. Use of the app is at your own risk.

            7. Changes. These terms may be updated. Continued use after changes constitutes acceptance.

            By tapping "I agree & continue," you confirm that you have read, understood, and accepted these Terms of Use.
        """.trimIndent()
    }
}