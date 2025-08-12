package com.example.weathermapapp.data.model

data class WeatherDataWrapper(
    val weatherResponse: WeatherResponse,
    val isFromCache: Boolean,
    val cacheTimestamp: Long? = null
)