package com.example.weathermapapp.data.repository

import com.example.weathermapapp.util.Resource
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface AuthRepository {
    fun logoutUser()
    fun getCurrentUserId(): String?
    suspend fun registerUser(email: String, pass: String, name: String): Resource<AuthResult>
    suspend fun loginUser(email: String, pass: String): Resource<AuthResult>
}

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    override suspend fun registerUser(email: String, pass: String, name: String): Resource<AuthResult> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
            result.user?.uid?.let { uid ->
                val userProfile = mapOf("name" to name, "email" to email)
                firestore.collection("users").document(uid).set(userProfile).await()
            }
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

    override fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }
}