package com.geoshift.locationsimulator.data.repository

import android.content.Context
import android.os.Environment
import com.geoshift.locationsimulator.data.database.GeoShiftDbHelper
import com.geoshift.locationsimulator.data.model.HistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class LocationHistoryRepository(context: Context) {
    private val dbHelper = GeoShiftDbHelper(context.applicationContext)
    private val _historyFlow = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyFlow: StateFlow<List<HistoryItem>> = _historyFlow.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _historyFlow.value = dbHelper.getAllHistory()
    }

    fun addHistory(title: String, subtitle: String, lat: Double, lng: Double, mode: String, durationMinutes: Int = 1) {
        // Deduplikasi: Jika item terakhir memiliki koordinat yang hampir identik (< 0.0001 deg), abaikan
        val currentList = _historyFlow.value
        val lastItem = currentList.firstOrNull()
        if (lastItem != null) {
            val dLat = abs(lastItem.latitude - lat)
            val dLng = abs(lastItem.longitude - lng)
            if (dLat < 0.0001 && dLng < 0.0001) {
                return
            }
        }

        val item = HistoryItem(
            title = title,
            subtitle = subtitle,
            latitude = lat,
            longitude = lng,
            timestamp = System.currentTimeMillis(),
            durationMinutes = durationMinutes,
            mode = mode,
            categoryGroup = "Today"
        )
        dbHelper.insert(item)
        refresh()
    }

    fun deleteItem(id: Long) {
        dbHelper.delete(id)
        refresh()
    }

    fun clearAll() {
        dbHelper.clearAll()
        refresh()
    }

    fun toggleFavorite(id: Long, current: Boolean) {
        dbHelper.toggleFavorite(id, current)
        refresh()
    }

    fun exportToGpx(context: Context): File? {
        val list = _historyFlow.value
        if (list.isEmpty()) return null

        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadDir.exists()) downloadDir.mkdirs()

            val fileName = "geoshift_history_${System.currentTimeMillis()}.gpx"
            val file = File(downloadDir, fileName)

            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            val sb = StringBuilder()
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            sb.append("<gpx version=\"1.1\" creator=\"GeoShift\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
            sb.append("  <metadata>\n")
            sb.append("    <name>GeoShift Location History</name>\n")
            sb.append("    <time>${sdf.format(Date())}</time>\n")
            sb.append("  </metadata>\n")

            list.forEach { item ->
                sb.append("  <wpt lat=\"${item.latitude}\" lon=\"${item.longitude}\">\n")
                sb.append("    <name>${item.title.replace("&", "&amp;")}</name>\n")
                sb.append("    <desc>${item.subtitle.replace("&", "&amp;")} (${item.mode})</desc>\n")
                sb.append("    <time>${sdf.format(Date(item.timestamp))}</time>\n")
                sb.append("  </wpt>\n")
            }

            sb.append("</gpx>\n")

            val writer = FileWriter(file)
            writer.write(sb.toString())
            writer.flush()
            writer.close()

            return file
        } catch (_: Exception) {
            return null
        }
    }
}
