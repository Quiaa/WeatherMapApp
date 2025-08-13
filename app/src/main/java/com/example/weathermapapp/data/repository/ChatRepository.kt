package com.example.weathermapapp.data.repository

import com.example.weathermapapp.data.model.ChatMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    fun getChatMessages(currentUserId: String, otherUserId: String): Flow<List<ChatMessage>> = callbackFlow {
        val chatRoomId = getChatRoomId(currentUserId, otherUserId)
        val messagesCollection = firestore.collection("chats").document(chatRoomId).collection("messages")

        val listenerRegistration = messagesCollection
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull {
                        val chatMessage = it.toObject(ChatMessage::class.java)
                        chatMessage?.copy(id = it.id)
                    }
                    trySend(messages).isSuccess
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    suspend fun sendMessage(message: ChatMessage) {
        val chatRoomId = getChatRoomId(message.senderId, message.receiverId)
        firestore.collection("chats").document(chatRoomId).collection("messages").add(message)
    }

    private fun getChatRoomId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "${userId1}_${userId2}"
        } else {
            "${userId2}_${userId1}"
        }
    }
}
