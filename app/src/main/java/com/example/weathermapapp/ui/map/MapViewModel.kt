package com.example.weathermapapp.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weathermapapp.data.model.UserLocation
import com.example.weathermapapp.data.model.WeatherResponse
import com.example.weathermapapp.data.repository.AuthRepository
import com.example.weathermapapp.data.repository.UserRepository
import com.example.weathermapapp.data.repository.WeatherRepository
import com.example.weathermapapp.util.Resource
import kotlinx.coroutines.launch
import com.example.weathermapapp.util.LocationProvider
import com.mapbox.geojson.Point
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import com.example.weathermapapp.data.model.WeatherDataWrapper
import androidx.lifecycle.MediatorLiveData

@HiltViewModel
class MapViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val weatherRepository: WeatherRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    // LiveData for the current device location event
    private val _currentDeviceLocation = MutableLiveData<Resource<Point>>()
    val currentDeviceLocation: LiveData<Resource<Point>> = _currentDeviceLocation
    // LiveData for the current user's location
    private val _userLocation = MutableLiveData<Resource<UserLocation?>>()
    val userLocation: LiveData<Resource<UserLocation?>> = _userLocation

    // LiveData for weather data
    private val _weatherData = MutableLiveData<Resource<WeatherDataWrapper>>()
    val weatherData: LiveData<Resource<WeatherDataWrapper>> = _weatherData
    private var lastFetchedPoint: Point? = null

    // LiveData for all other users' locations
    private val _allUsersLocations = MutableLiveData<Resource<List<UserLocation>>>()
    val allUsersLocations: LiveData<Resource<List<UserLocation>>> = _allUsersLocations
    private val _realtimeAllUsersLocations = MutableLiveData<Resource<List<UserLocation>>>()
    val realtimeAllUsersLocations: LiveData<Resource<List<UserLocation>>> = _realtimeAllUsersLocations

    private val _myRealtimeLocation = MutableLiveData<UserLocation>()
    val myRealtimeLocation: LiveData<UserLocation> = _myRealtimeLocation

    private val _otherUsersRealtimeLocations = MutableLiveData<List<UserLocation>>()
    val otherUsersRealtimeLocations: LiveData<List<UserLocation>> = _otherUsersRealtimeLocations

    private var lastLocationUpdateTime = 0L
    private val locationUpdateThreshold = 5000L // 5 seconds

    val isWeatherFromCache = MediatorLiveData<Boolean>().apply {
        addSource(_weatherData) { resource ->
            value = (resource is Resource.Success && resource.data?.isFromCache == true)
        }
    }
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

    fun fetchWeatherData(lat: Double, lon: Double, forceNetwork: Boolean = false) {
        lastFetchedPoint = Point.fromLngLat(lon, lat)
        viewModelScope.launch {
            _weatherData.value = Resource.Loading()
            val result = weatherRepository.getWeatherData(lat, lon, forceNetwork)
            _weatherData.value = result
        }
    }

    fun forceRefreshWeatherData() {
        lastFetchedPoint?.let { point ->
            viewModelScope.launch {
                _weatherData.value = Resource.Loading()
                val result = weatherRepository.getWeatherData(point.latitude(), point.longitude(), forceNetwork = true)
                _weatherData.value = result
            }
        }
    }

    fun logout() {
        authRepository.logoutUser()
    }

    fun saveLocation(location: UserLocation) {
        viewModelScope.launch {
            userRepository.saveUserLocation(location)
        }
    }

    fun observeUserLocation() {
        _userLocation.value = Resource.Loading()
        userRepository.getUserLocation()
            .onEach { result ->
                _userLocation.value = result
            }
            .launchIn(viewModelScope)
    }

    fun fetchAllUsersLocations() {
        viewModelScope.launch {
            _allUsersLocations.value = Resource.Loading()
            val result = userRepository.getAllUsersLocations()
            _allUsersLocations.value = result
        }
    }
    /**
     * Starts listening for real-time location updates from the device
     * and saves them to the repository.
     */
    fun startRealtimeLocationUpdates() {
        locationProvider.startLocationUpdates { point ->
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastLocationUpdateTime > locationUpdateThreshold) {
                lastLocationUpdateTime = currentTime
                viewModelScope.launch {
                    val userLocation = UserLocation(point.latitude(), point.longitude())
                    userRepository.saveRealtimeUserLocation(userLocation)
                }
            }
        }
    }

    /**
     * Stops listening for real-time location updates.
     */
    fun stopRealtimeLocationUpdates() {
        locationProvider.stopLocationUpdates()
    }

    /**
     * Starts observing the real-time locations of all other users from the repository.
     */
    fun observeRealtimeUsersLocations() {
        val myId = authRepository.getCurrentUserId()
        userRepository.getRealtimeAllUsersLocations()
            .onEach { result ->
                if (result is Resource.Success) {
                    val allLocations = result.data ?: emptyList()
                    _myRealtimeLocation.value = allLocations.firstOrNull { it.userId == myId }
                    _otherUsersRealtimeLocations.value = allLocations.filter { it.userId != myId }
                }
            }
            .launchIn(viewModelScope)
    }
}