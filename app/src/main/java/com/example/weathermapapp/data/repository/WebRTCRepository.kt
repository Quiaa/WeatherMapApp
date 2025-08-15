package com.example.weathermapapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import javax.inject.Inject

class WebRTCRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    fun observeCallState(currentUserId: String, callback: (String?) -> Unit) {
        firestore.collection("webrtc").document(currentUserId)
            .addSnapshotListener { snapshot, _ ->
                val target = snapshot?.getString("target")
                callback(target)
            }
    }

    suspend fun sendSdp(targetId: String, sdp: SessionDescription) {
        val sdpData = mapOf(
            "type" to sdp.type.canonicalForm(),
            "sdp" to sdp.description
        )
        firestore.collection("webrtc").document(targetId).set(sdpData).await()
    }

    suspend fun sendIceCandidate(targetId: String, candidate: IceCandidate) {
        val candidateData = mapOf(
            "sdpMid" to candidate.sdpMid,
            "sdpMLineIndex" to candidate.sdpMLineIndex,
            "sdp" to candidate.sdp
        )
        firestore.collection("webrtc").document(targetId)
            .collection("ice_candidates").add(candidateData).await()
    }

    fun observeSdp(currentUserId: String, callback: (SessionDescription) -> Unit) {
        firestore.collection("webrtc").document(currentUserId)
            .addSnapshotListener { snapshot, _ ->
                val data = snapshot?.data
                if (data != null && data.containsKey("type") && data.containsKey("sdp")) {
                    val type = SessionDescription.Type.fromCanonicalForm(data["type"] as String)
                    val sdp = data["sdp"] as String
                    callback(SessionDescription(type, sdp))
                    // SDP'yi aldıktan sonra temizle
                    snapshot.reference.delete()
                }
            }
    }

    fun observeIceCandidates(currentUserId: String, callback: (IceCandidate) -> Unit) {
        firestore.collection("webrtc").document(currentUserId)
            .collection("ice_candidates")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val data = change.document.data
                        val candidate = IceCandidate(
                            data["sdpMid"] as String,
                            (data["sdpMLineIndex"] as Long).toInt(),
                            data["sdp"] as String
                        )
                        callback(candidate)
                        // ICE candidate'i aldıktan sonra temizle
                        change.document.reference.delete()
                    }
                }
            }
    }

    suspend fun endCall(currentUserId: String, targetId: String) {
        firestore.collection("webrtc").document(currentUserId).delete().await()
        firestore.collection("webrtc").document(targetId).delete().await()
    }
}