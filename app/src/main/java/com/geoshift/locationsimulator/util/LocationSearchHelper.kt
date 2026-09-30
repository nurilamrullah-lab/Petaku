package com.geoshift.locationsimulator.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.regex.Pattern

data class SearchResultItem(
    val title: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val category: String = "Lokasi"
)

object LocationSearchHelper {

    private val COORD_PATTERN = Pattern.compile("(-?\\d{1,2}(?:\\.\\d+)?)[,\\s]+(-?\\d{1,3}(?:\\.\\d+)?)")

    fun parseDirectCoordinates(query: String): SearchResultItem? {
        val trimmed = query.trim()
        val matcher = COORD_PATTERN.matcher(trimmed)
        if (matcher.find()) {
            val lat = matcher.group(1)?.toDoubleOrNull() ?: return null
            val lng = matcher.group(2)?.toDoubleOrNull() ?: return null
            if (lat in -90.0..90.0 && lng in -180.0..180.0) {
                return SearchResultItem(
                    title = "Koordinat: ${"%.5f".format(lat)}, ${"%.5f".format(lng)}",
                    address = "Input Manual / Clipboard Koordinat",
                    latitude = lat,
                    longitude = lng,
                    category = "Koordinat"
                )
            }
        }
        return null
    }

    suspend fun searchLocation(context: Context, query: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResultItem>()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        // 1. Cek apakah input berupa koordinat langsung
        val directCoord = parseDirectCoordinates(trimmed)
        if (directCoord != null) {
            results.add(directCoord)
        }

        // 2. Coba Geocoder bawaan Android terlebih dahulu
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale("id", "ID"))
                val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    var syncResult: List<Address>? = null
                    geocoder.getFromLocationName(trimmed, 8) { list -> syncResult = list }
                    // Tunggu sesaat jika asynchronous
                    var count = 0
                    while (syncResult == null && count < 15) {
                        Thread.sleep(50)
                        count++
                    }
                    syncResult
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(trimmed, 8)
                }

                addresses?.forEach { addr ->
                    val title = addr.featureName ?: addr.thoroughfare ?: addr.subLocality ?: addr.locality ?: "Lokasi Ditemukan"
                    val fullAddress = (0..addr.maxAddressLineIndex).mapNotNull { addr.getAddressLine(it) }.joinToString(", ")
                    results.add(
                        SearchResultItem(
                            title = title,
                            address = if (fullAddress.isNotEmpty()) fullAddress else "${addr.locality ?: ""}, ${addr.adminArea ?: ""}",
                            latitude = addr.latitude,
                            longitude = addr.longitude,
                            category = addr.featureName?.let { "Alamat" } ?: "Tempat"
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // 3. Fallback ke OpenStreetMap Nominatim jika Geocoder lokal tidak menghasilkan hasil
        if (results.isEmpty() || (results.size == 1 && directCoord != null)) {
            try {
                val encoded = URLEncoder.encode(trimmed, "UTF-8")
                val urlStr = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&addressdetails=1&limit=10"
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("User-Agent", "GeoShift/1.0 (Android; Mobile)")

                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val jsonText = reader.readText()
                    reader.close()

                    val jsonArray = JSONArray(jsonText)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val lat = obj.optDouble("lat", 0.0)
                        val lon = obj.optDouble("lon", 0.0)
                        val displayName = obj.optString("display_name", "")
                        val type = obj.optString("type", "Lokasi")
                        val parts = displayName.split(",")
                        val title = parts.firstOrNull()?.trim() ?: displayName
                        val remaining = parts.drop(1).joinToString(",").trim()

                        results.add(
                            SearchResultItem(
                                title = title,
                                address = if (remaining.isNotEmpty()) remaining else displayName,
                                latitude = lat,
                                longitude = lon,
                                category = type.replaceFirstChar { it.uppercase() }
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }

        // Deduplikasi hasil berdasarkan koordinat
        results.distinctBy { "%.4f,%.4f".format(it.latitude, it.longitude) }
    }
}
