package com.example.weathermapapp.ui.webrtc

import android.view.View
import androidx.lifecycle.*
import com.example.weathermapapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject

data class UiState(
    val isPipMode: Boolean = false,
    val controlsVisibility: Int = View.VISIBLE,
    val localViewWidth: Int = 120,
    val localViewHeight: Int = 150
)

@HiltViewModel
class VideoCallViewModel @Inject constructor(
    private val webRTCService: WebRTCService,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _callEnded = MutableLiveData<Boolean>()
    val callEnded: LiveData<Boolean> = _callEnded

    private val _isMuted = MutableLiveData(false)
    val isMuted: LiveData<Boolean> = _isMuted

    private val _pipMode = MutableLiveData<Unit>()
    val pipMode: LiveData<Unit> = _pipMode

    private val _uiState = MutableLiveData(UiState())
    val uiState: LiveData<UiState> = _uiState

    private var isCallEnding = false

    init {
        webRTCService.listener = object : WebRTCService.Listener {
            override fun onCallEnded() {
                _callEnded.postValue(true)
            }
        }
    }

    fun init(username: String) {
        webRTCService.initialize(username)
    }

    fun setupViews(local: SurfaceViewRenderer, remote: SurfaceViewRenderer, target: String, isCaller: Boolean) {
        webRTCService.setupViews(local, remote, target, isCaller)
    }

    fun endCall() {
        if (!isCallEnding) {
            isCallEnding = true
            viewModelScope.launch {
                webRTCService.endCall()
            }
        }
    }

    fun switchCamera() {
        webRTCService.switchCamera()
    }

    fun toggleMic() {
        val muted = !(_isMuted.value ?: false)
        _isMuted.value = muted
        webRTCService.toggleAudio(muted)
    }

    fun getCurrentUserId(): String? {
        return authRepository.getCurrentUserId()
    }

    fun enterPipMode() {
        _pipMode.value = Unit
    }

    fun onPipModeChanged(isPipMode: Boolean) {
        if (isPipMode) {
            _uiState.value = UiState(
                isPipMode = true,
                controlsVisibility = View.GONE,
                localViewWidth = 48,
                localViewHeight = 60
            )
        } else {
            _uiState.value = UiState(
                isPipMode = false,
                controlsVisibility = View.VISIBLE,
                localViewWidth = 120,
                localViewHeight = 150
            )
        }
    }
}