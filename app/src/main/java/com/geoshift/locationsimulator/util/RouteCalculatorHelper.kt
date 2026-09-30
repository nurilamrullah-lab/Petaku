package com.geoshift.locationsimulator.util

import com.geoshift.locationsimulator.data.model.RoutePoint
import com.geoshift.locationsimulator.data.model.RouteSimulationTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object RouteCalculatorHelper {

    suspend fun calculateRoadRoute(
        originName: String,
        originLat: Double,
        originLng: Double,
        destName: String,
        destLat: Double,
        destLng: Double
    ): RouteSimulationTrack = withContext(Dispatchers.IO) {
        // 1. Coba kalkulasi rute jalan raya via OSRM Driving API
        try {
            val urlStr = "https://router.project-osrm.org/route/v1/driving/$originLng,$originLat;$destLng,$destLat?overview=full&geometries=geojson"
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 7000
            conn.readTimeout = 7000
            conn.setRequestProperty("User-Agent", "GeoShift/1.0 (Android; RouteSimulator)")

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val jsonText = reader.readText()
                reader.close()

                val root = JSONObject(jsonText)
                if (root.optString("code") == "Ok") {
                    val routes = root.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val firstRoute = routes.getJSONObject(0)
                        val totalDistanceMeters = firstRoute.optDouble("distance", 0.0)
                        val durationSeconds = firstRoute.optDouble("duration", 0.0)
                        val geometry = firstRoute.getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")

                        val waypoints = mutableListOf<RoutePoint>()
                        for (i in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(i)
                            val lng = coord.getDouble(0)
                            val lat = coord.getDouble(1)
                            val stepName = when (i) {
                                0 -> "Awal: $originName"
                                coordinates.length() - 1 -> "Tujuan: $destName"
                                else -> "Titik Jalan #$i"
                            }
                            waypoints.add(RoutePoint(stepName, lat, lng, "Jalur OSRM"))
                        }

                        if (waypoints.isNotEmpty()) {
                            return@withContext RouteSimulationTrack(
                                id = UUID.randomUUID().toString(),
                                name = "$originName -> $destName",
                                originName = originName,
                                destinationName = destName,
                                totalDistanceKm = (totalDistanceMeters / 1000.0).toFloat(),
                                estimatedDurationMinutes = (durationSeconds / 60.0).roundToInt().coerceAtLeast(1),
                                waypoints = waypoints
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback jika offline: Buat jalur terpolasi garis lurus bertahap
        val directDistKm = haversineKm(originLat, originLng, destLat, destLng)
        val steps = (directDistKm / 0.5f).toInt().coerceIn(5, 50)
        val fallbackWaypoints = mutableListOf<RoutePoint>()

        for (i in 0..steps) {
            val fraction = i.toDouble() / steps.toDouble()
            val lat = originLat + (destLat - originLat) * fraction
            val lng = originLng + (destLng - originLng) * fraction
            val name = when (i) {
                0 -> "Awal: $originName"
                steps -> "Tujuan: $destName"
                else -> "Jalur #$i"
            }
            fallbackWaypoints.add(RoutePoint(name, lat, lng, "Jalur Terpolasi"))
        }

        RouteSimulationTrack(
            id = UUID.randomUUID().toString(),
            name = "$originName -> $destName (Offline Direct)",
            originName = originName,
            destinationName = destName,
            totalDistanceKm = directDistKm.toFloat(),
            estimatedDurationMinutes = (directDistKm / 40.0 * 60.0).roundToInt().coerceAtLeast(1),
            waypoints = fallbackWaypoints
        )
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
