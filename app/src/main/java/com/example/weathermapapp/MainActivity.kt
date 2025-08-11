package com.example.weathermapapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.weathermapapp.databinding.ActivityMainBinding
import com.example.weathermapapp.ui.auth.AuthViewModel
import com.example.weathermapapp.ui.auth.LoginActivity
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Inflate the layout using ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the map
        initializeMap()

        // Setup logout button listener
        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
            // Navigate back to Login screen
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun initializeMap() {
        val mapboxMap = binding.mapView.getMapboxMap()
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS)

        val initialCameraOptions = CameraOptions.Builder()
            .center(com.mapbox.geojson.Point.fromLngLat(28.9784, 41.0082)) // Istanbul coordinates
            .zoom(9.0)
            .build()
        mapboxMap.setCamera(initialCameraOptions)
    }

    // --- MapView Lifecycle Management ---
    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }
}