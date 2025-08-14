package com.example.weathermapapp.data.repository

import com.example.weathermapapp.data.model.ollama.OllamaRequest
import com.example.weathermapapp.network.api.OllamaApiService
import com.example.weathermapapp.util.Resource
import javax.inject.Inject

class OllamaRepository @Inject constructor(
    private val ollamaApiService: OllamaApiService
) {
    suspend fun getLlamaResponse(prompt: String, model: String): Resource<String> {
        return try {
            val request = OllamaRequest(model = model, prompt = prompt)
            val response = ollamaApiService.generateResponse(request)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!.response)
            } else {
                Resource.Error("API Error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred.")
        }
    }
}