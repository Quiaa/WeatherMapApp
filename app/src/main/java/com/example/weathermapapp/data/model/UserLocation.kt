package com.example.weathermapapp.data.model

/**
 * Represents a user's selected location.
 * The default values are necessary for Firestore to deserialize the object.
 */
data class UserLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)