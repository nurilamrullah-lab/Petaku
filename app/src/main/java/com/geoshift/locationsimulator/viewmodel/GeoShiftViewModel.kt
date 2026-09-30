package com.geoshift.locationsimulator.viewmodel

import android.content.Context
import android.location.Location
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoshift.locationsimulator.GeoShiftApplication
import com.geoshift.locationsimulator.data.model.DeviceDiagnostic
import com.geoshift.locationsimulator.data.model.GnssFixInfo
import com.geoshift.locationsimulator.data.model.GnssSatellite
import com.geoshift.locationsimulator.data.model.HistoryItem
import com.geoshift.locationsimulator.data.model.LocationModel
import com.geoshift.locationsimulator.data.model.RoutePoint
import com.geoshift.locationsimulator.data.model.RouteSimulationTrack
import com.geoshift.locationsimulator.data.model.SimulationMode
import com.geoshift.locationsimulator.data.model.SimulationState
import com.geoshift.locationsimulator.engine.EnvironmentDetector
import com.geoshift.locationsimulator.engine.GnssMonitor
import com.geoshift.locationsimulator.service.GeoShiftSimulationService
import com.geoshift.locationsimulator.util.DeviceLocationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GeoShiftViewModel : ViewModel() {

    private val engine = GeoShiftApplication.instance.simulationEngine
    private val historyRepo = GeoShiftApplication.instance.historyRepository
    private val settingsRepo = GeoShiftApplication.instance.settingsRepository
    private val gnssMonitor = GnssMonitor(GeoShiftApplication.instance)

    val currentLocation: StateFlow<LocationModel> = engine.currentLocation
    val simulationState: StateFlow<SimulationState> = engine.state
    val simulationMode: StateFlow<SimulationMode> = engine.currentMode
    val routeProgress: StateFlow<Float> = engine.routeProgress
    val statusErrorMessage: StateFlow<String?> = engine.statusErrorMessage

    val satellites: StateFlow<List<GnssSatellite>> = gnssMonitor.satellites
    val fixInfo: StateFlow<GnssFixInfo> = gnssMonitor.fixInfo
    val liveGnssLocation: StateFlow<Location?> = gnssMonitor.liveLocation

    private val _realDeviceLocation = MutableStateFlow<Location?>(null)
    val realDeviceLocation: StateFlow<Location?> = _realDeviceLocation.asStateFlow()

    val historyList: StateFlow<List<HistoryItem>> = historyRepo.historyFlow
    val speedMultiplier: StateFlow<Float> = settingsRepo.speedMultiplier

    private val _diagnostic = MutableStateFlow(EnvironmentDetector.detect(GeoShiftApplication.instance))
    val diagnostic: StateFlow<DeviceDiagnostic> = _diagnostic.asStateFlow()

    private val _showDiagnosticDialog = MutableStateFlow(false)
    val showDiagnosticDialog: StateFlow<Boolean> = _showDiagnosticDialog.asStateFlow()

    fun openDiagnostic() {
        refreshDiagnostic()
        _showDiagnosticDialog.value = true
    }

    fun closeDiagnostic() {
        _showDiagnosticDialog.value = false
    }

    fun refreshDiagnostic() {
        val diag = EnvironmentDetector.detect(GeoShiftApplication.instance)
        _diagnostic.value = diag
        engine.resolveProvider()
    }

    fun startGnssMonitoring() {
        gnssMonitor.startListening()
    }

    fun stopGnssMonitoring() {
        gnssMonitor.stopListening()
    }

    fun setLocation(lat: Double, lng: Double, name: String, address: String) {
        engine.setLocation(lat, lng, name, address)
    }

    fun exportHistoryToGpx(context: Context): java.io.File? {
        return historyRepo.exportToGpx(context)
    }

    fun teleportLocation(lat: Double, lng: Double, name: String, address: String, context: Context) {
        engine.teleport(lat, lng, name, address)
        GeoShiftSimulationService.start(context)
        historyRepo.addHistory(name, address, lat, lng, "Teleport Spoof")
        Toast.makeText(context, "Teleport aktif ke: $name", Toast.LENGTH_SHORT).show()
    }

    fun syncWithRealLocation(context: Context) {
        DeviceLocationHelper.getRealDeviceLocation(context) { loc ->
            if (loc != null) {
                _realDeviceLocation.value = loc
                engine.setLocation(
                    loc.latitude,
                    loc.longitude,
                    "Lokasi GPS Asli",
                    "Sensor GPS Fisik (±${"%.1f".format(loc.accuracy)}m)"
                )
            }
        }
    }

    fun startSimulation(context: Context) {
        engine.start()
        GeoShiftSimulationService.start(context)
        val cur = engine.currentLocation.value
        historyRepo.addHistory(cur.locationName, cur.address, cur.latitude, cur.longitude, "Simulation Active")
    }

    fun pauseSimulation() {
        engine.pause()
    }

    fun resumeSimulation() {
        engine.resume()
    }

    fun stopSimulation(context: Context) {
        engine.stop()
        GeoShiftSimulationService.stop(context)
        Toast.makeText(context, "Mock lokasi dihentikan. Mengembalikan ke GPS HP...", Toast.LENGTH_SHORT).show()

        DeviceLocationHelper.getRealDeviceLocation(context) { realLoc ->
            if (realLoc != null) {
                _realDeviceLocation.value = realLoc
                engine.setLocation(
                    realLoc.latitude,
                    realLoc.longitude,
                    "Lokasi GPS Asli",
                    "Terdeteksi dari sensor GPS HP (Akurasi ±${"%.1f".format(realLoc.accuracy)}m)"
                )
                Toast.makeText(
                    context,
                    "Kembali ke lokasi GPS HP: ${"%.4f".format(realLoc.latitude)}, ${"%.4f".format(realLoc.longitude)}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun setSpeed(multiplier: Float) {
        settingsRepo.setSpeedMultiplier(multiplier)
    }

    fun setSpeedMultiplier(multiplier: Float) {
        settingsRepo.setSpeedMultiplier(multiplier)
    }

    fun setRoute(track: RouteSimulationTrack) {
        engine.setRoute(track)
    }

    fun startRouteSimulation(track: RouteSimulationTrack, context: Context) {
        engine.setRoute(track)
        startSimulation(context)
    }

    fun startPredefinedRoute(track: RouteSimulationTrack, context: Context) {
        engine.setRoute(track)
        startSimulation(context)
    }

    fun clearAllHistory() {
        historyRepo.clearAll()
    }

    fun deleteHistory(id: Long) {
        historyRepo.deleteItem(id)
    }

    fun toggleFavorite(id: Long, current: Boolean) {
        historyRepo.toggleFavorite(id, current)
    }
}
