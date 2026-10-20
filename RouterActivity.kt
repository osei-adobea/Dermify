package com.dermify.dermify

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RouterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // First launch? Show onboarding before anything else.
        if (!OnboardingPrefs.isCompleted(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            // Not signed in → go to Login
            goTo(LoginActivity::class.java)
            return
        }

        // Signed in → load tier from Firestore, THEN go to main app
        val uid = currentUser.uid
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    Session.uid = uid
                    Session.email = doc.getString("email")
                    Session.displayName = doc.getString("displayName")
                    Session.tier = (doc.getLong("tier") ?: 1L).toInt()
                    Session.isOwner = doc.getBoolean("isOwner") ?: false
                    Log.d("RouterTest", "Loaded tier=${Session.tier}, isOwner=${Session.isOwner}")
                } else {
                    // Profile doc missing — fall back to safe default
                    Session.uid = uid
                    Session.tier = 1
                    Log.e("RouterTest", "No user doc found, defaulting tier=1")
                }
                goTo(MainActivity::class.java)
            }
            .addOnFailureListener { e ->
                // Network/Firestore error — let them in with most restrictive tier
                Log.e("RouterTest", "Tier load failed", e)
                Session.uid = uid
                Session.tier = 1
                Toast.makeText(this, "Could not load profile, limited access", Toast.LENGTH_SHORT).show()
                goTo(MainActivity::class.java)
            }
    }

    private fun goTo(destination: Class<*>) {
        startActivity(Intent(this, destination))
        finish()
    }
}