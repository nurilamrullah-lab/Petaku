package com.geoshift.locationsimulator.data.model

data class DeviceDiagnostic(
    val deviceModel: String,
    val androidVersion: String,
    val sdkInt: Int,
    val architecture: String,
    val isRooted: Boolean,
    val isMockAppConfigured: Boolean,
    val isSmaliPatcherDetected: Boolean,
    val activeMode: SimulationMode,
    val statusMessage: String
)
