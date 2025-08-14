package com.example.weathermapapp.data.repository

import com.example.weathermapapp.data.model.ollama.OllamaRequest
import com.example.weathermapapp.data.model.ollama.OllamaResponse
import com.example.weathermapapp.network.api.OllamaApiService
import com.example.weathermapapp.util.Resource
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class OllamaRepository @Inject constructor(
    private val ollamaApiService: OllamaApiService,
    private val gson: Gson
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

    fun getLlamaResponseStream(prompt: String, model: String): Flow<String> = flow {
        // The stream: true parameter tells OLLAMA to send the answer in a streaming manner.
        val request = OllamaRequest(model = model, prompt = prompt, stream = true)

        val response = ollamaApiService.generateResponseStream(request)

        if (response.isSuccessful && response.body() != null) {
            val reader = response.body()!!.source().inputStream().bufferedReader()
            try {
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    // Each line is a JSON object. We're parsing it and extracting the "response" field.
                    val partialResponse = gson.fromJson(line, OllamaResponse::class.java)
                    if (partialResponse.done == false) {
                        emit(partialResponse.response)  // We're emitting each incoming token to Flow.
                    }
                }
            } finally {
                reader.close()
            }
        } else {
            // In case of an error, Flow will stop. This is where you can improve error handling.
            throw Exception("API Error: ${response.code()}")
        }
    }.flowOn(Dispatchers.IO)
}