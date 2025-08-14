package com.example.weathermapapp.network.api

import com.example.weathermapapp.data.model.ollama.OllamaRequest
import com.example.weathermapapp.data.model.ollama.OllamaResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

interface OllamaApiService {
    @POST("api/generate")
    suspend fun generateResponse(
        @Body request: OllamaRequest
    ): Response<OllamaResponse>

    @Streaming
    @POST("api/generate")
    suspend fun generateResponseStream(
        @Body request: OllamaRequest
    ): Response<ResponseBody>
}