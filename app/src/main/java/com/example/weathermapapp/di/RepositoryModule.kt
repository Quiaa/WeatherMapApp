package com.example.weathermapapp.di

import com.example.weathermapapp.data.repository.AuthRepository
import com.example.weathermapapp.data.repository.AuthRepositoryImpl
import com.example.weathermapapp.data.repository.UserRepository
import com.example.weathermapapp.data.repository.UserRepositoryImpl
import com.example.weathermapapp.data.repository.WeatherRepository
import com.example.weathermapapp.data.repository.WeatherRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    abstract fun bindWeatherRepository(
        weatherRepositoryImpl: WeatherRepositoryImpl
    ): WeatherRepository
}