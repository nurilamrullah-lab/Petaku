package com.geoshift.locationsimulator

import android.app.Application
import android.preference.PreferenceManager
import com.geoshift.locationsimulator.data.repository.LocationHistoryRepository
import com.geoshift.locationsimulator.data.repository.SettingsRepository
import com.geoshift.locationsimulator.engine.LocationSimulationEngine
import org.osmdroid.config.Configuration

class GeoShiftApplication : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var historyRepository: LocationHistoryRepository
        private set

    lateinit var simulationEngine: LocationSimulationEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // OSMDroid configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = packageName

        settingsRepository = SettingsRepository(this)
        historyRepository = LocationHistoryRepository(this)
        simulationEngine = LocationSimulationEngine(this, settingsRepository)
    }

    companion object {
        lateinit var instance: GeoShiftApplication
            private set
    }
}
