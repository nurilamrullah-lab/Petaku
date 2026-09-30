package com.geoshift.locationsimulator.data.model

enum class SimulationMode(val displayName: String, val badgeColor: Long) {
    ROOT_MODE("Root Mode Active", 0xFF10B981),
    SYSTEM_MOCK_MODE("System Mock Active", 0xFF00D2FF),
    APP_SIMULATION_MODE("App Simulation Active", 0xFFF59E0B)
}
