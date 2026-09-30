package com.geoshift.locationsimulator.engine

import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.geoshift.locationsimulator.data.model.GnssConstellation
import com.geoshift.locationsimulator.data.model.GnssFixInfo
import com.geoshift.locationsimulator.data.model.GnssSatellite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class GnssMonitor(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _satellites = MutableStateFlow<List<GnssSatellite>>(generateDefaultSatellites())
    val satellites: StateFlow<List<GnssSatellite>> = _satellites.asStateFlow()

    private val _fixInfo = MutableStateFlow(GnssFixInfo())
    val fixInfo: StateFlow<GnssFixInfo> = _fixInfo.asStateFlow()

    private val _liveLocation = MutableStateFlow<Location?>(null)
    val liveLocation: StateFlow<Location?> = _liveLocation.asStateFlow()

    private var gnssCallback: Any? = null
    private var locationListener: LocationListener? = null
    private var isListening = false
    private var dynamicSimulationJob: Job? = null
    private var lastHardwareUpdateTime = 0L

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (isListening || locationManager == null) return
        isListening = true

        // 1. DAFTARKAN LOCATION LISTENER UNTUK MEMICU CHIP GPS MENJADI AKTIF
        locationListener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                _liveLocation.value = loc
                _fixInfo.value = _fixInfo.value.copy(
                    isFixed = true,
                    fixType = if (loc.hasAltitude()) "3D GNSS FIX" else "2D FIX",
                    hdop = if (loc.hasAccuracy()) (loc.accuracy / 4f).coerceAtLeast(0.6f) else 0.8f,
                    vdop = if (loc.hasVerticalAccuracy()) (loc.verticalAccuracyMeters / 4f).coerceAtLeast(0.8f) else 1.0f
                )
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    0f,
                    locationListener!!,
                    Looper.getMainLooper()
                )
                Log.d("GnssMonitor", "GPS_PROVIDER location listener registered successfully")
            }
        } catch (e: Exception) {
            Log.e("GnssMonitor", "Gagal mendaftarkan GPS_PROVIDER updates: ${e.message}")
        }

        // 2. DAFTARKAN GNSS STATUS CALLBACK UNTUK DATA SATELIT REALTIME
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val callback = object : GnssStatus.Callback() {
                override fun onStarted() {
                    Log.d("GnssMonitor", "GNSS hardware tracking started")
                }

                override fun onStopped() {
                    Log.d("GnssMonitor", "GNSS hardware tracking stopped")
                }

                override fun onFirstFix(ttffMillis: Int) {
                    _fixInfo.value = _fixInfo.value.copy(
                        ttffSeconds = (ttffMillis / 1000).coerceAtLeast(1)
                    )
                }

                override fun onSatelliteStatusChanged(status: GnssStatus) {
                    lastHardwareUpdateTime = System.currentTimeMillis()
                    val count = status.satelliteCount
                    val satList = mutableListOf<GnssSatellite>()
                    var usedCount = 0

                    for (i in 0 until count) {
                        val constType = when (status.getConstellationType(i)) {
                            GnssStatus.CONSTELLATION_GPS -> GnssConstellation.GPS
                            GnssStatus.CONSTELLATION_GLONASS -> GnssConstellation.GLONASS
                            GnssStatus.CONSTELLATION_GALILEO -> GnssConstellation.GALILEO
                            GnssStatus.CONSTELLATION_BEIDOU -> GnssConstellation.BEIDOU
                            GnssStatus.CONSTELLATION_QZSS -> GnssConstellation.QZSS
                            else -> GnssConstellation.UNKNOWN
                        }

                        val svid = status.getSvid(i)
                        val az = status.getAzimuthDegrees(i)
                        val el = status.getElevationDegrees(i)
                        val cn0 = status.getCn0DbHz(i)
                        val used = status.usedInFix(i)
                        if (used) usedCount++

                        satList.add(
                            GnssSatellite(
                                svid = svid,
                                constellation = constType,
                                azimuthDegrees = az,
                                elevationDegrees = el,
                                cn0DbHz = cn0,
                                usedInFix = used
                            )
                        )
                    }

                    if (satList.isNotEmpty()) {
                        _satellites.value = satList
                        _fixInfo.value = _fixInfo.value.copy(
                            isFixed = usedCount >= 3,
                            fixType = if (usedCount >= 4) "3D GNSS FIX" else "2D FIX",
                            satellitesInView = count,
                            satellitesInUse = usedCount
                        )
                    }
                }
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    locationManager.registerGnssStatusCallback(context.mainExecutor, callback)
                } else {
                    locationManager.registerGnssStatusCallback(callback, Handler(Looper.getMainLooper()))
                }
                gnssCallback = callback
                Log.d("GnssMonitor", "registerGnssStatusCallback registered")
            } catch (e: Exception) {
                Log.e("GnssMonitor", "registerGnssStatusCallback failed: ${e.message}")
            }
        }

        // 3. BACKGROUND PULSE ENGINE: Menjamin tampilan tetap hidup & beranimasi secara realtime setiap detik
        dynamicSimulationJob = scope.launch {
            while (isActive && isListening) {
                delay(1000L)
                // Jika hardware GNSS tidak mengirim update baru dalam 3 detik (misal di dalam ruangan),
                // berikan fluktuasi sinyal alami (live dynamic jitter) agar radar & grafik tetap aktif realtime
                val now = System.currentTimeMillis()
                if (now - lastHardwareUpdateTime > 2500L) {
                    val currentSats = _satellites.value
                    if (currentSats.isNotEmpty()) {
                        val updated = currentSats.map { sat ->
                            val jitter = (Random.nextFloat() - 0.5f) * 1.2f
                            val newCn0 = (sat.cn0DbHz + jitter).coerceIn(18f, 49f)
                            sat.copy(cn0DbHz = newCn0)
                        }
                        _satellites.value = updated
                    }
                }
            }
        }
    }

    fun stopListening() {
        if (!isListening || locationManager == null) return
        isListening = false

        dynamicSimulationJob?.cancel()
        dynamicSimulationJob = null

        locationListener?.let {
            try {
                locationManager.removeUpdates(it)
            } catch (_: Exception) {}
        }
        locationListener = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && gnssCallback is GnssStatus.Callback) {
            try {
                locationManager.unregisterGnssStatusCallback(gnssCallback as GnssStatus.Callback)
            } catch (_: Exception) {}
        }
        gnssCallback = null
    }

    companion object {
        fun generateDefaultSatellites(): List<GnssSatellite> {
            val list = mutableListOf<GnssSatellite>()
            // GPS (USA)
            list.add(GnssSatellite(1, GnssConstellation.GPS, 45f, 65f, 42.5f, true))
            list.add(GnssSatellite(3, GnssConstellation.GPS, 120f, 40f, 38.0f, true))
            list.add(GnssSatellite(7, GnssConstellation.GPS, 210f, 75f, 44.2f, true))
            list.add(GnssSatellite(14, GnssConstellation.GPS, 290f, 30f, 32.1f, true))
            list.add(GnssSatellite(17, GnssConstellation.GPS, 15f, 25f, 26.5f, false))
            list.add(GnssSatellite(22, GnssConstellation.GPS, 175f, 55f, 39.8f, true))

            // GLONASS (RU)
            list.add(GnssSatellite(4, GnssConstellation.GLONASS, 70f, 50f, 36.4f, true))
            list.add(GnssSatellite(9, GnssConstellation.GLONASS, 140f, 60f, 41.0f, true))
            list.add(GnssSatellite(18, GnssConstellation.GLONASS, 260f, 45f, 34.2f, true))
            list.add(GnssSatellite(23, GnssConstellation.GLONASS, 330f, 20f, 24.5f, false))

            // Galileo (EU)
            list.add(GnssSatellite(5, GnssConstellation.GALILEO, 30f, 70f, 43.1f, true))
            list.add(GnssSatellite(12, GnssConstellation.GALILEO, 190f, 35f, 31.8f, true))
            list.add(GnssSatellite(26, GnssConstellation.GALILEO, 315f, 55f, 39.5f, true))

            // BeiDou (CN)
            list.add(GnssSatellite(6, GnssConstellation.BEIDOU, 95f, 80f, 46.0f, true))
            list.add(GnssSatellite(11, GnssConstellation.BEIDOU, 160f, 45f, 37.5f, true))
            list.add(GnssSatellite(21, GnssConstellation.BEIDOU, 235f, 65f, 42.0f, true))
            list.add(GnssSatellite(34, GnssConstellation.BEIDOU, 280f, 22f, 23.4f, false))

            // QZSS (JP)
            list.add(GnssSatellite(2, GnssConstellation.QZSS, 60f, 85f, 45.2f, true))
            list.add(GnssSatellite(4, GnssConstellation.QZSS, 80f, 72f, 43.8f, true))

            return list
        }
    }
}
