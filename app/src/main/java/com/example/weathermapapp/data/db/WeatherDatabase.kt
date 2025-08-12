package com.example.weathermapapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.weathermapapp.data.model.WeatherCacheEntity

@Database(entities = [WeatherCacheEntity::class], version = 1, exportSchema = false)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
}