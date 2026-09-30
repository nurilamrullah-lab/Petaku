package com.geoshift.locationsimulator.engine.provider

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import com.geoshift.locationsimulator.data.model.LocationModel
import com.geoshift.locationsimulator.data.model.SimulationMode

class StandardMockLocationProvider(private val context: Context) : MockLocationProvider {
    override val mode: SimulationMode = SimulationMode.SYSTEM_MOCK_MODE

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    private var isInitialized = false

    override fun isAvailable(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager
            if (appOps != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOps.unsafeCheckOpNoThrow(
                        android.app.AppOpsManager.OPSTR_MOCK_LOCATION,
                        android.os.Process.myUid(),
                        context.packageName
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appOps.checkOpNoThrow(
                        android.app.AppOpsManager.OPSTR_MOCK_LOCATION,
                        android.os.Process.myUid(),
                        context.packageName
                    )
                }
                mode == android.app.AppOpsManager.MODE_ALLOWED
            } else {
                try {
                    locationManager.addTestProvider(
                        LocationManager.GPS_PROVIDER,
                        false, false, false, false, true, true, true,
                        Criteria.POWER_LOW, Criteria.ACCURACY_FINE
                    )
                    true
                } finally {
                    try { locationManager.removeTestProvider(LocationManager.GPS_PROVIDER) } catch (_: Exception) {}
                }
            }
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    override fun initialize(): Result<Unit> {
        return try {
            for (p in providers) {
                try {
                    locationManager.addTestProvider(
                        p,
                        false, false, false, false, true, true, true,
                        Criteria.POWER_LOW, Criteria.ACCURACY_FINE
                    )
                } catch (ignored: IllegalArgumentException) {
                    // Provider might already be added
                }
                locationManager.setTestProviderEnabled(p, true)
            }
            isInitialized = true
            Result.success(Unit)
        } catch (e: SecurityException) {
            isInitialized = false
            Result.failure(Exception("Mock location permission not granted. Configure GeoShift as Mock Location App in Developer Options."))
        } catch (e: Exception) {
            isInitialized = false
            Result.failure(e)
        }
    }

    override fun pushLocation(location: LocationModel): Result<Unit> {
        if (!isInitialized) {
            val initRes = initialize()
            if (initRes.isFailure) return initRes
        }

        return try {
            for (p in providers) {
                val loc = Location(p).apply {
                    latitude = location.latitude
                    longitude = location.longitude
                    altitude = location.altitude
                    accuracy = location.accuracy
                    speed = location.speed
                    bearing = location.bearing
                    time = System.currentTimeMillis()
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        bearingAccuracyDegrees = 1.0f
                        speedAccuracyMetersPerSecond = 0.5f
                        verticalAccuracyMeters = 1.5f
                    }
                }
                locationManager.setTestProviderLocation(p, loc)
            }
            Result.success(Unit)
        } catch (e: SecurityException) {
            Result.failure(Exception("SecurityException: GeoShift is not configured in Developer Options."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun release() {
        try {
            for (p in providers) {
                try {
                    locationManager.setTestProviderEnabled(p, false)
                    locationManager.removeTestProvider(p)
                } catch (ignored: Exception) {}
            }
        } catch (ignored: Exception) {}
        isInitialized = false
    }

    override fun getDiagnosticInfo(): String {
        return if (isAvailable()) "Standard Mock Location: READY (Developer options active)"
        else "Standard Mock Location: NOT CONFIGURED in Developer Options"
    }
}
