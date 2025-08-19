package com.example.weathermapapp.data.repository.webrtc

import android.content.Context
import com.example.weathermapapp.data.model.webrtc.NSDataModel
import com.example.weathermapapp.data.model.webrtc.NSDataModelType
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import javax.inject.Inject

class NSWebRTCClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {

    private val eglBaseContext: EglBase.Context = EglBase.create().eglBaseContext
    private val peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private val iceServer = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun.ekiga.net").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun.ideasip.com").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun.rixtelecom.se").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun.schlund.de").createIceServer(),
        PeerConnection.IceServer.builder("turn:turn.bistri.com:80").setUsername("homeo").setPassword("homeo").createIceServer(),
        PeerConnection.IceServer.builder("turn:turn.anyfirewall.com:443?transport=tcp").setUsername("webrtc").setPassword("webrtc").createIceServer(),
        PeerConnection.IceServer.builder("turn:numb.viagenie.ca").setUsername("webrtc@live.com").setPassword("muazkh").createIceServer()
    )
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }
    private var videoCapturer: CameraVideoCapturer? = null
    private var localStream: MediaStream? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null

    var listener: Listener? = null

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        peerConnectionFactory = createPeerConnectionFactory()
    }

    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        val options = PeerConnectionFactory.Options()
        return PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBaseContext, true, true))
            .setOptions(options)
            .createPeerConnectionFactory()
    }

    fun initializeWebrtcClient(observer: PeerConnection.Observer) {
        // Create a new PeerConnection for each new call.
        peerConnection = createPeerConnection(observer)
    }

    private fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection? {
        return peerConnectionFactory.createPeerConnection(iceServer, observer)
    }

    fun initLocalSurfaceView(view: SurfaceViewRenderer, localView: SurfaceViewRenderer) {
        initSurfaceView(view)
        startLocalStreaming(localView)
    }

    fun initRemoteSurfaceView(view: SurfaceViewRenderer) {
        initSurfaceView(view)
    }

    private fun initSurfaceView(view: SurfaceViewRenderer) {
        view.init(eglBaseContext, null)
        view.setEnableHardwareScaler(true)
        view.setMirror(true)
    }

    private fun startLocalStreaming(localView: SurfaceViewRenderer) {
        localStream = peerConnectionFactory.createLocalMediaStream("local_stream")
        localAudioTrack = peerConnectionFactory.createAudioTrack("local_audio_track", localAudioSource)
        localStream?.addTrack(localAudioTrack)

        videoCapturer = getVideoCapturer(context)
        val surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, eglBaseContext)
        videoCapturer?.initialize(surfaceTextureHelper, context, localVideoSource.capturerObserver)
        videoCapturer?.startCapture(480, 640, 30)

        localVideoTrack = peerConnectionFactory.createVideoTrack("local_video_track", localVideoSource)
        localVideoTrack?.addSink(localView)
        localStream?.addTrack(localVideoTrack)
        // Add media stream to the PeerConnection.
        peerConnection?.addStream(localStream)
    }

    private fun getVideoCapturer(context: Context): CameraVideoCapturer {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames

        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        return enumerator.createCapturer(deviceNames.first(), null)
    }

    fun call(target: String) {
        val mediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }
        peerConnection?.createOffer(object : MySdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                super.onCreateSuccess(desc)
                peerConnection?.setLocalDescription(object : MySdpObserver() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        listener?.onTransferDataToOtherPeer(
                            NSDataModel(NSDataModelType.Offer, "", target, desc?.description)
                        )
                    }
                }, desc)
            }
        }, mediaConstraints)
    }

    fun answer(target: String) {
        val mediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }
        peerConnection?.createAnswer(object : MySdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                super.onCreateSuccess(desc)
                peerConnection?.setLocalDescription(object : MySdpObserver() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        listener?.onTransferDataToOtherPeer(
                            NSDataModel(NSDataModelType.Answer, "", target, desc?.description)
                        )
                    }
                }, desc)
            }
        }, mediaConstraints)
    }

    fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        peerConnection?.setRemoteDescription(MySdpObserver(), sessionDescription)
    }

    fun addIceCandidateToPeer(iceCandidate: IceCandidate) {
        peerConnection?.addIceCandidate(iceCandidate)
    }

    fun sendIceCandidate(target: String, iceCandidate: IceCandidate) {
        addIceCandidateToPeer(iceCandidate)
        listener?.onTransferDataToOtherPeer(
            NSDataModel(NSDataModelType.IceCandidates, "", target, gson.toJson(iceCandidate))
        )
    }

    fun closeConnection() {
        try {
            localAudioTrack?.setEnabled(false)
            localVideoTrack?.setEnabled(false)

            videoCapturer?.stopCapture()
            videoCapturer?.dispose()

            localStream?.let {
                it.removeTrack(localAudioTrack)
                it.removeTrack(localVideoTrack)
                localAudioTrack?.dispose()
                localVideoTrack?.dispose()
                peerConnection?.removeStream(it)
                it.dispose()
            }
            localStream = null

            peerConnection?.close()
            peerConnection = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun switchCamera() {
        videoCapturer?.switchCamera(null)
    }

    fun toggleAudio(isMuted: Boolean) {
        localAudioTrack?.setEnabled(!isMuted)
    }

    interface Listener {
        fun onTransferDataToOtherPeer(model: NSDataModel)
    }
}

open class MySdpObserver : SdpObserver {
    override fun onCreateSuccess(p0: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(p0: String?) {}
    override fun onSetFailure(p0: String?) {}
}