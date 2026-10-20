package com.dermify.dermify

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class AdminActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: UserAdminAdapter

    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        val recycler = findViewById<RecyclerView>(R.id.recyclerUsers)

        // Set up the list
        adapter = UserAdminAdapter(mutableListOf()) { user, newTier ->
            changeTier(user, newTier)
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        loadUsers()
    }

    private fun loadUsers() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        db.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                progressBar.visibility = View.GONE
                val users = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)
                }
                if (users.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                } else {
                    adapter.updateUsers(users)
                    findViewById<TextView>(R.id.tvUserCount).text = "${users.size} members"
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text = "Failed to load users: ${e.message}"
                Toast.makeText(this, "Load failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun changeTier(user: User, newTier: Int) {
        if (user.tier == newTier) {
            Toast.makeText(this, "${user.email} is already Tier $newTier", Toast.LENGTH_SHORT).show()
            return
        }
        // Protect admins: only the owner can demote a Tier 3 admin
        if (user.tier == 3 && newTier < 3 && !Session.isOwner) {
            Toast.makeText(this, "Only the owner can demote an admin", Toast.LENGTH_SHORT).show()
            return
        }

        // The owner's account can never be demoted by anyone
        if (user.isOwner && newTier < 3) {
            Toast.makeText(this, "The owner cannot be demoted", Toast.LENGTH_SHORT).show()
            return
        }
        db.collection("users")
            .document(user.uid)
            .update("tier", newTier)
            .addOnSuccessListener {
                adapter.updateUserTier(user.uid, newTier)
                Toast.makeText(this, "${user.email} → Tier $newTier", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}