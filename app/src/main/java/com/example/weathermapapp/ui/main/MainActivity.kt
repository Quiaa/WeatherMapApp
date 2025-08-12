package com.example.weathermapapp.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.weathermapapp.data.model.UserLocation
import com.example.weathermapapp.data.model.WeatherResponse
import com.example.weathermapapp.databinding.ActivityMainBinding
import com.example.weathermapapp.ui.auth.LoginActivity
import com.example.weathermapapp.ui.map.MapManager
import com.example.weathermapapp.ui.map.MapViewModel
import com.example.weathermapapp.util.Resource
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mapViewModel: MapViewModel by viewModels()
    private lateinit var mapManager: MapManager

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
        ) {
            // Permission granted, delegate the action to the ViewModel.
            mapViewModel.fetchCurrentDeviceLocation()
        } else {
            // Permission denied, show a toast.
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapManager = MapManager(this, binding.mapView)
        val mapClickListener = OnMapClickListener { point ->
            onMapClick(point)
            true
        }
        mapManager.initialize(mapClickListener) {
            mapViewModel.observeUserLocation()
            mapViewModel.observeRealtimeUsersLocations()
        }

        setupClickListeners()
        observeViewModels()
        checkAndRequestLocationPermission()
    }

    private fun onMapClick(point: Point) {
        val location = UserLocation(latitude = point.latitude(), longitude = point.longitude())
        mapViewModel.saveLocation(location)
        mapViewModel.fetchWeatherData(point.latitude(), point.longitude())
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            mapViewModel.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.btnCurrentLocation.setOnClickListener {
            checkAndRequestLocationPermission()
        }
    }

    private fun observeViewModels() {
        // Observer for the current user's saved location from Firestore.
        mapViewModel.userLocation.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    resource.data?.let { location ->
                        mapManager.updateSelectedLocationMarker(location)
                        mapViewModel.fetchWeatherData(location.latitude, location.longitude)
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
                is Resource.Loading -> {
                }
            }
        }

        mapViewModel.myRealtimeLocation.observe(this) { location ->
            location?.let { mapManager.updateMyRealtimeLocationMarker(it) }
        }

        mapViewModel.otherUsersRealtimeLocations.observe(this) { locations ->
            locations?.let { mapManager.updateOtherUsersRealtimeMarkers(it) }
        }

        // Observer for weather data updates.
        mapViewModel.weatherData.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.weatherCard.visibility = View.VISIBLE
                    binding.weatherProgressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.weatherProgressBar.visibility = View.GONE
                    resource.data?.let { updateWeatherUI(it) }
                }
                is Resource.Error -> {
                    binding.weatherProgressBar.visibility = View.GONE
                    Toast.makeText(this, "Weather Error: ${resource.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Observer for the result of a current device location request.
        mapViewModel.currentDeviceLocation.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    resource.data?.let { point ->
                        mapManager.moveCamera(point)
                        // Trigger a map click to also update weather, marker, and saved location.
                        onMapClick(point)
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
                is Resource.Loading -> { /* Optionally show a progress indicator */
                }
            }
        }
    }

    private fun updateWeatherUI(data: WeatherResponse) {
        binding.tvLocationName.text = data.name
        binding.tvWeatherDescription.text =
            data.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "N/A"
        binding.tvTemperature.text = "${data.main.temp.toInt()}°C"

        val iconCode = data.weather.firstOrNull()?.icon
        val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
        Glide.with(this)
            .load(iconUrl)
            .into(binding.ivWeatherIcon)
    }



    private fun checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is granted, start real-time updates.
            mapViewModel.startRealtimeLocationUpdates()
            // We can also fetch the current location for the button click functionality
            mapViewModel.fetchCurrentDeviceLocation()
        } else {
            locationPermissionRequest.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
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
        // Stop location updates to prevent memory leaks and battery drain
        mapViewModel.stopRealtimeLocationUpdates()
    }
}