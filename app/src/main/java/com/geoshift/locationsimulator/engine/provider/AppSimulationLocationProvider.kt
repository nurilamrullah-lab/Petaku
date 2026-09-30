package com.geoshift.locationsimulator.engine.provider

import com.geoshift.locationsimulator.data.model.LocationModel
import com.geoshift.locationsimulator.data.model.SimulationMode

class AppSimulationLocationProvider : MockLocationProvider {
    override val mode: SimulationMode = SimulationMode.APP_SIMULATION_MODE
    private var lastSimulatedLocation: LocationModel? = null

    override fun isAvailable(): Boolean = true

    override fun initialize(): Result<Unit> {
        return Result.success(Unit)
    }

    override fun pushLocation(location: LocationModel): Result<Unit> {
        lastSimulatedLocation = location
        return Result.success(Unit)
    }

    override fun release() {
        lastSimulatedLocation = null
    }

    override fun getDiagnosticInfo(): String {
        return "App Simulation Mode: READY (Internal sandbox simulation without system hooks)"
    }
}
