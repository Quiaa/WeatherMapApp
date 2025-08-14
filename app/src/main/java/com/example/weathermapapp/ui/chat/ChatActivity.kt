package com.example.weathermapapp.ui.chat

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weathermapapp.databinding.ActivityChatBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val chatViewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter

    private var otherUserId: String? = null
    private var otherUserName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        otherUserId = intent.getStringExtra("USER_ID")
        otherUserName = intent.getStringExtra("USER_NAME")
        val modelName = intent.getStringExtra("MODEL_NAME")

        if (otherUserId == null || otherUserName == null) {
            // Handle error: close activity or show a message
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        chatViewModel.setChatPartner(otherUserId!!, modelName)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = otherUserName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(chatViewModel.getCurrentUserId())
        binding.rvChatMessages.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSend.setOnClickListener {
            if (chatViewModel.isLoading.value == true) {
                Toast.makeText(this, "Please wait for the bot's response to finish..", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val messageText = binding.etMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                chatViewModel.sendMessage(messageText)
                binding.etMessage.text.clear()
            }
        }
    }

    private fun observeViewModel() {
        chatViewModel.messages.observe(this) { messages ->
            chatAdapter.submitList(messages) {
                // Scroll to the bottom to show the latest message
                if (messages.isNotEmpty()) {
                    binding.rvChatMessages.scrollToPosition(messages.size - 1)
                }
            }
        }
        chatViewModel.isLoading.observe(this) { isLoading ->
            binding.etMessage.isEnabled = !isLoading
            binding.btnSend.isEnabled = !isLoading
        }
    }
}
