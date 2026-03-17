package com.sarathi.emergency.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sarathi.emergency.MainActivity
import com.sarathi.emergency.R
import com.sarathi.emergency.SarathiApp
import com.sarathi.emergency.data.models.DriverLocationRequest
import com.sarathi.emergency.util.LocationHelper
import kotlinx.coroutines.*

/**
 * Background Service to track Ambulance/Driver location for life-saving coordination.
 * Runs in the foreground with a high-priority notification to ensure reliability.
 */
class BackgroundLocationService : Service() {
    companion object {
        private const val TAG = "BackgroundLocationSvc"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationHelper: LocationHelper
    private var stopUpdates: (() -> Unit)? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as SarathiApp
        locationHelper = LocationHelper(this)

        startForeground(1001, createNotification())

        val driverId = app.sessionManager.getDriverId()
        if (driverId.isNotEmpty()) {
            startTracking(driverId, app)
        } else {
            Log.w(TAG, "Missing driverId, stopping service")
            stopSelf()
        }

        return START_STICKY
    }

    private fun startTracking(driverId: String, app: SarathiApp) {
        stopUpdates = locationHelper.requestLocationUpdates { location ->
            serviceScope.launch {
                try {
                    val response = app.api.updateDriverLocation(
                        DriverLocationRequest(
                            driverId = driverId,
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    )
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Location update failed: ${response.code()}")
                    } else {
                        Log.d(TAG, "Location updated: ${location.latitude},${location.longitude}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Location update exception", e)
                }
            }
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "emergency_channel")
            .setContentTitle("SARATHI ACTIVE TRACKING")
            .setContentText("Transmitting your ambulance GPS coordinates to Hospital...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopUpdates?.invoke()
        serviceScope.cancel()
        Log.i(TAG, "Background tracking stopped")
    }
}
