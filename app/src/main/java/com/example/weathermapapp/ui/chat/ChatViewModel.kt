package com.example.weathermapapp.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weathermapapp.data.model.ChatMessage
import com.example.weathermapapp.data.repository.AuthRepository
import com.example.weathermapapp.data.repository.ChatRepository
import com.example.weathermapapp.data.repository.OllamaRepository
import com.example.weathermapapp.util.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
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
    private val _ttsEnabled = MutableLiveData(false)
    val ttsEnabled: LiveData<Boolean> = _ttsEnabled

    private lateinit var otherUserId: String
    private var chatPartnerModel: String? = null
    private var ttsManager: TtsManager? = null

    fun setChatPartner(otherUserId: String, model: String?) {
        this.otherUserId = otherUserId
        this.chatPartnerModel = model
        loadMessages()
    }

    fun setTtsManager(manager: TtsManager) {
        ttsManager = manager
    }

    fun toggleTts() {
        _ttsEnabled.value = !(_ttsEnabled.value ?: false)
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

        val userMessage = ChatMessage(
            id = System.currentTimeMillis().toString(),
            senderId = currentUserId,
            receiverId = otherUserId,
            message = messageText,
            timestamp = Date()
        )
        _messages.value = _messages.value.orEmpty() + userMessage

        chatPartnerModel?.let { model ->
            viewModelScope.launch {
                _isLoading.value = true
                val botMessageId = System.currentTimeMillis().toString()
                val initialBotMessage = ChatMessage(
                    id = botMessageId,
                    senderId = otherUserId,
                    receiverId = currentUserId,
                    message = "...",
                    timestamp = Date()
                )
                _messages.value = _messages.value.orEmpty() + initialBotMessage

                val fullResponse = StringBuilder()

                ollamaRepository.getLlamaResponseStream(messageText, model)
                    .onCompletion {
                        if (_ttsEnabled.value == true && fullResponse.isNotEmpty()) {
                            ttsManager?.speak(fullResponse.toString())
                        }
                        _isLoading.value = false
                    }
                    .catch { e ->
                        val currentMessages = _messages.value.orEmpty().toMutableList()
                        val botMessageIndex = currentMessages.indexOfFirst { it.id == botMessageId }
                        if (botMessageIndex != -1) {
                            currentMessages[botMessageIndex] = currentMessages[botMessageIndex].copy(message = "Error: ${e.message}")
                            _messages.value = currentMessages
                        }
                    }
                    .collect { token ->
                        val currentMessages = _messages.value.orEmpty().toMutableList()
                        val botMessageIndex = currentMessages.indexOfFirst { it.id == botMessageId }
                        if (botMessageIndex != -1) {
                            val existingMessage = currentMessages[botMessageIndex]
                            val newMessageText = if (existingMessage.message == "...") {
                                token
                            } else {
                                existingMessage.message + token
                            }
                            fullResponse.append(token)
                            currentMessages[botMessageIndex] = existingMessage.copy(message = newMessageText)
                            _messages.value = currentMessages
                        }
                    }
            }
        } ?: run {
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

    override fun onCleared() {
        super.onCleared()
        ttsManager?.shutdown()
    }
}
