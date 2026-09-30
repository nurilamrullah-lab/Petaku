package com.geoshift.locationsimulator.engine.provider

import android.content.Context
import com.geoshift.locationsimulator.data.model.LocationModel
import com.geoshift.locationsimulator.data.model.SimulationMode
import java.io.File

class RootLocationProvider(private val context: Context) : MockLocationProvider {
    override val mode: SimulationMode = SimulationMode.ROOT_MODE

    private var rootAvailable: Boolean = false
    private var standardFallback: StandardMockLocationProvider? = null

    init {
        rootAvailable = checkSuBinary()
    }

    override fun isAvailable(): Boolean {
        return checkSuBinary()
    }

    override fun initialize(): Result<Unit> {
        if (!checkSuBinary()) {
            return Result.failure(Exception("UNSUPPORTED: Root access (su binary) is not detected on this device."))
        }

        // Validate su execution without intrusive system modification
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                // Initialize standard provider under root privileges
                standardFallback = StandardMockLocationProvider(context)
                standardFallback?.initialize() ?: Result.success(Unit)
            } else {
                Result.failure(Exception("UNSUPPORTED: Root request denied by superuser manager."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("UNSUPPORTED: Unable to invoke su binary safely: ${e.message}"))
        }
    }

    override fun pushLocation(location: LocationModel): Result<Unit> {
        return standardFallback?.pushLocation(location)
            ?: Result.failure(Exception("UNSUPPORTED: Root mock provider is not initialized."))
    }

    override fun release() {
        standardFallback?.release()
        standardFallback = null
    }

    override fun getDiagnosticInfo(): String {
        return if (checkSuBinary()) "Root: YES (su present) | Integration: READY"
        else "Root: NOT DETECTED"
    }

    companion object {
        fun checkSuBinary(): Boolean {
            val paths = arrayOf(
                "/system/bin/su",
                "/system/xbin/su",
                "/sbin/su",
                "/system/su",
                "/system/bin/.ext/.su",
                "/system/usr/we-need-root/su-backup",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su"
            )
            for (path in paths) {
                if (File(path).exists()) return true
            }
            // Check PATH environment variable safely
            val envPath = System.getenv("PATH") ?: return false
            for (dir in envPath.split(":")) {
                if (File(dir, "su").exists()) return true
            }
            return false
        }
    }
}
