package com.example.weathermapapp.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weathermapapp.data.repository.AuthRepository
import com.example.weathermapapp.data.repository.AuthRepositoryImpl
import com.example.weathermapapp.util.Resource
import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository: AuthRepository = AuthRepositoryImpl()

    // LiveData for Registration
    private val _registrationState = MutableLiveData<Resource<AuthResult>>()
    val registrationState: LiveData<Resource<AuthResult>> = _registrationState

    // LiveData for Login
    private val _loginState = MutableLiveData<Resource<AuthResult>>()
    val loginState: LiveData<Resource<AuthResult>> = _loginState

    fun register(email: String, pass: String) {
        viewModelScope.launch {
            _registrationState.value = Resource.Loading()
            val result = repository.registerUser(email, pass)
            _registrationState.value = result
        }
    }

    /**
     * Function to log in a user.
     * It launches a coroutine to call the suspend function in the repository.
     */
    fun login(email: String, pass: String) {
        viewModelScope.launch {
            // Set the state to Loading before starting the operation.
            _loginState.value = Resource.Loading()

            // Call the repository function and update the LiveData with the result.
            val result = repository.loginUser(email, pass)
            _loginState.value = result
        }
    }

    fun logout() {
        repository.logoutUser()
    }
}