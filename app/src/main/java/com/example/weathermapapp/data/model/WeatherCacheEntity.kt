package com.example.weathermapapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey
    val locationKey: String, // "lat,lon"
    val cityName: String,
    val description: String,
    val icon: String,
    val temperature: Double,
    val timestamp: Long
)