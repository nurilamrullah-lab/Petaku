package com.geoshift.locationsimulator.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.geoshift.locationsimulator.GeoShiftApplication
import com.geoshift.locationsimulator.service.GeoShiftSimulationService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val settingsRepo = GeoShiftApplication.instance.settingsRepository
            if (settingsRepo.autoStartBoot.value) {
                GeoShiftSimulationService.start(context)
            }
        }
    }
}
