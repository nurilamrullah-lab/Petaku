package com.geoshift.locationsimulator.engine

import android.content.Context
import com.geoshift.locationsimulator.data.model.LocationModel
import com.geoshift.locationsimulator.data.model.RoutePoint
import com.geoshift.locationsimulator.data.model.RouteSimulationTrack
import com.geoshift.locationsimulator.data.model.SimulationMode
import com.geoshift.locationsimulator.data.model.SimulationState
import com.geoshift.locationsimulator.data.repository.SettingsRepository
import com.geoshift.locationsimulator.engine.provider.AppSimulationLocationProvider
import com.geoshift.locationsimulator.engine.provider.MockLocationProvider
import com.geoshift.locationsimulator.engine.provider.PatchedLocationProvider
import com.geoshift.locationsimulator.engine.provider.RootLocationProvider
import com.geoshift.locationsimulator.engine.provider.StandardMockLocationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class LocationSimulationEngine(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var simulationJob: Job? = null

    private val _state = MutableStateFlow(SimulationState.READY)
    val state: StateFlow<SimulationState> = _state.asStateFlow()

    private val _currentMode = MutableStateFlow(SimulationMode.APP_SIMULATION_MODE)
    val currentMode: StateFlow<SimulationMode> = _currentMode.asStateFlow()

    // Default anchor: Grand Indonesia, Jakarta Pusat (-6.2088, 106.8456)
    private val _currentLocation = MutableStateFlow(
        LocationModel(
            latitude = -6.2088,
            longitude = 106.8456,
            altitude = 14.0,
            accuracy = 1.2f,
            speed = 0.0f,
            bearing = 0.0f,
            locationName = "Jakarta Pusat",
            address = "DKI Jakarta, Indonesia"
        )
    )
    val currentLocation: StateFlow<LocationModel> = _currentLocation.asStateFlow()

    private val _routeProgress = MutableStateFlow(0f)
    val routeProgress: StateFlow<Float> = _routeProgress.asStateFlow()

    private val _statusErrorMessage = MutableStateFlow<String?>(null)
    val statusErrorMessage: StateFlow<String?> = _statusErrorMessage.asStateFlow()

    private var activeProvider: MockLocationProvider = AppSimulationLocationProvider()
    private var currentRoute: RouteSimulationTrack? = null
    private var currentWaypointIndex: Int = 0

    init {
        resolveProvider()
    }

    fun resolveProvider() {
        val diag = EnvironmentDetector.detect(context)
        _currentMode.value = diag.activeMode

        activeProvider.release()
        activeProvider = when (diag.activeMode) {
            SimulationMode.ROOT_MODE -> {
                if (diag.isSmaliPatcherDetected) PatchedLocationProvider(context)
                else RootLocationProvider(context)
            }
            SimulationMode.SYSTEM_MOCK_MODE -> StandardMockLocationProvider(context)
            SimulationMode.APP_SIMULATION_MODE -> AppSimulationLocationProvider()
        }
        _statusErrorMessage.value = null
    }

    fun setLocation(latitude: Double, longitude: Double, name: String = "Custom Pin", address: String = "Coordinates") {
        val updated = _currentLocation.value.copy(
            latitude = latitude,
            longitude = longitude,
            locationName = name,
            address = address,
            timestamp = System.currentTimeMillis()
        )
        _currentLocation.value = updated
        if (_state.value == SimulationState.RUNNING) {
            activeProvider.pushLocation(updated)
        }
    }

    fun teleport(latitude: Double, longitude: Double, name: String = "Teleported Pin", address: String = "Coordinates") {
        val updated = _currentLocation.value.copy(
            latitude = latitude,
            longitude = longitude,
            locationName = name,
            address = address,
            timestamp = System.currentTimeMillis()
        )
        _currentLocation.value = updated
        _state.value = SimulationState.RUNNING
        val initRes = activeProvider.initialize()
        if (initRes.isFailure) {
            _statusErrorMessage.value = initRes.exceptionOrNull()?.message
        }
        activeProvider.pushLocation(updated)
    }

    fun start() {
        _state.value = SimulationState.RUNNING
        _statusErrorMessage.value = null
        val initRes = activeProvider.initialize()
        if (initRes.isFailure) {
            _statusErrorMessage.value = initRes.exceptionOrNull()?.message
        }
        activeProvider.pushLocation(_currentLocation.value)

        simulationJob?.cancel()
        simulationJob = scope.launch {
            while (isActive && _state.value == SimulationState.RUNNING) {
                tickSimulation()
                val interval = settingsRepository.updateIntervalMs.value
                delay(interval)
            }
        }
    }

    fun pause() {
        if (_state.value == SimulationState.RUNNING) {
            _state.value = SimulationState.PAUSED
            simulationJob?.cancel()
        }
    }

    fun resume() {
        if (_state.value == SimulationState.PAUSED) {
            start()
        }
    }

    fun stop() {
        _state.value = SimulationState.STOPPED
        simulationJob?.cancel()
        simulationJob = null
        _routeProgress.value = 0f
        activeProvider.release()
    }

    fun setSpeedMultiplier(multiplier: Float) {
        settingsRepository.setSpeedMultiplier(multiplier)
    }

    fun setRoute(track: RouteSimulationTrack) {
        currentRoute = track
        currentWaypointIndex = 0
        _routeProgress.value = 0f
        if (track.waypoints.isNotEmpty()) {
            val first = track.waypoints.first()
            setLocation(first.latitude, first.longitude, first.name, "Route Origin")
        }
    }

    private fun tickSimulation() {
        val baseLoc = _currentLocation.value
        val speedMult = settingsRepository.speedMultiplier.value
        val jitterEnabled = settingsRepository.jitterEnabled.value

        var newLat = baseLoc.latitude
        var newLng = baseLoc.longitude
        var newBearing = baseLoc.bearing
        var calculatedSpeed = 0f

        val route = currentRoute
        if (route != null && route.waypoints.size >= 2) {
            val totalPts = route.waypoints.size
            val targetWp = route.waypoints.getOrNull(currentWaypointIndex + 1)
            if (targetWp != null) {
                // Move towards target waypoint
                val dLat = targetWp.latitude - baseLoc.latitude
                val dLng = targetWp.longitude - baseLoc.longitude
                val distKm = haversineKm(baseLoc.latitude, baseLoc.longitude, targetWp.latitude, targetWp.longitude)

                // Standard speed 50 km/h multiplied by speedMult
                val speedKmH = 50f * speedMult
                calculatedSpeed = (speedKmH / 3.6f) // m/s
                val stepKm = (speedKmH / 3600f) * (settingsRepository.updateIntervalMs.value / 1000f)

                if (distKm <= stepKm || distKm < 0.0005) {
                    // Reached waypoint
                    newLat = targetWp.latitude
                    newLng = targetWp.longitude
                    currentWaypointIndex++
                    if (currentWaypointIndex >= totalPts - 1) {
                        currentWaypointIndex = 0 // loop
                    }
                } else {
                    val ratio = stepKm / distKm
                    newLat += dLat * ratio
                    newLng += dLng * ratio
                    newBearing = calculateBearing(baseLoc.latitude, baseLoc.longitude, targetWp.latitude, targetWp.longitude)
                }

                val progress = (currentWaypointIndex.toFloat() / (totalPts - 1).toFloat()).coerceIn(0f, 1f)
                _routeProgress.value = progress
            }
        }

        // Apply realistic multipath noise / jitter if enabled
        var finalLat = newLat
        var finalLng = newLng
        if (jitterEnabled) {
            val jitterMeters = (Random.nextFloat() - 0.5f) * 1.5f
            val latDegreePerMeter = 1.0 / 111320.0
            val lngDegreePerMeter = 1.0 / (111320.0 * cos(Math.toRadians(newLat)))
            finalLat += jitterMeters * latDegreePerMeter
            finalLng += jitterMeters * lngDegreePerMeter
        }

        val updated = baseLoc.copy(
            latitude = finalLat,
            longitude = finalLng,
            speed = calculatedSpeed,
            bearing = newBearing,
            timestamp = System.currentTimeMillis()
        )
        _currentLocation.value = updated

        val pushRes = activeProvider.pushLocation(updated)
        if (pushRes.isFailure) {
            _statusErrorMessage.value = pushRes.exceptionOrNull()?.message
        }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val y = sin(Math.toRadians(lon2 - lon1)) * cos(Math.toRadians(lat2))
        val x = cos(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) -
                sin(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(Math.toRadians(lon2 - lon1))
        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360) % 360).toFloat()
    }
}
