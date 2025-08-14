package com.example.weathermapapp.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weathermapapp.data.model.ChatMessage
import com.example.weathermapapp.data.repository.AuthRepository
import com.example.weathermapapp.data.repository.ChatRepository
import com.example.weathermapapp.data.repository.OllamaRepository
import com.example.weathermapapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val ollamaRepository: OllamaRepository
) : ViewModel() {

    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messages: LiveData<List<ChatMessage>> = _messages
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private lateinit var otherUserId: String
    private var chatPartnerModel: String? = null

    fun setChatPartner(otherUserId: String, model: String?) {
        this.otherUserId = otherUserId
        this.chatPartnerModel = model
        loadMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId != null) {
                chatRepository.getChatMessages(currentUserId, otherUserId).collect {
                    _messages.value = it
                }
            }
        }
    }

    fun sendMessage(messageText: String) {
        val currentUserId = authRepository.getCurrentUserId() ?: return

        // Display the user's sent message immediately on the screen.
        val userMessage = ChatMessage(
            id = System.currentTimeMillis().toString(), // Temp ID
            senderId = currentUserId,
            receiverId = otherUserId,
            message = messageText,
            timestamp = Date()
        )
        _messages.value = _messages.value.orEmpty() + userMessage

        // If there is a model name (i.e. it's a bot conversation), ask OLLAMA.
        chatPartnerModel?.let { model ->
            viewModelScope.launch {
                _isLoading.value = true
                // We're sending the dynamic model name to the repository.
                when (val result = ollamaRepository.getLlamaResponse(messageText, model)) {
                    is Resource.Success -> {
                        val botMessage = ChatMessage(
                            id = System.currentTimeMillis().toString(),
                            senderId = otherUserId, // Bot ID
                            receiverId = currentUserId,
                            message = result.data ?: "Sorry, couldn't generate an answer.",
                            timestamp = Date()
                        )
                        _messages.value = _messages.value.orEmpty() + botMessage
                    }
                    is Resource.Error -> {
                        val errorMessage = ChatMessage(
                            id = System.currentTimeMillis().toString(),
                            senderId = otherUserId, // Bot ID
                            receiverId = currentUserId,
                            message = "Error: ${result.message}",
                            timestamp = Date()
                        )
                        _messages.value = _messages.value.orEmpty() + errorMessage
                    }
                    else -> {}
                }
                _isLoading.value = false
            }
        } ?: run {
            // If there is no model name (i.e., it's a real user), send to Firebase.
            viewModelScope.launch {
                val message = ChatMessage(
                    senderId = currentUserId,
                    receiverId = otherUserId,
                    message = messageText
                )
                chatRepository.sendMessage(message)
            }
        }
    }

    fun getCurrentUserId(): String {
        return authRepository.getCurrentUserId() ?: ""
    }
}
