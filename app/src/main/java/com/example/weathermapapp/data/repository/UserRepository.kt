package com.example.weathermapapp.data.repository

import com.example.weathermapapp.data.model.UserLocation
import com.example.weathermapapp.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Interface for UserRepository
interface UserRepository {
    suspend fun saveUserLocation(location: UserLocation): Resource<Unit>
    suspend fun getUserLocation(): Resource<UserLocation?>
    suspend fun getAllUsersLocations(): Resource<List<UserLocation>>
}

// Implementation of UserRepository
class UserRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    override suspend fun saveUserLocation(location: UserLocation): Resource<Unit> {
        val uid = firebaseAuth.currentUser?.uid
            ?: return Resource.Error("User not logged in.")

        return try {
            firestore.collection("users").document(uid).set(location).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to save location.")
        }
    }

    override suspend fun getUserLocation(): Resource<UserLocation?> {
        val uid = firebaseAuth.currentUser?.uid
            ?: return Resource.Error("User not logged in.")

        return try {
            val document = firestore.collection("users").document(uid).get().await()
            val location = document.toObject(UserLocation::class.java)
            Resource.Success(location)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to retrieve location.")
        }
    }

    override suspend fun getAllUsersLocations(): Resource<List<UserLocation>> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
            val documents = firestore.collection("users").get().await()
            val locations = documents.documents.mapNotNull { document ->
                if (document.id != currentUserId) {
                    document.toObject(UserLocation::class.java)
                } else {
                    null
                }
            }
            Resource.Success(locations)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to retrieve all user locations.")
        }
    }
}