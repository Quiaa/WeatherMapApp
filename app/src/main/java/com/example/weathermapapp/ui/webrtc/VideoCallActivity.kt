package com.example.weathermapapp.ui.webrtc

import android.Manifest
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.weathermapapp.databinding.ActivityVideoCallBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VideoCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoCallBinding
    private val viewModel: VideoCallViewModel by viewModels()

    private var targetUserId: String? = null
    private var isCaller: Boolean = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true && permissions[Manifest.permission.RECORD_AUDIO] == true) {
            setupCall()
        } else {
            Toast.makeText(this, "Camera and Microphone permissions are required.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        targetUserId = intent.getStringExtra("target")
        isCaller = intent.getBooleanExtra("isCaller", false)

        if (targetUserId == null) {
            finish()
            return
        }

        checkPermissionsAndSetupCall()
        setupClickListeners()
        observeViewModel()
    }

    private fun checkPermissionsAndSetupCall() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            setupCall()
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    private fun setupCall() {
        val currentUserId = viewModel.getCurrentUserId()
        if (currentUserId != null) {
            viewModel.init(currentUserId)
            viewModel.setupViews(binding.localView, binding.remoteView, targetUserId!!, isCaller)
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupClickListeners() {
        binding.btnEndCall.setOnClickListener {
            viewModel.endCall()
        }
        binding.btnSwitchCamera.setOnClickListener {
            viewModel.switchCamera()
        }
        binding.btnMic.setOnClickListener {
            viewModel.toggleMic()
        }
    }

    private fun observeViewModel() {
        viewModel.callEnded.observe(this) {
            if (it) {
                Toast.makeText(this, "Call Ended", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        viewModel.isMuted.observe(this) { isMuted ->
            // Update the UI of the microphone button.
            if (isMuted) {
                // Change the icon.
            } else {
                // Change the icon.
            }
        }
    }

    override fun onDestroy() {
        viewModel.endCall()
        super.onDestroy()
        binding.localView.release()
        binding.remoteView.release()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(9, 16))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            binding.localView.visibility = View.GONE
            binding.btnEndCall.visibility = View.GONE
            binding.btnSwitchCamera.visibility = View.GONE
            binding.btnMic.visibility = View.GONE
        } else {
            binding.localView.visibility = View.VISIBLE
            binding.btnEndCall.visibility = View.VISIBLE
            binding.btnSwitchCamera.visibility = View.VISIBLE
            binding.btnMic.visibility = View.VISIBLE
        }
    }
}