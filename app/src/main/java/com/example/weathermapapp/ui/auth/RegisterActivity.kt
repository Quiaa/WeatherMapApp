package com.example.weathermapapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.weathermapapp.MainActivity
import com.example.weathermapapp.databinding.ActivityRegisterBinding
import com.example.weathermapapp.util.Resource

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels() // Get a reference to the ViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Basic validation
            if (email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.register(email, password)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        // TODO: Add click listener for tvGoToLogin to open LoginActivity
    }

    private fun observeViewModel() {
        viewModel.registrationState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Show progress bar
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    // Hide progress bar
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                    // Navigate to MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Finish this activity so the user can't come back
                }
                is Resource.Error -> {
                    // Hide progress bar and show error message
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Error: ${resource.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}