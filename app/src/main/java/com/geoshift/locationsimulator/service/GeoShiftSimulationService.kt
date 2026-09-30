package com.geoshift.locationsimulator.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.geoshift.locationsimulator.GeoShiftApplication
import com.geoshift.locationsimulator.MainActivity
import com.geoshift.locationsimulator.R
import com.geoshift.locationsimulator.data.model.SimulationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GeoShiftSimulationService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                GeoShiftApplication.instance.simulationEngine.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                GeoShiftApplication.instance.simulationEngine.pause()
            }
            ACTION_RESUME -> {
                GeoShiftApplication.instance.simulationEngine.resume()
            }
            ACTION_START -> {
                val notification = buildNotification("Initializing...", "1x")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                    } else {
                        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                    }
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                observeEngineUpdates()
            }
        }
        return START_STICKY
    }

    private fun observeEngineUpdates() {
        val engine = GeoShiftApplication.instance.simulationEngine
        val settings = GeoShiftApplication.instance.settingsRepository

        scope.launch {
            engine.currentLocation.collectLatest { loc ->
                val speed = settings.speedMultiplier.value
                val speedStr = String.format("%.0fx", speed)
                val notification = buildNotification(loc.locationName, speedStr)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
            }
        }

        scope.launch {
            engine.state.collectLatest { state ->
                if (state == SimulationState.STOPPED) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    showStoppedNotification(this@GeoShiftSimulationService)
                    stopSelf()
                }
            }
        }
    }

    private fun buildNotification(locationName: String, speedStr: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, GeoShiftSimulationService::class.java).apply {
            action = ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this, 1, pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, GeoShiftSimulationService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("GeoShift simulation active")
            .setContentText("Location: $locationName | Speed: $speedStr")
            .setSubText("Location Simulator")
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "geoshift_sim_channel"
        const val NOTIFICATION_ID = 1001
        const val STOPPED_NOTIFICATION_ID = 1002

        const val ACTION_START = "com.geoshift.locationsimulator.START"
        const val ACTION_PAUSE = "com.geoshift.locationsimulator.PAUSE"
        const val ACTION_RESUME = "com.geoshift.locationsimulator.RESUME"
        const val ACTION_STOP = "com.geoshift.locationsimulator.STOP"

        fun showStoppedNotification(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(context)
            }

            val notif = builder
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("GeoShift – Mock Lokasi Dihentikan")
                .setContentText("Simulasi GPS dimatikan. Posisi dikembalikan ke sensor GPS asli HP.")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            manager.notify(STOPPED_NOTIFICATION_ID, notif)
        }

        fun start(context: Context) {
            val intent = Intent(context, GeoShiftSimulationService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, GeoShiftSimulationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
