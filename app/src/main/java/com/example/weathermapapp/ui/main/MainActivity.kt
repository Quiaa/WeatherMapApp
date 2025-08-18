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
import com.example.weathermapapp.databinding.ActivityMainBinding
import com.example.weathermapapp.ui.auth.LoginActivity
import com.example.weathermapapp.ui.map.MapManager
import com.example.weathermapapp.ui.main.WeatherUIData
import com.example.weathermapapp.ui.map.MapViewModel
import com.example.weathermapapp.util.Resource
import com.example.weathermapapp.util.TimeUtils
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import dagger.hilt.android.AndroidEntryPoint
import androidx.appcompat.app.AlertDialog
import com.example.weathermapapp.data.model.webrtc.NSDataModel
import com.example.weathermapapp.ui.webrtc.VideoCallActivity

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
            mapViewModel.fetchCurrentDeviceLocation()
        } else {
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
        }

        binding.btnCurrentLocation.setOnClickListener {
            checkAndRequestLocationPermission()
        }

        binding.btnRefresh.setOnClickListener {
            mapViewModel.forceRefreshWeatherData()
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

        mapViewModel.weatherUIState.observe(this) { uiState ->
            updateWeatherUI(uiState)
        }

        // Observer for the result of a current device location request.
        mapViewModel.currentDeviceLocation.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    resource.data?.let { point ->
                        mapManager.moveCamera(point)
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

        mapViewModel.logoutComplete.observe(this) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        mapViewModel.incomingCall.observe(this) { model ->
            model?.let {
                showIncomingCallDialog(it)
                mapViewModel.clearIncomingCallEvent() // Clear the event.
            }
        }
    }

    private fun showIncomingCallDialog(model: NSDataModel) {
        // It would be better to fetch and display the username from Firestore, but for now we're just showing the ID.
        AlertDialog.Builder(this)
            .setTitle("Incoming Video Call")
            .setMessage("${model.sender} is calling you.")
            .setPositiveButton("Accept") { dialog, _ ->
                val intent = Intent(this, VideoCallActivity::class.java).apply {
                    putExtra("target", model.sender)
                    putExtra("isCaller", false) // This user is the caller.
                }
                startActivity(intent)
                dialog.dismiss()
            }
            .setNegativeButton("Decline") { dialog, _ ->
                // TODO: Send a signal for decline
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun updateWeatherUI(uiState: WeatherUIData) {
        binding.weatherCard.visibility = uiState.weatherCardVisibility
        binding.weatherProgressBar.visibility = uiState.progressBarVisibility
        binding.cacheStatusContainer.visibility = uiState.cacheStatusVisibility

        binding.tvLocationName.text = uiState.locationName
        binding.tvWeatherDescription.text = uiState.description
        binding.tvTemperature.text = uiState.temperature
        binding.tvCacheStatus.text = uiState.cacheStatus

        if (uiState.iconUrl.isNotEmpty()) {
            Glide.with(this)
                .load(uiState.iconUrl)
                .into(binding.ivWeatherIcon)
        }
    }

    private fun checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            mapViewModel.startRealtimeLocationUpdates()
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
        mapViewModel.stopRealtimeLocationUpdates()
    }
}