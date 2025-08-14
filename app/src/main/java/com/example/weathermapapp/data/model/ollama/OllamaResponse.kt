package com.example.weathermapapp.data.model.ollama

data class OllamaResponse(
    val model: String,
    val response: String,
    val done: Boolean
)