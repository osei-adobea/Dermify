package com.dermify.dermify

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var dotsContainer: LinearLayout
    private lateinit var btnNext: MaterialButton
    private lateinit var tvSkip: TextView

    private val pageCount = 4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        dotsContainer = findViewById(R.id.dotsContainer)
        btnNext = findViewById(R.id.btnNext)
        tvSkip = findViewById(R.id.tvSkip)

        viewPager.adapter = OnboardingPagerAdapter(this)

        setupDots()
        updateUi(0)

        // React to swipes
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateUi(position)
            }
        })

        btnNext.setOnClickListener {
            if (viewPager.currentItem < pageCount - 1) {
                viewPager.currentItem += 1   // advance with built-in slide animation
            } else {
                finishOnboarding()           // last page → done
            }
        }

        tvSkip.setOnClickListener {
            // Skip jumps to the last (Terms) page rather than bypassing terms
            viewPager.currentItem = pageCount - 1
        }
    }

    private fun setupDots() {
        for (i in 0 until pageCount) {
            val dot = View(this)
            val params = LinearLayout.LayoutParams(dpToPx(8), dpToPx(8))
            params.marginStart = dpToPx(4)
            params.marginEnd = dpToPx(4)
            dot.layoutParams = params
            dot.setBackgroundResource(R.drawable.dot_inactive)
            dotsContainer.addView(dot)
        }
    }

    private fun updateUi(position: Int) {
        // Update dots
        for (i in 0 until dotsContainer.childCount) {
            val dot = dotsContainer.getChildAt(i)
            val params = dot.layoutParams as LinearLayout.LayoutParams
            if (i == position) {
                params.width = dpToPx(22)   // active dot is a pill
                dot.setBackgroundResource(R.drawable.dot_active)
            } else {
                params.width = dpToPx(8)
                dot.setBackgroundResource(R.drawable.dot_inactive)
            }
            dot.layoutParams = params
        }

        // Last page → button becomes the agree action, hide Skip
        if (position == pageCount - 1) {
            btnNext.text = "I agree & continue"
            tvSkip.visibility = View.INVISIBLE
        } else {
            btnNext.text = "Next"
            tvSkip.visibility = View.VISIBLE
        }
    }

    private fun finishOnboarding() {
        // Mark onboarding complete, then go to Login
        OnboardingPrefs.setCompleted(this, true)
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}