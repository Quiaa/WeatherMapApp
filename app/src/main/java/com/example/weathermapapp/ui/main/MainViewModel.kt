package com.example.weathermapapp.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.weathermapapp.ui.webrtc.IncomingCallService
import com.example.weathermapapp.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _permissionRequest = MutableLiveData<Event<Array<String>>>()
    val permissionRequest: LiveData<Event<Array<String>>> = _permissionRequest

    private val _startLocationUpdates = MutableLiveData<Event<Unit>>()
    val startLocationUpdates: LiveData<Event<Unit>> = _startLocationUpdates

    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    fun onMainActivityReady() {
        checkPermissions()
    }

    private fun checkPermissions() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            _startLocationUpdates.value = Event(Unit)
            startIncomingCallService()
        } else {
            _permissionRequest.value = Event(missingPermissions.toTypedArray())
        }
    }

    fun onPermissionResult(grantedPermissions: Map<String, Boolean>) {
        checkPermissions()
    }

    private fun startIncomingCallService() {
        val intent = Intent(context, IncomingCallService::class.java)
        context.startService(intent)
    }
}