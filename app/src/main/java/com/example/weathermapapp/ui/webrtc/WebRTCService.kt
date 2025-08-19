package com.example.weathermapapp.ui.webrtc

import com.example.weathermapapp.data.model.webrtc.NSDataModelType
import com.example.weathermapapp.data.repository.webrtc.MainRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebRTCService @Inject constructor(
    private val mainRepository: MainRepository
) {

    var listener: Listener? = null
    var remoteSurfaceView: SurfaceViewRenderer? = null
    private var targetUser: String? = null // To target the user.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)


    init {
        mainRepository.signalingEvent.onEach {
            when (it.type) {
                NSDataModelType.EndCall -> {
                    listener?.onCallEnded()
                }
                else -> {
                }
            }
        }.launchIn(serviceScope)
    }

    fun initialize(username: String) {
        mainRepository.initWebrtcClient(username, object : PeerConnection.Observer {
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}

            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    targetUser?.let { target ->
                        mainRepository.sendIceCandidate(target, it)
                    }
                }
            }

            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}

            override fun onAddStream(stream: MediaStream?) {
                // We are using the Post method to run UI operations on the main thread
                remoteSurfaceView?.post {
                    stream?.videoTracks?.firstOrNull()?.addSink(remoteSurfaceView)
                }
            }

            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}

            override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                // We are using the Post method to perform the UI operation in the Main/UI Thread
                remoteSurfaceView?.post {
                    mediaStreams?.firstOrNull()?.videoTracks?.firstOrNull()?.addSink(remoteSurfaceView)
                }
            }
        })
    }

    fun setupViews(localView: SurfaceViewRenderer, remoteView: SurfaceViewRenderer, target: String, isCaller: Boolean) {
        this.remoteSurfaceView = remoteView
        this.targetUser = target
        mainRepository.initLocalSurfaceView(localView, localView)
        mainRepository.initRemoteSurfaceView(remoteView)
        mainRepository.setTarget(target)
        mainRepository.onViewsReady()

        if (isCaller) {
            mainRepository.startCall()
        }
    }

    fun sendCallRequest(target: String, isVideoCall: Boolean) {
        mainRepository.sendCallRequest(target, isVideoCall)
    }

    suspend fun endCall() {
        mainRepository.endCall()
        // We're also informing our own UI that the search has been completed.
        listener?.onCallEnded()
        targetUser = null // Clear the target when the search is finished.
    }

    suspend fun endCall(sender: String, target: String) {
        mainRepository.endCall(sender, target)
    }

    fun switchCamera() {
        mainRepository.switchCamera()
    }

    fun toggleAudio(isMuted: Boolean) {
        mainRepository.toggleAudio(isMuted)
    }

    interface Listener {
        fun onCallEnded()
    }
}