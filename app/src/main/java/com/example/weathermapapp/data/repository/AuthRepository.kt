package com.example.weathermapapp.data.repository

import com.example.weathermapapp.BuildConfig
import com.example.weathermapapp.data.model.UserLocation
import com.example.weathermapapp.data.model.WeatherResponse
import com.example.weathermapapp.network.service.RetrofitInstance
import com.example.weathermapapp.util.Resource
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface AuthRepository {
    suspend fun registerUser(email: String, pass: String): Resource<AuthResult>
    suspend fun loginUser(email: String, pass: String): Resource<AuthResult>
    fun logoutUser()
    suspend fun saveUserLocation(location: UserLocation): Resource<Unit>
    suspend fun getUserLocation(): Resource<UserLocation?>
    suspend fun getWeatherData(lat: Double, lon: Double): Resource<WeatherResponse>
    suspend fun getAllUsersLocations(): Resource<List<UserLocation>>
}

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    override suspend fun registerUser(email: String, pass: String): Resource<AuthResult> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
            Resource.Success(result)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred.")
        }
    }

    override suspend fun loginUser(email: String, pass: String): Resource<AuthResult> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
            Resource.Success(result)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred.")
        }
    }

    override fun logoutUser() {
        firebaseAuth.signOut()
    }

    override suspend fun saveUserLocation(location: UserLocation): Resource<Unit> {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            return Resource.Error("User not logged in.")
        }

        return try {
            firestore.collection("users").document(uid).set(location).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to save location.")
        }
    }

    override suspend fun getUserLocation(): Resource<UserLocation?> {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            return Resource.Error("User not logged in.")
        }

        return try {
            val document = firestore.collection("users").document(uid).get().await()
            val location = document.toObject(UserLocation::class.java)
            Resource.Success(location)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to retrieve location.")
        }
    }

    // Implementation for the new weather function
    override suspend fun getWeatherData(lat: Double, lon: Double): Resource<WeatherResponse> {
        return try {
            // Call the API through our RetrofitInstance
            val response = RetrofitInstance.api.getCurrentWeather(
                latitude = lat,
                longitude = lon,
                apiKey = BuildConfig.OPENWEATHER_API_KEY // Use the key from BuildConfig
            )
            if (response.isSuccessful) {
                // If the response is successful, return the data
                response.body()?.let {
                    return Resource.Success(it)
                } ?: Resource.Error("Response body is empty.")
            } else {
                // If the server returned an error (e.g., 404, 500)
                Resource.Error("API Error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            // If there's a network error or other exception
            Resource.Error(e.message ?: "An unknown network error occurred.")
        }
    }
    override suspend fun getAllUsersLocations(): Resource<List<UserLocation>> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
            val documents = firestore.collection("users").get().await()
            val locations = documents.documents.mapNotNull { document ->
                // We don't want to show the current user's location in this list again
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