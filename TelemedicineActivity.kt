package com.dermify.dermify.telemedicine

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dermify.dermify.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout

class TelemedicineActivity : AppCompatActivity() {

    private lateinit var queueManager: ReferralQueueManager
    private lateinit var adapter: ReferralAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var emptyState: View
    private lateinit var emptyText: android.widget.TextView
    private lateinit var tabs: TabLayout

    // 0 = Pending, 1 = Sent
    private var currentTab = 0

    // Must match android:authorities in the FileProvider manifest entry.
    private val fileProviderAuthority: String
        get() = "$packageName.fileprovider"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_telemedicine)

        queueManager = ReferralQueueManager(this)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        recycler = findViewById(R.id.rvReferrals)
        emptyState = findViewById(R.id.emptyState)
        emptyText = findViewById(R.id.tvEmptyText)
        tabs = findViewById(R.id.tabs)
        recycler.layoutManager = LinearLayoutManager(this)

        adapter = ReferralAdapter(
            cases = emptyList(),
            showActions = true,
            onSend = { case -> sendCase(case) },
            onDelete = { case -> confirmDelete(case) }
        )
        recycler.adapter = adapter

        tabs.addTab(tabs.newTab().setText("Pending"))
        tabs.addTab(tabs.newTab().setText("Sent"))
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.position
                refresh()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        refresh()
    }

    override fun onResume() {
        super.onResume()
        // A case may have been queued from the result screen since we were last shown.
        refresh()
    }

    private fun refresh() {
        val pendingTab = currentTab == 0
        val cases = if (pendingTab) queueManager.getPendingCases() else queueManager.getSentCases()

        adapter.update(cases, showActions = pendingTab)

        val empty = cases.isEmpty()
        emptyState.isVisible = empty
        recycler.isVisible = !empty
        emptyText.text = if (pendingTab) {
            "No pending referrals.\nRefer a case from a scan result to see it here."
        } else {
            "No sent referrals yet.\nCases you send will appear here."
        }
    }

    private fun sendCase(case: ReferralCase) {
        val sent = WhatsAppReferralSender.sendCase(this, case, fileProviderAuthority)
        if (sent) {
            queueManager.markSent(case.caseId)
            refresh()
        }
        // If sending failed, WhatsAppReferralSender already showed a Toast and the
        // case stays PENDING so it can be retried.
    }

    private fun confirmDelete(case: ReferralCase) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete referral?")
            .setMessage("This removes the queued case. It cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                queueManager.deleteCase(case.caseId)
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}