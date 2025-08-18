package com.example.weathermapapp.ui.webrtc

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.weathermapapp.data.repository.AuthRepository
import com.example.weathermapapp.databinding.ActivityIncomingCallBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class IncomingCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncomingCallBinding
    private var callerId: String? = null

    @Inject
    lateinit var webRTCService: WebRTCService
    @Inject
    lateinit var authRepository: AuthRepository


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncomingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        callerId = intent.getStringExtra("callerId")

        if (callerId == null) {
            finish() // Cannot proceed without a caller ID
            return
        }

        binding.tvCallerId.text = "Incoming call from:\n$callerId"

        binding.btnAccept.setOnClickListener {
            val intent = Intent(this, VideoCallActivity::class.java).apply {
                putExtra("target", callerId)
                putExtra("isCaller", false)
            }
            startActivity(intent)
            finish()
        }

        binding.btnDecline.setOnClickListener {
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId != null) {
                webRTCService.endCall(currentUserId, callerId!!)
            }
            finish()
        }
    }
}
