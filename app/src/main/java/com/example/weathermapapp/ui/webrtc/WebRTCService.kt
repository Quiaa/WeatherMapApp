package com.example.weathermapapp.ui.webrtc

import com.example.weathermapapp.data.model.webrtc.NSDataModel
import com.example.weathermapapp.data.repository.webrtc.MainRepository
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebRTCService @Inject constructor(
    private val mainRepository: MainRepository
) : MainRepository.Listener {

    var listener: Listener? = null
    var remoteSurfaceView: SurfaceViewRenderer? = null
    private var targetUser: String? = null // To target the user.



    init {
        mainRepository.listener = this
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
        mainRepository.initFirebase()
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

    fun endCall() {
        mainRepository.endCall()
        // We're also informing our own UI that the search has been completed.
        listener?.onCallEnded()
        targetUser = null // Clear the target when the search is finished.
    }

    fun switchCamera() {
        mainRepository.switchCamera()
    }

    fun toggleAudio(isMuted: Boolean) {
        mainRepository.toggleAudio(isMuted)
    }

    override fun onCallRequestReceived(model: NSDataModel) {
        listener?.onCallRequestReceived(model)
    }

    override fun onCallEnded() {
        listener?.onCallEnded()
    }

    interface Listener {
        fun onCallRequestReceived(model: NSDataModel)
        fun onCallEnded()
    }
}