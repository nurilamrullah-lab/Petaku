package com.geoshift.locationsimulator.engine

import android.content.Context
import android.os.Build
import com.geoshift.locationsimulator.data.model.DeviceDiagnostic
import com.geoshift.locationsimulator.data.model.SimulationMode
import com.geoshift.locationsimulator.engine.provider.PatchedLocationProvider
import com.geoshift.locationsimulator.engine.provider.RootLocationProvider
import com.geoshift.locationsimulator.engine.provider.StandardMockLocationProvider

object EnvironmentDetector {

    fun detect(context: Context): DeviceDiagnostic {
        val isRoot = RootLocationProvider.checkSuBinary()
        val isPatch = PatchedLocationProvider.isSmaliPatcherPresent()
        val standardProvider = StandardMockLocationProvider(context)
        val isMockConfigured = standardProvider.isAvailable()

        val activeMode = when {
            isRoot && isPatch -> SimulationMode.ROOT_MODE
            isRoot -> SimulationMode.ROOT_MODE
            isMockConfigured -> SimulationMode.SYSTEM_MOCK_MODE
            else -> SimulationMode.APP_SIMULATION_MODE
        }

        val message = when {
            isRoot && isPatch -> "Root Mode: Framework & mock integration ready"
            isRoot -> "Root access detected, but the required location framework/patch is not available."
            isMockConfigured -> "System Mock Location configured in Developer Options"
            else -> "Developer Options not configured. App Simulation active."
        }

        return DeviceDiagnostic(
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            sdkInt = Build.VERSION.SDK_INT,
            architecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            isRooted = isRoot,
            isMockAppConfigured = isMockConfigured,
            isSmaliPatcherDetected = isPatch,
            activeMode = activeMode,
            statusMessage = message
        )
    }
}
