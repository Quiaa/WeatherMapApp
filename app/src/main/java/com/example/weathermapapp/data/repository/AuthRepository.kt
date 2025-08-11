package com.example.weathermapapp.data.repository

import com.example.weathermapapp.util.Resource
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

// The interface defines the functions that our ViewModel will use.
interface AuthRepository {
    suspend fun registerUser(email: String, pass: String): Resource<AuthResult>
    // Add the login function to the interface
    suspend fun loginUser(email: String, pass: String): Resource<AuthResult>
    fun logoutUser() // Add logout function
}

// The implementation handles the actual logic with Firebase.
class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override suspend fun registerUser(email: String, pass: String): Resource<AuthResult> {
        return try {
            // Attempt to create a user with email and password in Firebase.
            val result = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
            Resource.Success(result)
        } catch (e: Exception) {
            // If an exception occurs, return an Error resource with the exception message.
            Resource.Error(e.message ?: "An unknown error occurred.")
        }
    }

    // Implement the login function
    override suspend fun loginUser(email: String, pass: String): Resource<AuthResult> {
        return try {
            // Attempt to sign in a user with email and password in Firebase.
            val result = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
            Resource.Success(result)
        } catch (e: Exception) {
            // If an exception occurs, return an Error resource with the exception message.
            Resource.Error(e.message ?: "An unknown error occurred.")
        }
    }

    override fun logoutUser() {
        firebaseAuth.signOut()
    }
}