package com.geoshift.locationsimulator.engine.provider

import android.content.Context
import android.os.Build
import com.geoshift.locationsimulator.data.model.LocationModel
import com.geoshift.locationsimulator.data.model.SimulationMode
import java.io.File

class PatchedLocationProvider(private val context: Context) : MockLocationProvider {
    override val mode: SimulationMode = SimulationMode.ROOT_MODE

    private var rootProvider: RootLocationProvider? = null

    override fun isAvailable(): Boolean {
        return isSmaliPatcherPresent()
    }

    override fun initialize(): Result<Unit> {
        if (!isSmaliPatcherPresent()) {
            return Result.failure(Exception("UNSUPPORTED: Your Android version or installed patch is not supported. Required mock location patch/framework is not detected."))
        }
        rootProvider = RootLocationProvider(context)
        return rootProvider?.initialize() ?: Result.failure(Exception("UNSUPPORTED: Root initialization failed."))
    }

    override fun pushLocation(location: LocationModel): Result<Unit> {
        return rootProvider?.pushLocation(location)
            ?: Result.failure(Exception("UNSUPPORTED: Patched location provider not initialized."))
    }

    override fun release() {
        rootProvider?.release()
        rootProvider = null
    }

    override fun getDiagnosticInfo(): String {
        return if (isSmaliPatcherPresent()) {
            "Smali Patcher / Framework: DETECTED (Hooks Active)"
        } else {
            "Smali Patcher / Framework: NOT DETECTED"
        }
    }

    companion object {
        fun isSmaliPatcherPresent(): Boolean {
            // Check known Magisk / KernelSU modules or framework signatures
            val modulePaths = arrayOf(
                "/data/adb/modules/smalipatcher",
                "/data/adb/modules/fusedlocationdisabler",
                "/data/adb/modules/mock_locations",
                "/data/adb/modules/smali_patcher",
                "/data/adb/modules_update/smalipatcher"
            )
            for (p in modulePaths) {
                if (File(p).exists()) return true
            }

            // Check if mock flag can be safely tested without being flagged as mock
            return false
        }
    }
}
