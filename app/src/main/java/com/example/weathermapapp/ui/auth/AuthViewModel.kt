package com.example.weathermapapp.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weathermapapp.data.repository.AuthRepository
import com.example.weathermapapp.util.Resource
import com.google.firebase.auth.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _registrationState = MutableLiveData<Resource<AuthResult>>()
    val registrationState: LiveData<Resource<AuthResult>> = _registrationState

    private val _loginState = MutableLiveData<Resource<AuthResult>>()
    val loginState: LiveData<Resource<AuthResult>> = _loginState

    fun register(email: String, pass: String, name: String) {
        viewModelScope.launch {
            _registrationState.value = Resource.Loading()
            val result = repository.registerUser(email, pass, name)
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
}