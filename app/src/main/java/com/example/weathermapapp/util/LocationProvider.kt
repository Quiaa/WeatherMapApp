package com.example.weathermapapp.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.mapbox.geojson.Point

class LocationProvider(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null

    fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getCurrentLocation(onSuccess: (Point) -> Unit, onFailure: (String) -> Unit) {
        if (!isLocationPermissionGranted()) {
            onFailure("Location permission not granted.")
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val currentPoint = Point.fromLngLat(location.longitude, location.latitude)
                    onSuccess(currentPoint)
                } else {
                    // If last location is null, request a new location update
                    requestNewLocationData(onSuccess, onFailure)
                }
            }
            .addOnFailureListener {
                onFailure("Failed to get location: ${it.message}")
            }
    }

    private fun requestNewLocationData(onSuccess: (Point) -> Unit, onFailure: (String) -> Unit) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 5000
            fastestInterval = 2500
            numUpdates = 1 // We only need one update
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // --- START OF CORRECTION ---
                val lastLocation = locationResult.lastLocation
                if (lastLocation != null) {
                    val currentPoint = Point.fromLngLat(lastLocation.longitude, lastLocation.latitude)
                    onSuccess(currentPoint)
                } else {
                    // This is a rare case, but we handle it to be safe.
                    onFailure("Failed to get location from callback.")
                }
                // --- END OF CORRECTION ---
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                if (!locationAvailability.isLocationAvailable) {
                    onFailure("Location is not available. Turn on GPS.")
                }
            }
        }

        if (isLocationPermissionGranted()) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.myLooper()
            )
        }
    }

    /**
     * Starts continuous location updates.
     * @param onLocationUpdated A callback function that will be invoked with the new location.
     */
    fun startLocationUpdates(onLocationUpdated: (Point) -> Unit) {
        if (!isLocationPermissionGranted()) return

        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000 // Update every 10 seconds
            fastestInterval = 5000 // The fastest update interval
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    val currentPoint = Point.fromLngLat(it.longitude, it.latitude)
                    onLocationUpdated(currentPoint)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.myLooper()
        )
    }

    /**
     * Stops continuous location updates.
     */
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }
}