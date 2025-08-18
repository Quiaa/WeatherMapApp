package com.example.weathermapapp.data.repository

import com.example.weathermapapp.data.model.UserLocation
import com.example.weathermapapp.util.Resource
import kotlinx.coroutines.flow.Flow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// Interface for UserRepository
interface UserRepository {
    suspend fun saveUserLocation(location: UserLocation): Resource<Unit>
    fun getUserLocation(): Flow<Resource<UserLocation?>>
    suspend fun getAllUsersLocations(): Resource<List<UserLocation>>
    suspend fun saveRealtimeUserLocation(location: UserLocation): Resource<Unit>
    fun getRealtimeAllUsersLocations(): Flow<Resource<List<UserLocation>>>
    suspend fun getUserName(uid: String): String
}

// Implementation of UserRepository
class UserRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : UserRepository {

    override suspend fun getUserName(uid: String): String {
        return try {
            firestore.collection("users").document(uid).get().await().getString("name") ?: "Unknown User"
        } catch (e: Exception) {
            "Unknown User"
        }
    }

    override suspend fun saveUserLocation(location: UserLocation): Resource<Unit> {
        val uid = firebaseAuth.currentUser?.uid ?: return Resource.Error("User not logged in.")
        return try {
            val name = getUserName(uid)
            val locationToSave = location.copy(userId = uid, userName = name)
            firestore.collection("selected_locations").document(uid).set(locationToSave).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to save location.")
        }
    }

    override fun getUserLocation(): Flow<Resource<UserLocation?>> {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            return kotlinx.coroutines.flow.flowOf(Resource.Error("User not logged in."))
        }

        return firestore.collection("selected_locations").document(uid)
            .snapshots()
            .map { documentSnapshot ->
                try {
                    val location = documentSnapshot.toObject(UserLocation::class.java)
                    Resource.Success(location)
                } catch (e: Exception) {
                    Resource.Error(e.message ?: "Failed to parse location.")
                }
            }
    }

    override suspend fun getAllUsersLocations(): Resource<List<UserLocation>> {
        return Resource.Success(emptyList())
    }

    override suspend fun saveRealtimeUserLocation(location: UserLocation): Resource<Unit> {
        val uid = firebaseAuth.currentUser?.uid ?: return Resource.Error("User not logged in.")
        return try {
            val name = getUserName(uid)
            val locationToSave = location.copy(userId = uid, userName = name)
            firestore.collection("realtime_locations").document(uid).set(locationToSave).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to save real-time location.")
        }
    }

    override fun getRealtimeAllUsersLocations(): Flow<Resource<List<UserLocation>>> {
        return firestore.collection("realtime_locations").snapshots().map { querySnapshot ->
            try {
                val locations = querySnapshot.documents.mapNotNull { it.toObject<UserLocation>() }
                Resource.Success(locations)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Failed to parse real-time locations.")
            }
        }
    }
}