package com.example.weathermapapp

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.bumptech.glide.Glide
import com.example.weathermapapp.data.model.UserLocation
import com.example.weathermapapp.data.model.WeatherResponse
import com.example.weathermapapp.databinding.ActivityMainBinding
import com.example.weathermapapp.ui.auth.AuthViewModel
import com.example.weathermapapp.ui.auth.LoginActivity
import com.example.weathermapapp.util.Resource
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeMap()
        setupLogoutButton()
        observeViewModels() // Combine observers
    }

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun initializeMap() {
        val annotationApi = binding.mapView.annotations
        pointAnnotationManager = annotationApi.createPointAnnotationManager()

        val mapboxMap = binding.mapView.getMapboxMap()
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->
            getBitmapFromVectorDrawable(this, R.drawable.ic_red_marker)?.let {
                style.addImage("red-marker", it)
            }
            binding.mapView.gestures.addOnMapClickListener(this)
            authViewModel.loadUserLocation()
        }
    }

    // Renamed function to hold all observers
    private fun observeViewModels() {
        // Observer for user location
        authViewModel.userLocation.observe(this) { resource ->
            if (resource is Resource.Success) {
                resource.data?.let { location ->
                    val userPoint = Point.fromLngLat(location.longitude, location.latitude)
                    placeMarkerOnMap(userPoint)
                    moveCameraToPoint(userPoint)
                    // Fetch weather data for the loaded location
                    authViewModel.fetchWeatherData(location.latitude, location.longitude)
                } ?: run {
                    val defaultPoint = Point.fromLngLat(28.9784, 41.0082)
                    moveCameraToPoint(defaultPoint, 9.0)
                }
            } else if (resource is Resource.Error) {
                Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                val defaultPoint = Point.fromLngLat(28.9784, 41.0082)
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
    }

    // New function to update the weather card UI
    private fun updateWeatherUI(data: WeatherResponse) {
        binding.tvLocationName.text = data.name
        binding.tvWeatherDescription.text = data.weather.firstOrNull()?.description?.capitalize() ?: "N/A"
        binding.tvTemperature.text = "${data.main.temp.toInt()}°C"

        // Construct the icon URL and load with Glide
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
        // Fetch weather for the newly clicked location
        authViewModel.fetchWeatherData(point.latitude(), point.longitude())
        return true
    }

    private fun placeMarkerOnMap(point: Point) {
        // ... (this function remains the same)
        if (pointAnnotation == null) {
            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage("red-marker")
                .withIconSize(1.5)
            pointAnnotation = pointAnnotationManager?.create(pointAnnotationOptions)
        } else {
            pointAnnotation?.point = point
            pointAnnotationManager?.update(pointAnnotation!!)
        }
    }

    private fun moveCameraToPoint(point: Point, zoom: Double = 12.0) {
        // ... (this function remains the same)
        val cameraOptions = CameraOptions.Builder()
            .center(point)
            .zoom(zoom)
            .build()
        binding.mapView.getMapboxMap().setCamera(cameraOptions)
    }

    private fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap? {
        // ... (this function remains the same)
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