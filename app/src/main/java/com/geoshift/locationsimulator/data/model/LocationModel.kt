package com.geoshift.locationsimulator.data.model

data class LocationModel(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 14.0,
    val accuracy: Float = 1.2f,
    val speed: Float = 0.0f,
    val bearing: Float = 0.0f,
    val timestamp: Long = System.currentTimeMillis(),
    val providerName: String = "gps",
    val locationName: String = "Selected Coordinate",
    val address: String = "Indonesia"
) {
    fun toFormattedCoordinates(): String {
        return String.format("%.4f, %.4f", latitude, longitude)
    }
}
