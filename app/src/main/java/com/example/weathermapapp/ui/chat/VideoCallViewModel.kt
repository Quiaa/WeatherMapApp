package com.example.weathermapapp.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.weathermapapp.data.repository.AuthRepository
import com.example.weathermapapp.data.repository.WebRTCRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.webrtc.*
import javax.inject.Inject

@HiltViewModel
class VideoCallViewModel @Inject constructor(
    private val app: Application,
    private val authRepository: AuthRepository,
    private val webRTCRepository: WebRTCRepository
) : AndroidViewModel(app) {

    private val _localVideoTrack = MutableLiveData<VideoTrack>()
    val localVideoTrack: LiveData<VideoTrack> = _localVideoTrack

    private val _remoteVideoTrack = MutableLiveData<VideoTrack>()
    val remoteVideoTrack: LiveData<VideoTrack> = _remoteVideoTrack

    private val _callEnded = MutableLiveData<Boolean>()
    val callEnded: LiveData<Boolean> = _callEnded

    private var peerConnection: PeerConnection? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private lateinit var targetUserId: String

    fun init(targetId: String) {
        this.targetUserId = targetId
        initializeWebRTC()
    }

    private fun initializeWebRTC() {
        val currentUserId = authRepository.getCurrentUserId() ?: return

        // 1. PeerConnectionFactory'yi başlat
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(app)
                .createInitializationOptions()
        )

        val peerConnectionFactory = PeerConnectionFactory.builder().createPeerConnectionFactory()

        // 2. Video kaynağını ve track'i oluştur
        val eglBaseContext = EglBase.create().eglBaseContext
        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext)
        videoCapturer = createCameraCapturer()
        val videoSource = peerConnectionFactory.createVideoSource(videoCapturer!!.isScreencast)
        videoCapturer!!.initialize(surfaceTextureHelper, app, videoSource.capturerObserver)
        videoCapturer!!.startCapture(480, 640, 30)
        val localVideoTrack = peerConnectionFactory.createVideoTrack("local_video_track", videoSource)
        _localVideoTrack.postValue(localVideoTrack)


        // 3. PeerConnection'ı oluştur
        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        )
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    viewModelScope.launch {
                        webRTCRepository.sendIceCandidate(targetUserId, it)
                    }
                }
            }

            override fun onAddStream(stream: MediaStream?) {
                stream?.videoTracks?.firstOrNull()?.let {
                    _remoteVideoTrack.postValue(it)
                }
            }

            // HATA ÇÖZÜMÜ: Eksik olan metotları ekleyin
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                // Bu metodu boş bırakabilirsiniz.
            }

            override fun onDataChannel(p0: DataChannel?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                // Bu metodu da boş bırakabilirsiniz.
            }
        })

        // 4. Yerel stream'i PeerConnection'a ekle
        val mediaStream = peerConnectionFactory.createLocalMediaStream("local_stream")
        mediaStream.addTrack(localVideoTrack)
        peerConnection?.addStream(mediaStream)

        // Sinyalizasyon dinleyicilerini başlat
        webRTCRepository.observeSdp(currentUserId) { sdp ->
            handleSdp(sdp)
        }
        webRTCRepository.observeIceCandidates(currentUserId) { candidate ->
            peerConnection?.addIceCandidate(candidate)
        }
    }

    fun startCall() {
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    peerConnection?.setLocalDescription(SdpObserverAdapter, it)
                    viewModelScope.launch {
                        webRTCRepository.sendSdp(targetUserId, it)
                    }
                }
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
            override fun onSetSuccess() {}
        }, MediaConstraints())
    }

    private fun handleSdp(sdp: SessionDescription) {
        if (sdp.type == SessionDescription.Type.OFFER) {
            peerConnection?.setRemoteDescription(SdpObserverAdapter, sdp)
            peerConnection?.createAnswer(object : SdpObserver {
                override fun onCreateSuccess(answerSdp: SessionDescription?) {
                    answerSdp?.let {
                        peerConnection?.setLocalDescription(SdpObserverAdapter, it)
                        viewModelScope.launch {
                            webRTCRepository.sendSdp(targetUserId, it)
                        }
                    }
                }
                override fun onCreateFailure(p0: String?) {}
                override fun onSetFailure(p0: String?) {}
                override fun onSetSuccess() {}
            }, MediaConstraints())
        } else if (sdp.type == SessionDescription.Type.ANSWER) {
            peerConnection?.setRemoteDescription(SdpObserverAdapter, sdp)
        }
    }

    private fun createCameraCapturer(): CameraVideoCapturer {
        val enumerator = Camera2Enumerator(app)
        val deviceNames = enumerator.deviceNames

        // Önce ön kamerayı dene
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        // Ön kamera yoksa arka kamerayı dene
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        throw IllegalStateException("No camera available.")
    }

    fun switchCamera() {
        videoCapturer?.switchCamera(null)
    }

    fun toggleMic(isMuted: Boolean) {
        peerConnection?.senders?.find { it.track()?.kind() == "audio" }?.track()?.setEnabled(!isMuted)
    }

    fun endCall() {
        val currentUserId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            webRTCRepository.endCall(currentUserId, targetUserId)
        }
        _callEnded.postValue(true)
    }

    override fun onCleared() {
        super.onCleared()
        videoCapturer?.dispose()
        peerConnection?.dispose()
    }

    // SdpObserver için boş bir adaptör
    object SdpObserverAdapter : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) {}
        override fun onSetFailure(p0: String?) {}
    }
}