package com.example.weathermapapp

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

class MainActivity : AppCompatActivity(), OnMapClickListener {

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

        // Initialize the MapManager to handle all map-related operations.
        mapManager = MapManager(this, binding.mapView)
        mapManager.initialize(this) {
            // This lambda block is executed once the map style has been loaded.
            // Now it's safe to load location data.
            mapViewModel.loadUserLocation()
            mapViewModel.fetchAllUsersLocations()
        }

        setupClickListeners()
        observeViewModels()
    }
    override fun onMapClick(point: Point): Boolean {
        mapManager.placeUserMarker(point) // Visually update the marker on the map.
        val location = UserLocation(latitude = point.latitude(), longitude = point.longitude())
        mapViewModel.saveLocation(location) // Save the new location via ViewModel.
        mapViewModel.fetchWeatherData(point.latitude(), point.longitude()) // Fetch weather for the new location.
        return true
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
                        val userPoint = Point.fromLngLat(location.longitude, location.latitude)
                        mapManager.placeUserMarker(userPoint)
                        mapManager.moveCamera(userPoint)
                        mapViewModel.fetchWeatherData(location.latitude, location.longitude)
                    } ?: run {
                        // If no location is saved, move camera to a default location.
                        val defaultPoint = Point.fromLngLat(28.9784, 41.0082) // Istanbul
                        mapManager.moveCamera(defaultPoint, 9.0)
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                    val defaultPoint = Point.fromLngLat(28.9784, 41.0082)
                    mapManager.moveCamera(defaultPoint, 9.0)
                }
                is Resource.Loading -> { /* Optionally handle loading state */
                }
            }
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

        // Observer for all other users' locations.
        mapViewModel.allUsersLocations.observe(this) { resource ->
            if (resource is Resource.Success) {
                resource.data?.let { locations ->
                    val points = locations.map { Point.fromLngLat(it.longitude, it.latitude) }
                    mapManager.placeOtherUsersMarkers(points)
                }
            } else if (resource is Resource.Error) {
                Toast.makeText(this, "Error fetching other users: ${resource.message}", Toast.LENGTH_LONG)
                    .show()
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
    }
}