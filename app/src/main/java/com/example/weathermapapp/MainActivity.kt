package com.example.weathermapapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.weathermapapp.data.model.UserLocation
import com.example.weathermapapp.data.model.WeatherResponse
import com.example.weathermapapp.databinding.ActivityMainBinding
import com.example.weathermapapp.ui.auth.AuthViewModel
import com.example.weathermapapp.ui.auth.LoginActivity
import com.example.weathermapapp.util.Resource
import com.google.android.gms.location.*
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures

class MainActivity : AppCompatActivity(), OnMapClickListener {

    private lateinit var binding: ActivityMainBinding
    private val authViewModel: AuthViewModel by viewModels()

    private var pointAnnotationManager: PointAnnotationManager? = null
    private var pointAnnotation: PointAnnotation? = null
    private var otherUsersAnnotationManager: PointAnnotationManager? = null // For other users
    private var userPointAnnotationManager: PointAnnotationManager? = null // For the current user
    private var userPointAnnotation: PointAnnotation? = null // For the current user

    // FusedLocationProviderClient for getting current location
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // ActivityResultLauncher for handling permission requests
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted.
                getCurrentLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted.
                getCurrentLocation()
            }
            else -> {
                // No location access granted.
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        initializeMap()
        setupClickListeners() // Renamed from setupLogoutButton
        observeViewModels()
    }

    // Renamed to handle all click listeners in one place
    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.btnCurrentLocation.setOnClickListener {
            checkLocationPermission()
        }
    }

    private fun initializeMap() {
        val annotationApi = binding.mapView.annotations
        userPointAnnotationManager = annotationApi.createPointAnnotationManager()
        otherUsersAnnotationManager = annotationApi.createPointAnnotationManager() // Create manager for other users

        val mapboxMap = binding.mapView.getMapboxMap()
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->
            // Add red marker for the current user
            getBitmapFromVectorDrawable(this, R.drawable.ic_red_marker)?.let {
                style.addImage("red-marker", it)
            }
            // Add blue marker for other users
            getBitmapFromVectorDrawable(this, R.drawable.ic_blue_marker)?.let {
                style.addImage("blue-marker", it)
            }

            binding.mapView.gestures.addOnMapClickListener(this)
            authViewModel.loadUserLocation()
            authViewModel.fetchAllUsersLocations() // Fetch all users' locations
        }
    }

    private fun observeViewModels() {
        // Observer for user location
        authViewModel.userLocation.observe(this) { resource ->
            if (resource is Resource.Success) {
                resource.data?.let { location ->
                    val userPoint = Point.fromLngLat(location.longitude, location.latitude)
                    placeMarkerOnMap(userPoint)
                    moveCameraToPoint(userPoint)
                    authViewModel.fetchWeatherData(location.latitude, location.longitude)
                } ?: run {
                    val defaultPoint = Point.fromLngLat(28.9784, 41.0082) // Default to Istanbul
                    moveCameraToPoint(defaultPoint, 9.0)
                }
            } else if (resource is Resource.Error) {
                Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                val defaultPoint = Point.fromLngLat(28.9784, 41.0082) // Default to Istanbul
                moveCameraToPoint(defaultPoint, 9.0)
            }
        }

        // Observer for weather data
        authViewModel.weatherData.observe(this) { resource ->
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

        authViewModel.allUsersLocations.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // You can show a loading indicator if you want
                }
                is Resource.Success -> {
                    resource.data?.let { locations ->
                        placeOtherUsersMarkers(locations)
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(this, "Error fetching other users: ${resource.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateWeatherUI(data: WeatherResponse) {
        binding.tvLocationName.text = data.name
        binding.tvWeatherDescription.text = data.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "N/A"
        binding.tvTemperature.text = "${data.main.temp.toInt()}°C"

        val iconCode = data.weather.firstOrNull()?.icon
        val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
        Glide.with(this)
            .load(iconUrl)
            .into(binding.ivWeatherIcon)
    }

    override fun onMapClick(point: Point): Boolean {
        placeMarkerOnMap(point)
        val location = UserLocation(latitude = point.latitude(), longitude = point.longitude())
        authViewModel.saveLocation(location)
        authViewModel.fetchWeatherData(point.latitude(), point.longitude())
        return true
    }

    // New function to check for location permissions
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation()
        } else {
            // You can directly ask for the permission.
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // New function to get the current location
    private fun getCurrentLocation() {
        // Double-check permission before proceeding
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission not granted, cannot get location.", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val currentPoint = Point.fromLngLat(location.longitude, location.latitude)
                    moveCameraToPoint(currentPoint)
                    onMapClick(currentPoint) // Reuse onMapClick logic to update marker, weather, and save location
                } else {
                    Toast.makeText(this, "Could not retrieve location. Turn on GPS.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun placeOtherUsersMarkers(locations: List<UserLocation>) {
        // Clear old markers first to avoid duplicates
        otherUsersAnnotationManager?.deleteAll()

        locations.forEach { location ->
            val point = Point.fromLngLat(location.longitude, location.latitude)
            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage("blue-marker") // Use the blue marker
                .withIconSize(1.5)
            otherUsersAnnotationManager?.create(pointAnnotationOptions)
        }
    }

    private fun placeMarkerOnMap(point: Point) {
        if (userPointAnnotation == null) {
            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage("red-marker")
                .withIconSize(1.5)
            userPointAnnotation = userPointAnnotationManager?.create(pointAnnotationOptions)
        } else {
            userPointAnnotation?.point = point
            userPointAnnotationManager?.update(userPointAnnotation!!)
        }
    }

    private fun moveCameraToPoint(point: Point, zoom: Double = 12.0) {
        // This function remains the same
        val cameraOptions = CameraOptions.Builder()
            .center(point)
            .zoom(zoom)
            .build()
        binding.mapView.getMapboxMap().setCamera(cameraOptions)
    }

    private fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap? {
        // This function remains the same
        val drawable = AppCompatResources.getDrawable(context, drawableId) ?: return null
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
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