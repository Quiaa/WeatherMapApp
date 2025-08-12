package com.example.weathermapapp.data.repository

import com.example.weathermapapp.BuildConfig
import com.example.weathermapapp.data.db.WeatherDao
import com.example.weathermapapp.data.model.WeatherCacheEntity
import com.example.weathermapapp.data.model.WeatherDataWrapper
import com.example.weathermapapp.data.model.WeatherResponse
import com.example.weathermapapp.network.api.WeatherApiService
import com.example.weathermapapp.util.Resource
import javax.inject.Inject

interface WeatherRepository {
    suspend fun getWeatherData(lat: Double, lon: Double, forceNetwork: Boolean = false): Resource<WeatherDataWrapper>
}

class WeatherRepositoryImpl @Inject constructor(
    private val weatherApiService: WeatherApiService,
    private val weatherDao: WeatherDao
) : WeatherRepository {

    private val CACHE_TIMEOUT = 5 * 60 * 1000 // 5 minutes in milliseconds

    override suspend fun getWeatherData(lat: Double, lon: Double, forceNetwork: Boolean): Resource<WeatherDataWrapper> {
        val locationKey = "$lat,$lon"
        val currentTime = System.currentTimeMillis()
        val cachedWeather = weatherDao.getWeatherByLocation(locationKey)

        // If it has not been forced to refresh and there is fresh data in the cache, use it
        if (!forceNetwork && cachedWeather != null && (currentTime - cachedWeather.timestamp < CACHE_TIMEOUT)) {
            val response = mapToWeatherResponse(cachedWeather)
            val wrapper = WeatherDataWrapper(response, isFromCache = true, cacheTimestamp = cachedWeather.timestamp)
            return Resource.Success(wrapper)
        }

        // Fetch new data from the network
        return try {
            val networkResponse = weatherApiService.getCurrentWeather(
                latitude = lat,
                longitude = lon,
                apiKey = BuildConfig.OPENWEATHER_API_KEY
            )

            if (networkResponse.isSuccessful && networkResponse.body() != null) {
                // If successful, update the database and return the new data
                val weatherData = networkResponse.body()!!
                weatherDao.insertWeather(mapToCacheEntity(weatherData, locationKey, currentTime))
                val wrapper = WeatherDataWrapper(weatherData, isFromCache = false)
                Resource.Success(wrapper)
            } else {
                // Network error, but if there is data in the cache, even if it is old, use it
                cachedWeather?.let {
                    val response = mapToWeatherResponse(it)
                    val wrapper = WeatherDataWrapper(response, isFromCache = true, cacheTimestamp = it.timestamp)
                    return Resource.Success(wrapper)
                }
                Resource.Error("API Error: ${networkResponse.code()} - ${networkResponse.message()}")
            }
        } catch (e: Exception) {
            // Network error (no internet etc.) but if there is data in the cache, use it
            cachedWeather?.let {
                val response = mapToWeatherResponse(it)
                val wrapper = WeatherDataWrapper(response, isFromCache = true, cacheTimestamp = it.timestamp)
                return Resource.Success(wrapper)
            }
            Resource.Error(e.message ?: "An unknown network error occurred.")
        }
    }

    private fun mapToWeatherResponse(cache: WeatherCacheEntity): WeatherResponse {
        return WeatherResponse(
            weather = listOf(com.example.weathermapapp.data.model.Weather(cache.description, cache.icon)),
            main = com.example.weathermapapp.data.model.Main(cache.temperature, 0.0, 0),
            name = cache.cityName
        )
    }

    private fun mapToCacheEntity(response: WeatherResponse, locationKey: String, timestamp: Long): WeatherCacheEntity {
        return WeatherCacheEntity(
            locationKey = locationKey,
            cityName = response.name,
            description = response.weather.firstOrNull()?.description ?: "N/A",
            icon = response.weather.firstOrNull()?.icon ?: "",
            temperature = response.main.temp,
            timestamp = timestamp
        )
    }
}