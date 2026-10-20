package com.dermify.dermify

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()

        val etDisplayName = findViewById<EditText>(R.id.etDisplayName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etAccessCode = findViewById<EditText>(R.id.etAccessCode)
        val btnSignup = findViewById<Button>(R.id.btnSignup)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)

        btnSignup.setOnClickListener {
            val name = etDisplayName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val code = etAccessCode.text.toString().trim()

            // Basic validation
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill name, email, and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tier = TierCodes.tierForCode(code)

            progressBar.visibility = View.VISIBLE
            btnSignup.isEnabled = false

            // 1) Create the Auth account
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid
                    if (uid == null) {
                        progressBar.visibility = View.GONE
                        btnSignup.isEnabled = true
                        Toast.makeText(this, "Signup failed: no user id", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // 2) Save the user profile to Firestore
                    val user = User(
                        uid = uid,
                        email = email,
                        displayName = name,
                        tier = tier,
                        createdAt = System.currentTimeMillis()
                    )

                    db.collection("users").document(uid)
                        .set(user)
                        .addOnSuccessListener {
                            progressBar.visibility = View.GONE
                            Log.d("SignupTest", "User created, tier=$tier")
                            Toast.makeText(this, "Account created! Tier $tier", Toast.LENGTH_SHORT).show()
                            // Go to main app
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            progressBar.visibility = View.GONE
                            btnSignup.isEnabled = true
                            Log.e("SignupTest", "Firestore save failed", e)
                            Toast.makeText(this, "Profile save failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    btnSignup.isEnabled = true
                    Log.e("SignupTest", "Auth signup failed", e)
                    Toast.makeText(this, "Signup failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        tvGoToLogin.setOnClickListener {
            // LoginActivity comes in the next step; safe to leave for now
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
