package com.dermify.dermify
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.dermify.dermify.databinding.ActivityMainBinding
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Open Profile screen from toolbar icon
        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNav.setOnItemSelectedListener { item ->
            val currentDest = navController.currentDestination?.id

            when (item.itemId) {
                R.id.cameraFragment -> {
                    if (currentDest == R.id.verifyFragment) {
                        showDiscardWarning {
                            navController.popBackStack(
                                R.id.cameraFragment, false
                            )
                        }
                    } else {
                        navController.popBackStack(
                            R.id.cameraFragment, false
                        )
                    }
                    true
                }
                R.id.historyFragment -> {
                    if (currentDest == R.id.verifyFragment) {
                        showDiscardWarning {
                            navController.navigate(R.id.historyFragment)
                        }
                    } else {
                        navController.navigate(R.id.historyFragment)
                    }
                    true
                }
                else -> false
            }
        }

    }

    private fun showDiscardWarning(onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Discard verification?")
            .setMessage(
                "You haven't saved this scan yet. " +
                        "Are you sure you want to leave?"
            )
            .setPositiveButton("Leave") { _, _ -> onConfirm() }
            .setNegativeButton("Stay", null)
            .setCancelable(false)
            .show()
    }
}
