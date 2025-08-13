package com.example.weathermapapp.ui.main

import android.view.View

data class WeatherUIData(
    val locationName: String = "",
    val description: String = "",
    val temperature: String = "",
    val cacheStatus: String = "",
    val iconUrl: String = "",
    val weatherCardVisibility: Int = View.VISIBLE,
    val progressBarVisibility: Int = View.GONE,
    val cacheStatusVisibility: Int = View.GONE
)
