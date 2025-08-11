package com.example.weathermapapp.data.repository

import com.example.weathermapapp.BuildConfig
import com.example.weathermapapp.data.model.WeatherResponse
import com.example.weathermapapp.network.service.RetrofitInstance
import com.example.weathermapapp.util.Resource

// Interface for WeatherRepository
interface WeatherRepository {
    suspend fun getWeatherData(lat: Double, lon: Double): Resource<WeatherResponse>
}

// Implementation of WeatherRepository
class WeatherRepositoryImpl : WeatherRepository {

    override suspend fun getWeatherData(lat: Double, lon: Double): Resource<WeatherResponse> {
        return try {
            // Call the API through our RetrofitInstance
            val response = RetrofitInstance.api.getCurrentWeather(
                latitude = lat,
                longitude = lon,
                apiKey = BuildConfig.OPENWEATHER_API_KEY // Use the key from BuildConfig
            )
            if (response.isSuccessful) {
                // If the response is successful, return the data
                response.body()?.let {
                    Resource.Success(it)
                } ?: Resource.Error("Response body is empty.")
            } else {
                // If the server returned an error (e.g., 404, 500)
                Resource.Error("API Error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            // If there's a network error or other exception
            Resource.Error(e.message ?: "An unknown network error occurred.")
        }
    }
}