package com.geoshift.locationsimulator.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("geoshift_settings", Context.MODE_PRIVATE)

    private val _speedMultiplier = MutableStateFlow(prefs.getFloat(KEY_SPEED_MULTIPLIER, 1.0f))
    val speedMultiplier: StateFlow<Float> = _speedMultiplier.asStateFlow()

    private val _mapStyle = MutableStateFlow(prefs.getString(KEY_MAP_STYLE, "Google Roadmap") ?: "Google Roadmap")
    val mapStyle: StateFlow<String> = _mapStyle.asStateFlow()

    private val _jitterEnabled = MutableStateFlow(prefs.getBoolean(KEY_JITTER_ENABLED, true))
    val jitterEnabled: StateFlow<Boolean> = _jitterEnabled.asStateFlow()

    private val _updateIntervalMs = MutableStateFlow(prefs.getLong(KEY_UPDATE_INTERVAL, 500L))
    val updateIntervalMs: StateFlow<Long> = _updateIntervalMs.asStateFlow()

    private val _backgroundPersistence = MutableStateFlow(prefs.getBoolean(KEY_PERSISTENCE, true))
    val backgroundPersistence: StateFlow<Boolean> = _backgroundPersistence.asStateFlow()

    private val _antiDetectionHeuristics = MutableStateFlow(prefs.getBoolean(KEY_ANTI_DETECTION, true))
    val antiDetectionHeuristics: StateFlow<Boolean> = _antiDetectionHeuristics.asStateFlow()

    private val _mapsApiKey = MutableStateFlow(prefs.getString(KEY_MAPS_API_KEY, "") ?: "")
    val mapsApiKey: StateFlow<String> = _mapsApiKey.asStateFlow()

    // Preferences Baru
    private val _uiTheme = MutableStateFlow(prefs.getString(KEY_UI_THEME, "Dark Navy (AMOLED)") ?: "Dark Navy (AMOLED)")
    val uiTheme: StateFlow<String> = _uiTheme.asStateFlow()

    private val _defaultAnchorName = MutableStateFlow(prefs.getString(KEY_DEFAULT_ANCHOR_NAME, "Jember Kidul (Ruko Gajah Mada)") ?: "Jember Kidul (Ruko Gajah Mada)")
    val defaultAnchorName: StateFlow<String> = _defaultAnchorName.asStateFlow()

    private val _defaultAnchorLat = MutableStateFlow(prefs.getFloat(KEY_DEFAULT_ANCHOR_LAT, -8.1724f).toDouble())
    val defaultAnchorLat: StateFlow<Double> = _defaultAnchorLat.asStateFlow()

    private val _defaultAnchorLng = MutableStateFlow(prefs.getFloat(KEY_DEFAULT_ANCHOR_LNG, 113.7007f).toDouble())
    val defaultAnchorLng: StateFlow<Double> = _defaultAnchorLng.asStateFlow()

    private val _telemetryUnits = MutableStateFlow(prefs.getString(KEY_TELEMETRY_UNITS, "Metric (km/jam, m)") ?: "Metric (km/jam, m)")
    val telemetryUnits: StateFlow<String> = _telemetryUnits.asStateFlow()

    private val _autoStartBoot = MutableStateFlow(prefs.getBoolean(KEY_AUTO_START_BOOT, false))
    val autoStartBoot: StateFlow<Boolean> = _autoStartBoot.asStateFlow()

    private val _notificationEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true))
    val notificationEnabled: StateFlow<Boolean> = _notificationEnabled.asStateFlow()

    fun setSpeedMultiplier(multiplier: Float) {
        prefs.edit().putFloat(KEY_SPEED_MULTIPLIER, multiplier).apply()
        _speedMultiplier.value = multiplier
    }

    fun setMapStyle(style: String) {
        prefs.edit().putString(KEY_MAP_STYLE, style).apply()
        _mapStyle.value = style
    }

    fun setJitterEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_JITTER_ENABLED, enabled).apply()
        _jitterEnabled.value = enabled
    }

    fun setUpdateInterval(intervalMs: Long) {
        prefs.edit().putLong(KEY_UPDATE_INTERVAL, intervalMs).apply()
        _updateIntervalMs.value = intervalMs
    }

    fun setBackgroundPersistence(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PERSISTENCE, enabled).apply()
        _backgroundPersistence.value = enabled
    }

    fun setAntiDetectionHeuristics(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ANTI_DETECTION, enabled).apply()
        _antiDetectionHeuristics.value = enabled
    }

    fun setMapsApiKey(key: String) {
        prefs.edit().putString(KEY_MAPS_API_KEY, key).apply()
        _mapsApiKey.value = key
    }

    fun setUiTheme(theme: String) {
        prefs.edit().putString(KEY_UI_THEME, theme).apply()
        _uiTheme.value = theme
    }

    fun setDefaultAnchor(name: String, lat: Double, lng: Double) {
        prefs.edit()
            .putString(KEY_DEFAULT_ANCHOR_NAME, name)
            .putFloat(KEY_DEFAULT_ANCHOR_LAT, lat.toFloat())
            .putFloat(KEY_DEFAULT_ANCHOR_LNG, lng.toFloat())
            .apply()
        _defaultAnchorName.value = name
        _defaultAnchorLat.value = lat
        _defaultAnchorLng.value = lng
    }

    fun setTelemetryUnits(units: String) {
        prefs.edit().putString(KEY_TELEMETRY_UNITS, units).apply()
        _telemetryUnits.value = units
    }

    fun setAutoStartBoot(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_START_BOOT, enabled).apply()
        _autoStartBoot.value = enabled
    }

    fun setNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply()
        _notificationEnabled.value = enabled
    }

    companion object {
        private const val KEY_SPEED_MULTIPLIER = "speed_multiplier"
        private const val KEY_MAP_STYLE = "map_style"
        private const val KEY_JITTER_ENABLED = "jitter_enabled"
        private const val KEY_UPDATE_INTERVAL = "update_interval"
        private const val KEY_PERSISTENCE = "persistence"
        private const val KEY_ANTI_DETECTION = "anti_detection"
        private const val KEY_MAPS_API_KEY = "maps_api_key"

        private const val KEY_UI_THEME = "ui_theme"
        private const val KEY_DEFAULT_ANCHOR_NAME = "default_anchor_name"
        private const val KEY_DEFAULT_ANCHOR_LAT = "default_anchor_lat"
        private const val KEY_DEFAULT_ANCHOR_LNG = "default_anchor_lng"
        private const val KEY_TELEMETRY_UNITS = "telemetry_units"
        private const val KEY_AUTO_START_BOOT = "auto_start_boot"
        private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
    }
}
