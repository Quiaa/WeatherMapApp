package com.example.weathermapapp.data.repository.webrtc

import com.example.weathermapapp.data.model.webrtc.NSDataModel
import com.example.weathermapapp.data.model.webrtc.NSDataModelType
import com.google.gson.Gson
import org.webrtc.*
import javax.inject.Inject

class MainRepository @Inject constructor(
    private val firebaseClient: FirebaseClient,
    private val webRTCClient: NSWebRTCClient,
    private val gson: Gson
) : NSWebRTCClient.Listener {

    private var target: String? = null
    private var currentUsername: String? = null
    var listener: Listener? = null

    init {
        webRTCClient.listener = this
    }

    fun initWebrtcClient(username: String, observer: PeerConnection.Observer) {
        currentUsername = username
        webRTCClient.initializeWebrtcClient(observer)
    }

    fun initFirebase() {
        firebaseClient.subscribeForLatestEvent { event ->
            when (event.type) {
                NSDataModelType.Offer -> {
                    webRTCClient.onRemoteSessionReceived(
                        SessionDescription(SessionDescription.Type.OFFER, event.data.toString())
                    )
                    target = event.sender
                    webRTCClient.answer(event.sender)
                }
                NSDataModelType.Answer -> {
                    webRTCClient.onRemoteSessionReceived(
                        SessionDescription(SessionDescription.Type.ANSWER, event.data.toString())
                    )
                }
                NSDataModelType.IceCandidates -> {
                    val candidate = try {
                        gson.fromJson(event.data.toString(), IceCandidate::class.java)
                    } catch (e: Exception) {
                        null
                    }
                    candidate?.let { webRTCClient.addIceCandidateToPeer(it) }
                }
                NSDataModelType.StartVideoCall -> {
                    target = event.sender
                    listener?.onCallRequestReceived(event)
                }
                NSDataModelType.EndCall -> {
                    listener?.onCallEnded()
                }
                else -> Unit
            }
        }
    }

    fun setTarget(targetUsername: String) {
        target = targetUsername
    }

    fun startCall() {
        target?.let {
            webRTCClient.call(it)
        }
    }

    fun sendCallRequest(target: String, isVideoCall: Boolean) {
        currentUsername?.let {
            firebaseClient.sendEvent(
                NSDataModel(
                    if (isVideoCall) NSDataModelType.StartVideoCall else NSDataModelType.StartAudioCall,
                    it,
                    target
                )
            )
        }
    }

    fun endCall() {
        target?.let {
            currentUsername?.let { cUser ->
                firebaseClient.sendEvent(NSDataModel(NSDataModelType.EndCall, cUser, it))
            }
        }
        webRTCClient.closeConnection()
        firebaseClient.clearLatestEvent()
    }

    fun sendIceCandidate(target: String, iceCandidate: IceCandidate) {
        webRTCClient.sendIceCandidate(target, iceCandidate)
    }

    fun initLocalSurfaceView(view: SurfaceViewRenderer, localView: SurfaceViewRenderer){
        webRTCClient.initLocalSurfaceView(view, localView)
    }

    fun initRemoteSurfaceView(view: SurfaceViewRenderer){
        webRTCClient.initRemoteSurfaceView(view)
    }

    fun switchCamera() {
        webRTCClient.switchCamera()
    }

    fun toggleAudio(isMuted: Boolean) {
        webRTCClient.toggleAudio(isMuted)
    }

    override fun onTransferDataToOtherPeer(model: NSDataModel) {
        // Update the sender and target here and send it.
        currentUsername?.let { cUser->
            target?.let { targetUser ->
                firebaseClient.sendEvent(model.copy(sender = cUser, target = targetUser))
            }
        }
    }

    interface Listener {
        fun onCallRequestReceived(model: NSDataModel)
        fun onCallEnded()
    }
}