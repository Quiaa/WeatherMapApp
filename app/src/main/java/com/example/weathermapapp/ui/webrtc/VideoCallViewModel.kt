package com.example.weathermapapp.ui.webrtc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.weathermapapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject

@HiltViewModel
class VideoCallViewModel @Inject constructor(
    private val webRTCService: WebRTCService,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _callEnded = MutableLiveData<Boolean>()
    val callEnded: LiveData<Boolean> = _callEnded

    private val _isMuted = MutableLiveData(false)
    val isMuted: LiveData<Boolean> = _isMuted

    private var isCallEnding = false

    init {
        webRTCService.listener = object : WebRTCService.Listener {
            override fun onCallRequestReceived(model: com.example.weathermapapp.data.model.webrtc.NSDataModel) {
                // This ViewModel only exists when the call screen is present, so this event won't be processed here.
            }

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
            webRTCService.endCall()
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
}