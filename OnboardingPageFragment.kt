package com.dermify.dermify

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class OnboardingPageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_onboarding_page, container, false)

        val iconRes = arguments?.getInt(ARG_ICON) ?: android.R.drawable.ic_menu_camera
        val title = arguments?.getString(ARG_TITLE) ?: ""
        val body = arguments?.getString(ARG_BODY) ?: ""

        view.findViewById<ImageView>(R.id.ivIcon).setImageResource(iconRes)
        view.findViewById<TextView>(R.id.tvTitle).text = title
        view.findViewById<TextView>(R.id.tvBody).text = body

        return view
    }

    companion object {
        private const val ARG_ICON = "icon"
        private const val ARG_TITLE = "title"
        private const val ARG_BODY = "body"

        fun newInstance(iconRes: Int, title: String, body: String): OnboardingPageFragment {
            return OnboardingPageFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ICON, iconRes)
                    putString(ARG_TITLE, title)
                    putString(ARG_BODY, body)
                }
            }
        }
    }
}