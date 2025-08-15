package com.example.weathermapapp.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.weathermapapp.databinding.ActivityVideoCallBinding
import dagger.hilt.android.AndroidEntryPoint
import org.webrtc.EglBase
import org.webrtc.RendererCommon

@AndroidEntryPoint
class VideoCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoCallBinding
    private val viewModel: VideoCallViewModel by viewModels()
    private lateinit var targetUserId: String

    private val eglBaseContext: EglBase.Context = EglBase.create().eglBaseContext

    companion object {
        private const val CAMERA_MIC_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        targetUserId = intent.getStringExtra("target") ?: run {
            Toast.makeText(this, "Target user not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        checkPermissionsAndInitialize()
        setupClickListeners()
        observeViewModel()
    }

    private fun checkPermissionsAndInitialize() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), CAMERA_MIC_PERMISSION_REQUEST_CODE)
        } else {
            initializeCall()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_MIC_PERMISSION_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            initializeCall()
        } else {
            Toast.makeText(this, "Camera and Mic permissions are required for video call", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeCall() {
        viewModel.init(targetUserId)

        binding.localView.init(eglBaseContext, null)
        binding.localView.setMirror(true)
        binding.localView.setEnableHardwareScaler(true)
        binding.localView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)


        binding.remoteView.init(eglBaseContext, null)
        binding.remoteView.setMirror(false)
        binding.remoteView.setEnableHardwareScaler(true)
        binding.remoteView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)

        viewModel.startCall()
    }

    private fun setupClickListeners() {
        binding.btnCallEnd.setOnClickListener {
            viewModel.endCall()
        }
        binding.btnSwitchCamera.setOnClickListener {
            viewModel.switchCamera()
        }
        var isMicMuted = false
        binding.btnMicOff.setOnClickListener {
            isMicMuted = !isMicMuted
            viewModel.toggleMic(isMicMuted)
            binding.btnMicOff.alpha = if (isMicMuted) 0.5f else 1.0f
        }
    }

    private fun observeViewModel() {
        viewModel.localVideoTrack.observe(this) { track ->
            track.addSink(binding.localView)
        }
        viewModel.remoteVideoTrack.observe(this) { track ->
            track.addSink(binding.remoteView)
        }
        viewModel.callEnded.observe(this) {
            if (it) {
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.localView.release()
        binding.remoteView.release()
    }
}