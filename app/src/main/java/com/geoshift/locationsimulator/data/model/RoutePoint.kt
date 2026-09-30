package com.geoshift.locationsimulator.data.model

data class RoutePoint(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val note: String = ""
)

data class RouteSimulationTrack(
    val id: String,
    val name: String,
    val originName: String,
    val destinationName: String,
    val totalDistanceKm: Float,
    val estimatedDurationMinutes: Int,
    val waypoints: List<RoutePoint>
)
