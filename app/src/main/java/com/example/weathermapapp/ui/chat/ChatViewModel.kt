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
import kotlinx.coroutines.flow.catch
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
        // If chatPartnerModel is null, it's a real user.
        if (chatPartnerModel == null) {
            viewModelScope.launch {
                val currentUserId = authRepository.getCurrentUserId()
                if (currentUserId != null) {
                    chatRepository.getChatMessages(currentUserId, otherUserId).collect {
                        _messages.value = it
                    }
                }
            }
        } else {
            // If there is a model name, it's a bot. Initialize the conversation as empty.
            _messages.value = emptyList()
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
                val botMessageId = System.currentTimeMillis().toString()
                val initialBotMessage = ChatMessage(
                    id = botMessageId,
                    senderId = otherUserId,
                    receiverId = currentUserId,
                    message = "...", // Waiting message
                    timestamp = Date()
                )
                _messages.value = _messages.value.orEmpty() + initialBotMessage

                // Start listening to the flow.
                ollamaRepository.getLlamaResponseStream(messageText, model)
                    .catch { e ->
                        // If there is an error, update the bot message.
                        val currentMessages = _messages.value.orEmpty().toMutableList()
                        val botMessageIndex = currentMessages.indexOfFirst { it.id == botMessageId }
                        if (botMessageIndex != -1) {
                            currentMessages[botMessageIndex] = currentMessages[botMessageIndex].copy(
                                message = "Error: ${e.message}"
                            )
                            _messages.value = currentMessages
                        }
                        _isLoading.value = false
                    }
                    .collect { token ->
                        // 4. When a new token (word) arrives, update the bot message.
                        val currentMessages = _messages.value.orEmpty().toMutableList()
                        val botMessageIndex = currentMessages.indexOfFirst { it.id == botMessageId }
                        if (botMessageIndex != -1) {
                            val existingMessage = currentMessages[botMessageIndex]
                            // If the message is "...", replace it with the incoming token; otherwise, add the token to the end.
                            val newMessageText = if (existingMessage.message == "...") {
                                token
                            } else {
                                existingMessage.message + token
                            }
                            currentMessages[botMessageIndex] = existingMessage.copy(message = newMessageText)
                            _messages.value = currentMessages
                        }
                        _isLoading.value = false // When the first token arrives, close the loading indicator.
                    }
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
