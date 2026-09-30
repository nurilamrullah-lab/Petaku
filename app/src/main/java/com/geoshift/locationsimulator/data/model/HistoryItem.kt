package com.geoshift.locationsimulator.data.model

data class HistoryItem(
    val id: Long = 0,
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val durationMinutes: Int,
    val mode: String,
    val distanceKm: Float = 0.0f,
    val isFavorite: Boolean = false,
    val categoryGroup: String = "Today" // "Today", "Yesterday", "Older"
)
