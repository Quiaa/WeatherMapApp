package com.example.weathermapapp.ui.webrtc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weathermapapp.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IncomingCallViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _callerName = MutableLiveData<String>()
    val callerName: LiveData<String> = _callerName

    fun fetchCallerName(uid: String) {
        viewModelScope.launch {
            val name = userRepository.getUserName(uid)
            _callerName.postValue(name)
        }
    }
}
