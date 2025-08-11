package com.example.weathermapapp.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weathermapapp.data.model.UserLocation
import com.example.weathermapapp.data.model.WeatherResponse
import com.example.weathermapapp.data.repository.AuthRepository
import com.example.weathermapapp.data.repository.AuthRepositoryImpl
import com.example.weathermapapp.data.repository.UserRepository
import com.example.weathermapapp.data.repository.UserRepositoryImpl
import com.example.weathermapapp.data.repository.WeatherRepository
import com.example.weathermapapp.data.repository.WeatherRepositoryImpl
import com.example.weathermapapp.util.Resource
import kotlinx.coroutines.launch
import com.example.weathermapapp.util.LocationProvider
import com.mapbox.geojson.Point
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository: AuthRepository = AuthRepositoryImpl()
    private val userRepository: UserRepository = UserRepositoryImpl()
    private val weatherRepository: WeatherRepository = WeatherRepositoryImpl()

    private val locationProvider: LocationProvider = LocationProvider(application)

    // LiveData for the current device location event
    private val _currentDeviceLocation = MutableLiveData<Resource<Point>>()
    val currentDeviceLocation: LiveData<Resource<Point>> = _currentDeviceLocation
    // LiveData for the current user's location
    private val _userLocation = MutableLiveData<Resource<UserLocation?>>()
    val userLocation: LiveData<Resource<UserLocation?>> = _userLocation

    // LiveData for weather data
    private val _weatherData = MutableLiveData<Resource<WeatherResponse>>()
    val weatherData: LiveData<Resource<WeatherResponse>> = _weatherData

    // LiveData for all other users' locations
    private val _allUsersLocations = MutableLiveData<Resource<List<UserLocation>>>()
    val allUsersLocations: LiveData<Resource<List<UserLocation>>> = _allUsersLocations

    fun fetchCurrentDeviceLocation() {
        _currentDeviceLocation.value = Resource.Loading()
        locationProvider.getCurrentLocation(
            onSuccess = { point ->
                _currentDeviceLocation.value = Resource.Success(point)
            },
            onFailure = { errorMessage ->
                _currentDeviceLocation.value = Resource.Error(errorMessage)
            }
        )
    }

    fun logout() {
        authRepository.logoutUser()
    }

    fun saveLocation(location: UserLocation) {
        viewModelScope.launch {
            userRepository.saveUserLocation(location)
        }
    }

    fun loadUserLocation() {
        viewModelScope.launch {
            _userLocation.value = Resource.Loading()
            val result = userRepository.getUserLocation()
            _userLocation.value = result
        }
    }

    fun fetchWeatherData(lat: Double, lon: Double) {
        viewModelScope.launch {
            _weatherData.value = Resource.Loading()
            val result = weatherRepository.getWeatherData(lat, lon)
            _weatherData.value = result
        }
    }

    fun fetchAllUsersLocations() {
        viewModelScope.launch {
            _allUsersLocations.value = Resource.Loading()
            val result = userRepository.getAllUsersLocations()
            _allUsersLocations.value = result
        }
    }
}