package com.example.weathermapapp.data.repository.webrtc

import com.example.weathermapapp.data.model.webrtc.NSDataModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseClient @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val gson: Gson
) {

    private var latestEventCallback: ((NSDataModel) -> Unit)? = null
    private val currentUserId: String?
        get() = auth.currentUser?.uid

    suspend fun sendEvent(event: NSDataModel) {
        val eventMap = gson.fromJson(gson.toJson(event), HashMap::class.java)
        firestore.collection("webrtc_events").document(event.target).set(eventMap).await()
    }

    fun subscribeForLatestEvent(callback: (NSDataModel) -> Unit) {
        latestEventCallback = callback
        currentUserId?.let { userId ->
            firestore.collection("webrtc_events").document(userId)
                .addSnapshotListener { value, error ->
                    if (error != null || value == null || !value.exists()) {
                        return@addSnapshotListener
                    }

                    try {
                        // We're taking the Map received from Firestore, converting it to a JSON string using Gson, and then parsing it into a model.
                        // This prevents Firestore's direct object conversion errors, especially with Enums.
                        val dataMap = value.data
                        if (dataMap != null) {
                            val json = gson.toJson(dataMap)
                            val event = gson.fromJson(json, NSDataModel::class.java)
                            latestEventCallback?.invoke(event)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
        }
    }

    fun clearLatestEvent() {
        currentUserId?.let {
            // To clear the event, we can send an empty model or a specific "empty" event.
            firestore.collection("webrtc_events").document(it).delete()
        }
    }
}