package com.example.weathermapapp.ui.webrtc

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object CallManager {
    enum class CallState {
        IDLE,
        INCOMING,
        IN_PROGRESS
    }

    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState = _callState.asStateFlow()

    fun setCallState(state: CallState) {
        _callState.value = state
    }
}