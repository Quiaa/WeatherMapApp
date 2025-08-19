package com.example.weathermapapp.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.weathermapapp.data.model.UserLocation
import com.example.weathermapapp.databinding.ActivityMainBinding
import com.example.weathermapapp.ui.auth.LoginActivity
import com.example.weathermapapp.ui.map.MapManager
import com.example.weathermapapp.ui.map.MapViewModel
import com.example.weathermapapp.ui.webrtc.IncomingCallService
import com.example.weathermapapp.util.Resource
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mapViewModel: MapViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var mapManager: MapManager

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        mainViewModel.onPermissionResult(permissions)
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

        mainViewModel.onMainActivityReady()
    }

    private fun startIncomingCallService() {
        val intent = Intent(this, IncomingCallService::class.java)
        startService(intent)
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
            mainViewModel.onMainActivityReady()
        }

        binding.btnRefresh.setOnClickListener {
            mapViewModel.forceRefreshWeatherData()
        }
    }

    private fun observeViewModels() {
        mainViewModel.permissionRequest.observe(this) { event ->
            event.getContentIfNotHandled()?.let { permissions ->
                permissionRequest.launch(permissions)
            }
        }

        mainViewModel.startLocationUpdates.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                mapViewModel.startRealtimeLocationUpdates()
                mapViewModel.fetchCurrentDeviceLocation()
            }
        }

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
            stopIncomingCallService()
            finish()
        }
    }

    private fun stopIncomingCallService() {
        val intent = Intent(this, IncomingCallService::class.java)
        stopService(intent)
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
        if (isFinishing) {
            stopIncomingCallService()
        }
    }
}