package com.example.weathermapapp.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weathermapapp.data.model.ChatMessage
import com.example.weathermapapp.data.repository.AuthRepository
import com.example.weathermapapp.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messages: LiveData<List<ChatMessage>> = _messages

    private lateinit var otherUserId: String

    fun setChatPartner(otherUserId: String) {
        this.otherUserId = otherUserId
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
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId != null) {
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
