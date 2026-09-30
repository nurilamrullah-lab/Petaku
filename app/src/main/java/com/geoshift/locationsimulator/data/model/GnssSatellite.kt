package com.geoshift.locationsimulator.data.model

enum class GnssConstellation(val code: String, val displayName: String, val colorHex: Long) {
    GPS("G", "GPS (USA)", 0xFF00D2FF),         // Cyan
    GLONASS("R", "GLONASS (RU)", 0xFFFF5252),  // Red
    GALILEO("E", "Galileo (EU)", 0xFF10B981),  // Emerald Green
    BEIDOU("B", "BeiDou (CN)", 0xFFFFB300),    // Amber/Gold
    QZSS("Q", "QZSS (JP)", 0xFF7C4DFF),        // Purple
    UNKNOWN("U", "Other", 0xFF859399)
}

data class GnssSatellite(
    val svid: Int,
    val constellation: GnssConstellation,
    val azimuthDegrees: Float,
    val elevationDegrees: Float,
    val cn0DbHz: Float,
    val usedInFix: Boolean,
    val hasAlmanac: Boolean = true,
    val hasEphemeris: Boolean = true
) {
    val prnLabel: String
        get() = "${constellation.code}${svid.toString().padStart(2, '0')}"
}

data class GnssFixInfo(
    val isFixed: Boolean = true,
    val fixType: String = "3D GNSS FIX",
    val satellitesInView: Int = 24,
    val satellitesInUse: Int = 16,
    val pdop: Float = 1.2f,
    val hdop: Float = 0.8f,
    val vdop: Float = 0.9f,
    val ttffSeconds: Int = 1
)
