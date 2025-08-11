package com.example.weathermapapp.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.appcompat.content.res.AppCompatResources
import com.example.weathermapapp.R
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures // Gerekli import eklendi

class MapManager(private val context: Context, private val mapView: MapView) {

    private var userPointAnnotationManager: PointAnnotationManager? = null
    private var otherUsersAnnotationManager: PointAnnotationManager? = null
    private var userPointAnnotation: PointAnnotation? = null

    fun initialize(onMapClick: OnMapClickListener, onStyleLoaded: () -> Unit) {
        val annotationApi = mapView.annotations
        userPointAnnotationManager = annotationApi.createPointAnnotationManager()
        otherUsersAnnotationManager = annotationApi.createPointAnnotationManager()

        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) { style ->
            // Add marker images to the style
            getBitmapFromVectorDrawable(R.drawable.ic_red_marker)?.let {
                style.addImage("red-marker", it)
            }
            getBitmapFromVectorDrawable(R.drawable.ic_blue_marker)?.let {
                style.addImage("blue-marker", it)
            }

            // Set the map click listener (CORRECTED LINE)
            mapView.gestures.addOnMapClickListener(onMapClick)

            // Notify that the style has loaded
            onStyleLoaded()
        }
    }

    fun placeUserMarker(point: Point) {
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

    fun placeOtherUsersMarkers(points: List<Point>) {
        otherUsersAnnotationManager?.deleteAll()
        val annotationOptions = points.map { point ->
            PointAnnotationOptions()
                .withPoint(point)
                .withIconImage("blue-marker")
                .withIconSize(1.5)
        }
        otherUsersAnnotationManager?.create(annotationOptions)
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