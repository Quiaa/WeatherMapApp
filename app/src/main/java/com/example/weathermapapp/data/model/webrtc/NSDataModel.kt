package com.example.weathermapapp.data.model.webrtc

data class NSDataModel(
    val type: NSDataModelType,
    val sender: String,
    val target: String,
    val data: Any? = null
){
    fun isValid(): Boolean {
        return sender.isNotEmpty() && target.isNotEmpty()
    }
}