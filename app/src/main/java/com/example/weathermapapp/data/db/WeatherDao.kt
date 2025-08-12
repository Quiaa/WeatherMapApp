package com.example.weathermapapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.weathermapapp.data.model.WeatherCacheEntity

@Dao
interface WeatherDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weather: WeatherCacheEntity)

    @Query("SELECT * FROM weather_cache WHERE locationKey = :locationKey")
    suspend fun getWeatherByLocation(locationKey: String): WeatherCacheEntity?

    @Query("DELETE FROM weather_cache WHERE locationKey = :locationKey")
    suspend fun deleteWeatherByLocation(locationKey: String)
}