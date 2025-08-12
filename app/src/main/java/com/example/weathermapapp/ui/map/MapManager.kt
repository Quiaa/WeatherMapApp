package com.example.weathermapapp.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import com.example.weathermapapp.R
import com.example.weathermapapp.data.model.UserLocation
import com.google.gson.JsonObject
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures

class MapManager(private val context: Context, private val mapView: MapView) {

    private var selectedLocationAnnManager: PointAnnotationManager? = null // Red (weather) marker
    private var myRealtimeLocationAnnManager: PointAnnotationManager? = null // Green (current location) marker
    private var otherRealtimeLocationsAnnManager: PointAnnotationManager? = null // Blue (other users) markers

    private var selectedLocationAnnotation: PointAnnotation? = null
    private var myRealtimeLocationAnnotation: PointAnnotation? = null
    private var otherUsersAnnotations = mutableMapOf<String, PointAnnotation>()

    fun initialize(onMapClick: OnMapClickListener, onStyleLoaded: () -> Unit) {
        val annotationApi = mapView.annotations
        selectedLocationAnnManager = annotationApi.createPointAnnotationManager()
        myRealtimeLocationAnnManager = annotationApi.createPointAnnotationManager()
        otherRealtimeLocationsAnnManager = annotationApi.createPointAnnotationManager()

        addAnnotationClickListener()

        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) { style ->

            getBitmapFromVectorDrawable(R.drawable.ic_red_marker)?.let { style.addImage("red-marker", it) }
            getBitmapFromVectorDrawable(R.drawable.ic_green_marker)?.let { style.addImage("green-marker", it) }
            getBitmapFromVectorDrawable(R.drawable.ic_blue_marker)?.let { style.addImage("blue-marker", it) }

            mapView.gestures.addOnMapClickListener(onMapClick)

            onStyleLoaded()
        }
    }


    fun updateSelectedLocationMarker(location: UserLocation) {
        val point = Point.fromLngLat(location.longitude, location.latitude)

        val data = JsonObject().apply { addProperty("name", "Weather for ${location.userName}'s choice") }

        selectedLocationAnnotation?.let { selectedLocationAnnManager?.delete(it) }

        val options = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage("red-marker")
            .withIconSize(1.5)
            .withData(data)

        selectedLocationAnnotation = selectedLocationAnnManager?.create(options)
    }


    fun updateMyRealtimeLocationMarker(location: UserLocation) {
        val point = Point.fromLngLat(location.longitude, location.latitude)
        val data = JsonObject().apply { addProperty("name", "Me: ${location.userName}") }

        if (myRealtimeLocationAnnotation == null) {
            val options = PointAnnotationOptions().withPoint(point).withIconImage("green-marker").withIconSize(1.5).withData(data)
            myRealtimeLocationAnnotation = myRealtimeLocationAnnManager?.create(options)
        } else {
            myRealtimeLocationAnnotation?.point = point
            myRealtimeLocationAnnManager?.update(myRealtimeLocationAnnotation!!)
        }
    }


    fun updateOtherUsersRealtimeMarkers(locations: List<UserLocation>) {
        val newLocationsMap = locations.associateBy { it.userId }
        val currentAnnotationIds = otherUsersAnnotations.keys.toMutableSet()

        // Update existing and add new markers
        for (location in locations) {
            val userId = location.userId
            if (userId.isEmpty()) continue

            val point = Point.fromLngLat(location.longitude, location.latitude)

            if (otherUsersAnnotations.containsKey(userId)) {
                // Update existing marker
                val annotation = otherUsersAnnotations[userId]
                annotation?.point = point
                otherRealtimeLocationsAnnManager?.update(annotation!!)
                currentAnnotationIds.remove(userId)
            } else {
                // Add new marker
                val data = JsonObject().apply { addProperty("name", location.userName) }
                val options = PointAnnotationOptions()
                    .withPoint(point)
                    .withIconImage("blue-marker")
                    .withIconSize(1.5)
                    .withData(data)
                val annotation = otherRealtimeLocationsAnnManager?.create(options)
                if (annotation != null) {
                    otherUsersAnnotations[userId] = annotation
                }
            }
        }

        // Remove old markers
        for (userId in currentAnnotationIds) {
            otherUsersAnnotations[userId]?.let {
                otherRealtimeLocationsAnnManager?.delete(it)
            }
            otherUsersAnnotations.remove(userId)
        }
    }

    private fun addAnnotationClickListener() {
        val listener = OnPointAnnotationClickListener { annotation ->
            annotation.getData()?.let {
                val name = it.asJsonObject.get("name").asString
                Toast.makeText(context, name, Toast.LENGTH_LONG).show()
            }
            true
        }
        selectedLocationAnnManager?.addClickListener(listener)
        myRealtimeLocationAnnManager?.addClickListener(listener)
        otherRealtimeLocationsAnnManager?.addClickListener(listener)
    }

    fun moveCamera(point: Point, zoom: Double = 12.0) {
        val cameraOptions = CameraOptions.Builder()
            .center(point)
            .zoom(zoom)
            .build()
        mapView.getMapboxMap().setCamera(cameraOptions)
    }

    private fun getBitmapFromVectorDrawable(drawableId: Int): Bitmap? {
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
}