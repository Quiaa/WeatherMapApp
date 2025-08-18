package com.example.weathermapapp.data.repository.webrtc

import com.example.weathermapapp.data.model.webrtc.NSDataModel
import com.example.weathermapapp.data.model.webrtc.NSDataModelType
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainRepository @Inject constructor(
    private val firebaseClient: FirebaseClient,
    private val webRTCClient: NSWebRTCClient,
    private val gson: Gson
) : NSWebRTCClient.Listener {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var target: String? = null
    private var currentUsername: String? = null

    private var isSignalingReady = false
    private val pendingEvents = mutableListOf<NSDataModel>()

    private val _signalingEvent = MutableSharedFlow<NSDataModel>(replay = 1)
    val signalingEvent = _signalingEvent.asSharedFlow()

    init {
        webRTCClient.listener = this
        initFirebase()
    }

    fun initWebrtcClient(username: String, observer: PeerConnection.Observer) {
        currentUsername = username
        webRTCClient.initializeWebrtcClient(observer)
    }

    private fun initFirebase() {
        firebaseClient.subscribeForLatestEvent { event ->
            if (event.type == NSDataModelType.StartVideoCall) {
                target = event.sender
                _signalingEvent.tryEmit(event)
                return@subscribeForLatestEvent
            }

            if (isSignalingReady) {
                handleEvent(event)
            } else {
                when (event.type) {
                    NSDataModelType.Offer, NSDataModelType.Answer, NSDataModelType.IceCandidates -> {
                        pendingEvents.add(event)
                    }
                    else -> { /* Ignore */ }
                }
            }
        }
    }

    fun onViewsReady() {
        isSignalingReady = true
        processPendingEvents()
    }

    private fun processPendingEvents() {
        pendingEvents.forEach { handleEvent(it) }
        pendingEvents.clear()
    }

    private fun resetState() {
        isSignalingReady = false
        target = null
        pendingEvents.clear()
    }

    private fun handleEvent(event: NSDataModel) {
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
            NSDataModelType.EndCall -> {
                _signalingEvent.tryEmit(event)
                firebaseClient.clearLatestEvent()
                webRTCClient.closeConnection()
                resetState()
            }
            else -> Unit
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
        repositoryScope.launch {
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
    }

    suspend fun endCall() {
        target?.let {
            currentUsername?.let { cUser ->
                firebaseClient.sendEvent(NSDataModel(NSDataModelType.EndCall, cUser, it))
            }
        }
        firebaseClient.clearLatestEvent()
        webRTCClient.closeConnection()
        resetState()
    }

    suspend fun endCall(sender: String, target: String) {
        firebaseClient.sendEvent(NSDataModel(NSDataModelType.EndCall, sender, target))
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
        repositoryScope.launch {
            // Update the sender and target here and send it.
            currentUsername?.let { cUser ->
                target?.let { targetUser ->
                    firebaseClient.sendEvent(model.copy(sender = cUser, target = targetUser))
                }
            }
        }
    }
}