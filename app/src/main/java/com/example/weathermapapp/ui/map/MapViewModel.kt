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
import android.view.View
import androidx.lifecycle.MediatorLiveData
import com.example.weathermapapp.data.model.WeatherDataWrapper
import com.example.weathermapapp.ui.main.WeatherUIData
import com.example.weathermapapp.util.TimeUtils
import com.example.weathermapapp.data.model.webrtc.NSDataModel
import com.example.weathermapapp.data.model.webrtc.NSDataModelType
import com.example.weathermapapp.data.repository.webrtc.MainRepository
import com.example.weathermapapp.ui.webrtc.WebRTCService

@HiltViewModel
class MapViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val weatherRepository: WeatherRepository,
    private val locationProvider: LocationProvider,
    private val mainRepository: MainRepository
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

    private val _weatherUIState = MediatorLiveData<WeatherUIData>()
    val weatherUIState: LiveData<WeatherUIData> = _weatherUIState

    // LiveData for all other users' locations
    private val _allUsersLocations = MutableLiveData<Resource<List<UserLocation>>>()
    val allUsersLocations: LiveData<Resource<List<UserLocation>>> = _allUsersLocations
    private val _realtimeAllUsersLocations = MutableLiveData<Resource<List<UserLocation>>>()
    val realtimeAllUsersLocations: LiveData<Resource<List<UserLocation>>> = _realtimeAllUsersLocations

    private val _myRealtimeLocation = MutableLiveData<UserLocation>()
    val myRealtimeLocation: LiveData<UserLocation> = _myRealtimeLocation

    private val _otherUsersRealtimeLocations = MutableLiveData<List<UserLocation>>()
    val otherUsersRealtimeLocations: LiveData<List<UserLocation>> = _otherUsersRealtimeLocations

    private val _logoutComplete = MutableLiveData<Boolean>()
    val logoutComplete: LiveData<Boolean> = _logoutComplete
    private val _incomingCall = MutableLiveData<NSDataModel?>()
    val incomingCall: LiveData<NSDataModel?> = _incomingCall

    private var lastLocationUpdateTime = 0L
    private val locationUpdateThreshold = 5000L // 5 seconds

    init {
        _weatherUIState.addSource(_weatherData) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    _weatherUIState.value = WeatherUIData(
                        progressBarVisibility = View.VISIBLE,
                        weatherCardVisibility = View.VISIBLE,
                        cacheStatusVisibility = View.GONE
                    )
                }
                is Resource.Success -> {
                    resource.data?.let { wrapper ->
                        val data = wrapper.weatherResponse
                        val cacheStatusText = if (wrapper.isFromCache && wrapper.cacheTimestamp != null) {
                            "(Cached) ${TimeUtils.getTimeAgo(wrapper.cacheTimestamp)}"
                        } else {
                            "Updated just now"
                        }
                        val iconUrl = "https://openweathermap.org/img/wn/${data.weather.firstOrNull()?.icon}@2x.png"

                        _weatherUIState.value = WeatherUIData(
                            locationName = data.name,
                            description = data.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "N/A",
                            temperature = "${data.main.temp.toInt()}°C",
                            cacheStatus = cacheStatusText,
                            iconUrl = iconUrl,
                            progressBarVisibility = View.GONE,
                            cacheStatusVisibility = View.VISIBLE,
                            weatherCardVisibility = View.VISIBLE
                        )
                    }
                }
                is Resource.Error -> {
                    // Optionally, you could add error-specific fields to WeatherUIData
                    _weatherUIState.value = WeatherUIData(
                        progressBarVisibility = View.GONE,
                        weatherCardVisibility = View.GONE
                    )
                }
            }
        }
        viewModelScope.launch {
            mainRepository.signalingEvent.collect {
                if (it.type == NSDataModelType.StartVideoCall) {
                    _incomingCall.postValue(it)
                }
            }
        }
    }
    fun clearIncomingCallEvent() {
        _incomingCall.value = null
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
        _logoutComplete.value = true
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
                    val allLocations = result.data.orEmpty().toMutableList()

                    allLocations.addAll(bots)

                    _myRealtimeLocation.value = allLocations.firstOrNull { it.userId == myId }
                    _otherUsersRealtimeLocations.value = allLocations.filter { it.userId != myId }
                }
            }
            .launchIn(viewModelScope)
    }

    private val bots = listOf(
        UserLocation(
            latitude = 41.03, longitude = 28.99,
            userId = "llama3-bot", userName = "Llama 3 Bot"
        )
        // You can add new bots here in the future.
    )
}