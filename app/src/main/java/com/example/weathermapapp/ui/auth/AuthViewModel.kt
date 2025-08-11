package com.example.weathermapapp.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weathermapapp.data.model.UserLocation
import com.example.weathermapapp.data.model.WeatherResponse
import com.example.weathermapapp.data.repository.AuthRepository
import com.example.weathermapapp.data.repository.AuthRepositoryImpl
import com.example.weathermapapp.util.Resource
import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository: AuthRepository = AuthRepositoryImpl()

    private val _registrationState = MutableLiveData<Resource<AuthResult>>()
    val registrationState: LiveData<Resource<AuthResult>> = _registrationState
    private val _loginState = MutableLiveData<Resource<AuthResult>>()
    val loginState: LiveData<Resource<AuthResult>> = _loginState

    private val _userLocation = MutableLiveData<Resource<UserLocation?>>()
    val userLocation: LiveData<Resource<UserLocation?>> = _userLocation

    private val _weatherData = MutableLiveData<Resource<WeatherResponse>>()
    val weatherData: LiveData<Resource<WeatherResponse>> = _weatherData

    fun register(email: String, pass: String) {
        viewModelScope.launch {
            _registrationState.value = Resource.Loading()
            val result = repository.registerUser(email, pass)
            _registrationState.value = result
        }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _loginState.value = Resource.Loading()
            val result = repository.loginUser(email, pass)
            _loginState.value = result
        }
    }

    fun logout() {
        repository.logoutUser()
    }

    fun saveLocation(location: UserLocation) {
        viewModelScope.launch {
            repository.saveUserLocation(location)
        }
    }

    fun loadUserLocation() {
        viewModelScope.launch {
            _userLocation.value = Resource.Loading()
            val result = repository.getUserLocation()
            _userLocation.value = result
        }
    }

    fun fetchWeatherData(lat: Double, lon: Double) {
        viewModelScope.launch {
            _weatherData.value = Resource.Loading()
            val result = repository.getWeatherData(lat, lon)
            _weatherData.value = result
        }
    }
}