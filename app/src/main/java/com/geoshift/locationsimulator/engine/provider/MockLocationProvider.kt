package com.geoshift.locationsimulator.engine.provider

import com.geoshift.locationsimulator.data.model.LocationModel
import com.geoshift.locationsimulator.data.model.SimulationMode

interface MockLocationProvider {
    val mode: SimulationMode
    fun isAvailable(): Boolean
    fun initialize(): Result<Unit>
    fun pushLocation(location: LocationModel): Result<Unit>
    fun release()
    fun getDiagnosticInfo(): String
}
