package com.example.weathermapapp.util

import java.util.concurrent.TimeUnit

object TimeUtils {
    fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            minutes < 1 -> "updated just now"
            minutes < 60 -> "updated $minutes minutes ago"
            hours < 24 -> "updated $hours hours ago"
            else -> "updated $days days ago"
        }
    }
}