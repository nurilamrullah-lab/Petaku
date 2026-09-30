package com.geoshift.locationsimulator.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.geoshift.locationsimulator.data.model.HistoryItem

class GeoShiftDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val createHistoryTable = """
            CREATE TABLE $TABLE_HISTORY (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_SUBTITLE TEXT NOT NULL,
                $COLUMN_LATITUDE REAL NOT NULL,
                $COLUMN_LONGITUDE REAL NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_DURATION INTEGER NOT NULL,
                $COLUMN_MODE TEXT NOT NULL,
                $COLUMN_DISTANCE REAL NOT NULL,
                $COLUMN_IS_FAVORITE INTEGER NOT NULL DEFAULT 0,
                $COLUMN_CATEGORY TEXT NOT NULL
            )
        """.trimIndent()
        db.execSQL(createHistoryTable)

        // Seed with design telemetry samples
        seedInitialHistory(db)
    }

    private fun seedInitialHistory(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        val oneHour = 3600000L
        val oneDay = 86400000L

        insertSeed(db, "Jakarta, Indonesia", "Thamrin Financial CBD, DKI", -6.2088, 106.8456, now - (30 * 60000L), 45, "Static Spoof", 0.0f, true, "Today")
        insertSeed(db, "Surabaya, Indonesia", "Gubeng Station Loop", -7.2625, 112.7383, now - oneDay - (4 * oneHour), 72, "Route GPX", 18.4f, false, "Yesterday")
        insertSeed(db, "Jember, Indonesia", "University Area (UNEJ Campus)", -8.1724, 113.7007, now - (2 * oneDay) - (6 * oneHour), 125, "Static Spoof", 0.0f, true, "Older")
        insertSeed(db, "Bali, Indonesia", "Kuta Beach Shoreline, Badung", -8.7185, 115.1686, now - (5 * oneDay), 30, "Joystick Mode", 5.2f, false, "Older")
    }

    private fun insertSeed(
        db: SQLiteDatabase,
        title: String,
        subtitle: String,
        lat: Double,
        lng: Double,
        timestamp: Long,
        duration: Int,
        mode: String,
        dist: Float,
        fav: Boolean,
        cat: String
    ) {
        val cv = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_SUBTITLE, subtitle)
            put(COLUMN_LATITUDE, lat)
            put(COLUMN_LONGITUDE, lng)
            put(COLUMN_TIMESTAMP, timestamp)
            put(COLUMN_DURATION, duration)
            put(COLUMN_MODE, mode)
            put(COLUMN_DISTANCE, dist)
            put(COLUMN_IS_FAVORITE, if (fav) 1 else 0)
            put(COLUMN_CATEGORY, cat)
        }
        db.insert(TABLE_HISTORY, null, cv)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        onCreate(db)
    }

    fun getAllHistory(): List<HistoryItem> {
        val list = mutableListOf<HistoryItem>()
        val db = readableDatabase
        val cursor = db.query(TABLE_HISTORY, null, null, null, null, null, "$COLUMN_TIMESTAMP DESC")
        cursor.use {
            while (it.moveToNext()) {
                val item = HistoryItem(
                    id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                    title = it.getString(it.getColumnIndexOrThrow(COLUMN_TITLE)),
                    subtitle = it.getString(it.getColumnIndexOrThrow(COLUMN_SUBTITLE)),
                    latitude = it.getDouble(it.getColumnIndexOrThrow(COLUMN_LATITUDE)),
                    longitude = it.getDouble(it.getColumnIndexOrThrow(COLUMN_LONGITUDE)),
                    timestamp = it.getLong(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP)),
                    durationMinutes = it.getInt(it.getColumnIndexOrThrow(COLUMN_DURATION)),
                    mode = it.getString(it.getColumnIndexOrThrow(COLUMN_MODE)),
                    distanceKm = it.getFloat(it.getColumnIndexOrThrow(COLUMN_DISTANCE)),
                    isFavorite = it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_FAVORITE)) == 1,
                    categoryGroup = it.getString(it.getColumnIndexOrThrow(COLUMN_CATEGORY))
                )
                list.add(item)
            }
        }
        return list
    }

    fun insert(item: HistoryItem): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COLUMN_TITLE, item.title)
            put(COLUMN_SUBTITLE, item.subtitle)
            put(COLUMN_LATITUDE, item.latitude)
            put(COLUMN_LONGITUDE, item.longitude)
            put(COLUMN_TIMESTAMP, item.timestamp)
            put(COLUMN_DURATION, item.durationMinutes)
            put(COLUMN_MODE, item.mode)
            put(COLUMN_DISTANCE, item.distanceKm)
            put(COLUMN_IS_FAVORITE, if (item.isFavorite) 1 else 0)
            put(COLUMN_CATEGORY, item.categoryGroup)
        }
        return db.insert(TABLE_HISTORY, null, cv)
    }

    fun delete(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_HISTORY, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun clearAll(): Int {
        val db = writableDatabase
        return db.delete(TABLE_HISTORY, null, null)
    }

    fun toggleFavorite(id: Long, current: Boolean): Int {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COLUMN_IS_FAVORITE, if (current) 0 else 1)
        }
        return db.update(TABLE_HISTORY, cv, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    companion object {
        const val DATABASE_NAME = "geoshift.db"
        const val DATABASE_VERSION = 1

        const val TABLE_HISTORY = "history"
        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_SUBTITLE = "subtitle"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_DURATION = "duration"
        const val COLUMN_MODE = "mode"
        const val COLUMN_DISTANCE = "distance"
        const val COLUMN_IS_FAVORITE = "is_favorite"
        const val COLUMN_CATEGORY = "category"
    }
}
