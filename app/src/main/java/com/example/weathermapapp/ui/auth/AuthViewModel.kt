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

    // We instantiate our repository. In a larger app, we would use dependency injection.
    private val repository: AuthRepository = AuthRepositoryImpl()

    // LiveData to hold the state of the registration process.
    private val _registrationState = MutableLiveData<Resource<AuthResult>>()
    val registrationState: LiveData<Resource<AuthResult>> = _registrationState

    /**
     * Function to register a user.
     * It launches a coroutine to call the suspend function in the repository.
     */
    fun register(email: String, pass: String) {
        // We use viewModelScope to launch a coroutine that is automatically
        // cancelled when the ViewModel is cleared.
        viewModelScope.launch {
            // Set the state to Loading before starting the operation.
            _registrationState.value = Resource.Loading()

            // Call the repository function and update the LiveData with the result.
            val result = repository.registerUser(email, pass)
            _registrationState.value = result
        }
    }
}