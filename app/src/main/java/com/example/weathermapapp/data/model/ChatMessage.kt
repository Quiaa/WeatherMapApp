package com.example.weathermapapp.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
)
