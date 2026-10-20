package com.dermify.dermify

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class OnboardingPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> OnboardingPageFragment.newInstance(
                iconRes = R.drawable.img_onboarding_1,
                title = "Scan skin conditions instantly",
                body = "Point your camera at the affected area and get an instant AI assessment across four common conditions — Acne, Eczema, Fungal Infection, and Scabies.\n\nA screening aid, not a diagnosis."
            )
            1 -> OnboardingPageFragment.newInstance(
                iconRes = R.drawable.img_onboarding_2,
                title = "How your scans are used",
                body = "Verified scans may be used to improve Dermify's AI over time.\n\nScanning a face? Turn on Face Scan before capturing — face images are excluded from training.\n\nYou stay in control of what's shared."
            )
            2 -> OnboardingPageFragment.newInstance(
                iconRes = R.drawable.img_onboarding_3,
                title = "Your role in Dermify",
                body = "Clinician — scan and view results.\n\nSpecialist — verify and correct results.\n\nAdmin — manage users and access."
            )
            else -> OnboardingTermsFragment()
        }
    }
}