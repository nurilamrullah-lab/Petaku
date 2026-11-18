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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GeoShiftSimulationService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var stateJob: Job? = null
    private var announced = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                GeoShiftApplication.instance.simulationEngine.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                cancelStartedNotification()
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
                val notification = buildServiceNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                // Sekali per sesi: START berulang (ganti titik/rute) tidak memunculkannya lagi
                if (!announced) {
                    announced = true
                    showStartedNotification()
                }
                observeEngineState()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // Hanya memantau penghentian; notifikasi tidak di-refresh tiap perpindahan koordinat.
    private fun observeEngineState() {
        if (stateJob?.isActive == true) return
        val engine = GeoShiftApplication.instance.simulationEngine
        stateJob = scope.launch {
            engine.state.collectLatest { state ->
                if (state == SimulationState.STOPPED) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    cancelStartedNotification()
                    showStoppedNotification(this@GeoShiftSimulationService)
                    stopSelf()
                }
            }
        }
    }

    private fun openAppPendingIntent(): PendingIntent {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun stopPendingIntent(): PendingIntent {
        val stopIntent = Intent(this, GeoShiftSimulationService::class.java).apply {
            action = ACTION_STOP
        }
        return PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Notifikasi wajib service foreground. Android tidak menghapusnya lewat timeout selama
     * service berjalan, jadi dibuat senyap (channel MIN: tanpa suara, tanpa heads-up, tanpa ikon
     * status bar di kebanyakan perangkat), tidak ongoing (bisa di-swipe di Android 14+), dan
     * hanya diposting sekali.
     */
    private fun buildServiceNotification(): Notification {
        return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Petaku berjalan di latar belakang")
            .setContentIntent(openAppPendingIntent())
            .setOngoing(false)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Hentikan", stopPendingIntent())
            .build()
    }

    private fun showStartedNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val loc = GeoShiftApplication.instance.simulationEngine.currentLocation.value
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Mock lokasi aktif")
            .setContentText(loc.locationName)
            .setContentIntent(openAppPendingIntent())
            .setOngoing(false)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setTimeoutAfter(ANNOUNCE_TIMEOUT_MS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        manager.notify(STARTED_NOTIFICATION_ID, notification)
    }

    private fun cancelStartedNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(STARTED_NOTIFICATION_ID)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val alerts = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val service = NotificationChannel(
                SERVICE_CHANNEL_ID,
                getString(R.string.notification_service_channel_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = getString(R.string.notification_service_channel_desc)
                setShowBadge(false)
            }
            manager.createNotificationChannel(alerts)
            manager.createNotificationChannel(service)
        }
    }

    companion object {
        const val CHANNEL_ID = "geoshift_sim_channel"
        const val SERVICE_CHANNEL_ID = "geoshift_service_channel"
        const val NOTIFICATION_ID = 1001
        const val STOPPED_NOTIFICATION_ID = 1002
        const val STARTED_NOTIFICATION_ID = 1003
        private const val ANNOUNCE_TIMEOUT_MS = 4000L

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

            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Mock lokasi dihentikan")
                .setContentText("Posisi dikembalikan ke GPS asli HP.")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setTimeoutAfter(ANNOUNCE_TIMEOUT_MS)
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
