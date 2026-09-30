package com.geoshift.locationsimulator.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.function.Consumer

object DeviceLocationHelper {

    private const val TAG = "GeoShiftLoc"

    private fun isRealLocation(loc: Location): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            !loc.isMock
        } else {
            @Suppress("DEPRECATION")
            !loc.isFromMockProvider
        }
    }

    @SuppressLint("MissingPermission")
    fun getRealDeviceLocation(context: Context, onResult: (Location?) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            onResult(null)
            return
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm == null) {
            onResult(null)
            return
        }

        // Prioritas provider untuk lokasi riil non-mock:
        // 1. FUSED_PROVIDER (Google Play Services / Framework Fused - tercepat & paling akurat)
        // 2. GPS_PROVIDER (Sensor satelit GNSS fisik)
        // 3. NETWORK_PROVIDER (Cell tower / WiFi)
        // 4. PASSIVE_PROVIDER (Cache lokasi dari aplikasi lain seperti Google Maps)
        val providerCandidates = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            providerCandidates.add(LocationManager.FUSED_PROVIDER)
        }
        providerCandidates.add(LocationManager.GPS_PROVIDER)
        providerCandidates.add(LocationManager.NETWORK_PROVIDER)
        providerCandidates.add(LocationManager.PASSIVE_PROVIDER)

        var bestReal: Location? = null

        // 1. Periksa cache lokasi terakhir yang BENAR-BENAR ASLI (bukan mock)
        for (p in providerCandidates) {
            val loc = runCatching { lm.getLastKnownLocation(p) }.getOrNull()
            if (loc != null && isRealLocation(loc)) {
                if (bestReal == null || loc.time > bestReal.time || (bestReal.hasAccuracy() && loc.accuracy < bestReal.accuracy)) {
                    bestReal = loc
                }
            }
        }

        // Jika ada lokasi asli di cache, langsung panggil callback SEKETIKA (0ms delay seperti Google Maps!)
        if (bestReal != null) {
            Log.d(TAG, "Ditemukan real location dari cache: ${bestReal.latitude}, ${bestReal.longitude} (provider: ${bestReal.provider})")
            Handler(Looper.getMainLooper()).post { onResult(bestReal) }
        }

        // 2. Tetap jalankan request lokasi hardware segar untuk memperbarui akurasi jika diizinkan
        var updateDispatched = (bestReal != null)

        // Android 30+ (R, S, T, U, V, 16): Gunakan getCurrentLocation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val freshProvider = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && lm.isProviderEnabled(LocationManager.FUSED_PROVIDER) -> LocationManager.FUSED_PROVIDER
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> null
            }

            if (freshProvider != null) {
                try {
                    lm.getCurrentLocation(
                        freshProvider,
                        CancellationSignal(),
                        context.mainExecutor,
                        Consumer { freshLoc ->
                            if (freshLoc != null && isRealLocation(freshLoc)) {
                                onResult(freshLoc)
                                updateDispatched = true
                            } else if (!updateDispatched && bestReal != null) {
                                onResult(bestReal)
                            }
                        }
                    )
                    return
                } catch (e: Exception) {
                    Log.w(TAG, "getCurrentLocation error: ${e.message}")
                }
            }
        }

        // 3. Listener fallback untuk requestSingleUpdate
        try {
            val listener = object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    try { lm.removeUpdates(this) } catch (_: Exception) {}
                    if (isRealLocation(loc)) {
                        Handler(Looper.getMainLooper()).post { onResult(loc) }
                        updateDispatched = true
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            val pToRequest = if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                LocationManager.GPS_PROVIDER
            } else {
                LocationManager.NETWORK_PROVIDER
            }

            lm.requestSingleUpdate(pToRequest, listener, Looper.getMainLooper())

            // Safety timeout: jika dalam 3 detik belum ada fresh update dan belum pernah kirim hasil, kembalikan bestReal atau coba lagi
            Handler(Looper.getMainLooper()).postDelayed({
                try { lm.removeUpdates(listener) } catch (_: Exception) {}
                if (!updateDispatched) {
                    // Coba sekali lagi ambil any provider
                    val fallback = runCatching { lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) }.getOrNull()
                        ?: runCatching { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
                    onResult(fallback ?: bestReal)
                }
            }, 3000)
        } catch (e: Exception) {
            if (!updateDispatched) {
                Handler(Looper.getMainLooper()).post { onResult(bestReal) }
            }
        }
    }
}
