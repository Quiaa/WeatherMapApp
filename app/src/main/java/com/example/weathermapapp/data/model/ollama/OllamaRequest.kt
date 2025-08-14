package com.example.weathermapapp.data.model.ollama

data class OllamaRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false
)