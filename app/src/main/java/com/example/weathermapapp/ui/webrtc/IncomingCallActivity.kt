package com.example.weathermapapp.ui.webrtc

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.weathermapapp.data.repository.AuthRepository
import com.example.weathermapapp.databinding.ActivityIncomingCallBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class IncomingCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncomingCallBinding
    private var callerId: String? = null
    private val viewModel: IncomingCallViewModel by viewModels()

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
        viewModel.fetchCallerName(callerId!!)

        viewModel.callerName.observe(this) { name ->
            binding.tvCallerId.text = "Incoming call from:\n$name"
        }

        binding.btnAccept.setOnClickListener {
            val intent = Intent(this, VideoCallActivity::class.java).apply {
                putExtra("target", callerId)
                putExtra("isCaller", false)
            }
            startActivity(intent)
            finish()
        }

        binding.btnDecline.setOnClickListener {
            lifecycleScope.launch {
                val currentUserId = authRepository.getCurrentUserId()
                if (currentUserId != null) {
                    webRTCService.endCall(currentUserId, callerId!!)
                }
            }
            finish()
        }
    }
}
