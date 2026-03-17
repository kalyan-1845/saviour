package com.sarathi.emergency.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

/**
 * Location helper with dual strategy:
 *  1. Google Play Services FusedLocationClient (primary)
 *  2. Android LocationManager fallback (works without Play Services)
 */
class LocationHelper(private val context: Context) {
    companion object {
        private const val TAG = "LocationHelper"
    }

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Try to get last known location.
     * Uses FusedLocation first, falls back to LocationManager.
     */
    fun getLastLocation(onResult: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            onResult(null)
            return
        }
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        onResult(location)
                    } else {
                        // Fused returned null — try LocationManager fallback
                        getLastLocationFallback(onResult)
                    }
                }
                .addOnFailureListener {
                    // Play Services not available — use fallback
                    Log.w(TAG, "Fused location failed, using fallback")
                    getLastLocationFallback(onResult)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Last location failed", e)
            getLastLocationFallback(onResult)
        }
    }

    /**
     * Fallback: get last known location from Android LocationManager
     */
    private fun getLastLocationFallback(onResult: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            onResult(null)
            return
        }
        try {
            val manager = locationManager ?: run { onResult(null); return }

            // Try GPS provider first, then Network
            val gpsLoc = try {
                manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } catch (_: Exception) { null }

            val netLoc = try {
                manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } catch (_: Exception) { null }

            // Return the most recent one
            val bestLoc = when {
                gpsLoc != null && netLoc != null -> {
                    if (gpsLoc.time > netLoc.time) gpsLoc else netLoc
                }
                gpsLoc != null -> gpsLoc
                netLoc != null -> netLoc
                else -> null
            }
            onResult(bestLoc)
        } catch (e: SecurityException) {
            Log.e(TAG, "No permission for fallback last location", e)
            onResult(null)
        }
    }

    /**
     * Request continuous location updates.
     * Uses FusedLocation first, falls back to LocationManager.
     * Returns a cleanup function to stop updates.
     */
    fun requestLocationUpdates(onLocation: (Location) -> Unit): () -> Unit {
        if (!hasLocationPermission()) return {}

        // Try FusedLocationClient first
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(2000)
            .setMaxUpdateDelayMillis(5000)
            .build()

        val fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { onLocation(it) }
            }
        }

        var usingFused = false

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                fusedCallback,
                Looper.getMainLooper()
            )
            usingFused = true
        } catch (e: Exception) {
            // FusedLocation not available, use fallback
            Log.w(TAG, "Fused updates unavailable, using LocationManager")
        }

        // Also start LocationManager as a backup (it'll just give us more data points)
        var managerListener: LocationListener? = null
        if (!usingFused) {
            try {
                val manager = locationManager
                if (manager != null) {
                    managerListener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            onLocation(location)
                        }
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }
                    val listener = managerListener
                    // Try GPS provider
                    if (listener != null && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        manager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            3000L, 5f,
                            listener,
                            Looper.getMainLooper()
                        )
                    }
                    // Also try Network provider
                    if (listener != null && manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        manager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            3000L, 5f,
                            listener,
                            Looper.getMainLooper()
                        )
                    }
                }
            } catch (e: SecurityException) {
                // Permission issue
                Log.e(TAG, "No permission for location updates", e)
            }
        }

        return {
            if (usingFused) {
                fusedLocationClient.removeLocationUpdates(fusedCallback)
            }
            managerListener?.let { listener ->
                locationManager?.removeUpdates(listener)
            }
        }
    }
}
