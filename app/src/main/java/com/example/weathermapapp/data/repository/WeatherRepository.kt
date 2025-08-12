package com.example.weathermapapp.data.repository

import com.example.weathermapapp.BuildConfig
import com.example.weathermapapp.data.model.WeatherResponse
import com.example.weathermapapp.network.api.WeatherApiService
import com.example.weathermapapp.util.Resource
import javax.inject.Inject

// Interface for WeatherRepository
interface WeatherRepository {
    suspend fun getWeatherData(lat: Double, lon: Double): Resource<WeatherResponse>
}

// Implementation of WeatherRepository
class WeatherRepositoryImpl @Inject constructor(
    private val weatherApiService: WeatherApiService
) : WeatherRepository {
    override suspend fun getWeatherData(lat: Double, lon: Double): Resource<WeatherResponse> {
        return try {
            val response = weatherApiService.getCurrentWeather(
                latitude = lat,
                longitude = lon,
                apiKey = BuildConfig.OPENWEATHER_API_KEY
            )
            // ... geri kalan kod aynı
            if (response.isSuccessful) {
                response.body()?.let {
                    Resource.Success(it)
                } ?: Resource.Error("Response body is empty.")
            } else {
                Resource.Error("API Error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown network error occurred.")
        }
    }
}