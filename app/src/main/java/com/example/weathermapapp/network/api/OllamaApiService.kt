package com.example.weathermapapp.network.api

import com.example.weathermapapp.data.model.ollama.OllamaRequest
import com.example.weathermapapp.data.model.ollama.OllamaResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface OllamaApiService {
    @POST("api/generate")
    suspend fun generateResponse(
        @Body request: OllamaRequest
    ): Response<OllamaResponse>
}