package com.dermify.dermify

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.Toast
import com.dermify.dermify.curation.ContributionRepository
import com.dermify.dermify.curation.CurationSync

class ProfileActivity : AppCompatActivity() {

    private lateinit var contributionRepo: ContributionRepository
    private var contributionStatusView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val auth = FirebaseAuth.getInstance()

        val tvAvatar = findViewById<TextView>(R.id.tvAvatar)
        val tvName = findViewById<TextView>(R.id.tvName)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        val tvTierBadge = findViewById<TextView>(R.id.tvTierBadge)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnAdmin = findViewById<Button>(R.id.btnAdmin)
        val btnSyncPending = findViewById<Button>(R.id.btnSyncPending)

        // Populate from Session
        val name = Session.displayName ?: "User"
        tvName.text = name
        tvEmail.text = Session.email ?: auth.currentUser?.email ?: ""

        // Avatar initials
        val initials = name.trim().split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
        tvAvatar.text = if (initials.isNotEmpty()) initials else "?"

        // Tier / role badge
        tvTierBadge.text = when {
            Session.isOwner -> "Owner · Tier 3"
            Session.tier == 3 -> "Admin · Tier 3"
            Session.tier == 2 -> "Specialist · Tier 2"
            else -> "Clinician · Tier 1"
        }

        // Admin Panel: visible only to Tier 3
        if (Session.tier == 3) {
            btnAdmin.visibility = View.VISIBLE
            btnAdmin.setOnClickListener {
                startActivity(Intent(this, AdminActivity::class.java))
            }
        }

        // Contribution outbox. Visible to every tier — anyone who consents to
        // contribute is entitled to see whether their image actually left the
        // phone. Uploads themselves are WorkManager's job; this button only
        // nudges the queue for a user who does not want to wait for WiFi.
        val tvContributionStatus = findViewById<TextView>(R.id.tvContributionStatus)
        val switchMetered = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(
            R.id.switchMeteredUpload
        )
        contributionRepo = ContributionRepository(this)
        btnSyncPending.visibility = View.VISIBLE
        btnSyncPending.text = "Sync contributions now"

        switchMetered.isChecked = CurationSync.isMeteredUploadAllowed(this)
        switchMetered.setOnCheckedChangeListener { _, checked ->
            CurationSync.setMeteredUploadAllowed(this, checked)
            Toast.makeText(
                this,
                if (checked) "Uploads may use mobile data" else "Uploads will wait for WiFi",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnSyncPending.setOnClickListener {
            btnSyncPending.isEnabled = false
            lifecycleScope.launch {
                // Clears the give-up counter on anything that exhausted its
                // attempts, then re-triggers the worker.
                contributionRepo.retryFailed()
                CurationSync.requestUpload(this@ProfileActivity)
                btnSyncPending.isEnabled = true
                Toast.makeText(
                    this@ProfileActivity,
                    "Upload queued",
                    Toast.LENGTH_SHORT
                ).show()
                refreshContributionStatus(tvContributionStatus)
            }
        }

        contributionStatusView = tvContributionStatus

        btnLogout.setOnClickListener {
            auth.signOut()
            Session.clear()
            // Send back to Router, which will route to Login (no session now)
            val intent = Intent(this, RouterActivity::class.java)
            // Clear back stack so Back doesn't return into the app
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        val btnTelemedicine = findViewById<Button>(R.id.btnTelemedicine)
        btnTelemedicine.setOnClickListener {
            startActivity(Intent(this, com.dermify.dermify.telemedicine.TelemedicineActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        contributionStatusView?.let { view ->
            lifecycleScope.launch { refreshContributionStatus(view) }
        }
    }

    private suspend fun refreshContributionStatus(view: TextView) {
        val pending = contributionRepo.pendingCount()
        val failed = contributionRepo.failedCount()
        view.text = when {
            pending == 0 && failed == 0 -> "No contributions waiting to upload."
            failed == 0 -> "$pending contribution(s) pending upload."
            pending == 0 -> "$failed contribution(s) failed to upload. Tap sync to retry."
            else -> "$pending pending, $failed failed. Tap sync to retry."
        }
    }
}